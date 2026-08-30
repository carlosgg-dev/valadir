# Security Architecture

What this system guarantees, and how it behaves when it cannot.

## Token Strategy

Authentication is based on two tokens with different roles:

- **Access token** — JWT signed with ECDSA P-256 (ES256), short-lived (15 min), sent on every request. Validated by
  signature, by expiry, and against the revocation keys below — a token that carries no `jti`, `sub` or `iat` is
  refused, since it cannot be matched against either of them.
- **Refresh token** — opaque UUID, long-lived (7 days), stored server-side in Redis. Carries no claims. Redis holds
  its SHA-256 fingerprint, never the token: read access to Redis — a dump, a backup, a replica — yields no usable
  credential. The same applies to the password reset verification token.

The asymmetric key pair allows other services to verify access tokens using only the public key, without access to the
signing key.

## Redis Usage

| Repository               | Type               | Purpose                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------------|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RefreshTokenRepository` | Whitelist          | Tracks active refresh tokens by fingerprint (`TokenFingerprint`), under `auth:refresh_token:{fingerprint}` and as members of `auth:user_tokens:{accountId}`. Long-lived tokens require explicit server-side revocation on logout or reuse detection. Both the token key and the set carry a TTL matching the token expiry.                                                                                                                                                                                                                                                                                                                                                  |
| `AccessTokenRevocation`  | Blacklist + cutoff | Answers whether an access token is refused, for the two reasons it can be. `auth:blacklist:{jti}` holds tokens revoked one at a time (logout), with a TTL equal to the remaining token lifetime. `auth:token_cutoff:{accountId}` holds the instant from which every access token of an account is refused (password reset and "close every session"), with a TTL equal to the access token lifetime — past it, nothing it could reject is still alive. Both keys travel in a single `MGET`, so the check still costs one round-trip per request. |

The access token uses a blacklist (not a whitelist) because it is used on every request — querying a whitelist on each
call would add unnecessary latency. The refresh token uses a whitelist because its long lifetime would make a blacklist
grow indefinitely without TTL-based cleanup.

A plain SHA-256 is the right derivation for these tokens, and a password hash is not. The lookup happens *by* the token,
so it must be deterministic — Argon2, which the OTPs use, yields a different hash per call by design. The work factor
buys nothing either: a refresh token is a 122-bit random UUID, so there is no candidate list to run against the digest.
What a fingerprint costs is the ability to read a token back, which nothing needs: the token travels only in the
response that issued it and in the request that spends it.

Fingerprinting the keys ends every session live at the moment it is deployed: what `validate` now looks up no longer
matches what the old keys were named, so users sign in again. Nothing needs migrating — unlike the email normalisation
below, there is no row to rewrite, only keys to let expire, which they do within their own TTL. The key prefix is
deliberately unchanged: a 64-character hex fingerprint cannot collide with a 36-character UUID, so a new namespace
would buy nothing.

## Refresh Token Rotation

Every refresh operation consumes the current refresh token and issues a new pair. A token not found in Redis is treated
as invalid — the user must log in again.

The system supports multiple active sessions. Each login issues a new refresh token without invalidating existing ones,
allowing concurrent sessions across different devices. `POST /api/auth/logout/all` closes every one of them, the calling
device included — a user who suspects the account is compromised should not have to reset their password to sign their
other devices out. Completing a password reset does the same, as part of completing it.

## Session Ownership

`auth:user_tokens:{accountId}` holds the set of live sessions of one account. Five flows mutate it, and each upholds one
property:

| Flow                      | Property                                     |
|---------------------------|----------------------------------------------|
| Login                     | **adds** a session; existing ones stay alive |
| Refresh                   | rotates **exactly one** member of the set    |
| Logout                    | revokes **exactly one** session              |
| Logout all                | revokes **every** session of that account    |
| Password reset (complete) | revokes **every** session of that account    |

The set carries the same TTL as a refresh token, refreshed on every login and every rotation. Since all refresh tokens
live the same span, the member just added is always the last of the set to die, so the set never outlives a live session
and never survives the account's last one. Nothing removes a fingerprint when its token expires on its own — only logout
and rotation do the `SREM` — so an unbounded set would keep one dead member per login, forever.

Every operation is scoped to the account resolved from the authenticated principal, never to an identifier carried in
the request body. Logout checks that the refresh token it is asked to revoke belongs to the authenticated account
(`GET KEYS[2] == accountId` in `logout_invalidate_tokens.lua`); without that guard, an access token of account A plus a
refresh token of account B would kill B's session.

**Closing every session closes the access tokens too.** Revoking the refresh tokens alone would leave an access token
issued beforehand authenticating for the rest of its 15 minutes — precisely in the flows a user runs when they suspect
the account is compromised. `invalidate_account_tokens.lua` deletes the refresh tokens and writes
`auth:token_cutoff:{accountId}` in one atomic call, so both halves of "this session is over" land together; every access
token of that account whose `iat` is at or before the cutoff is then refused on its next request.

The cutoff revokes by time rather than by identity, so it needs no registry of live `jti` and no write on the token
issue path. Its cost is resolution: `iat` travels in whole seconds, so a token minted within the same second as the
revocation cannot be proven newer than the cutoff and is refused. The tie is resolved closed — a sign-in that lands in
that same second is answered 401 and succeeds on the retry, whereas resolving it open would let that one token live out
its full lifetime.

## Account Identity

Email addresses are normalised to lower case before they are stored or looked up, so `A@x.com` and `a@x.com` resolve to
one single account — and to one single rate-limit bucket. Without normalisation, case variants act as independent
accounts and as independent brute-force budgets.

There is no Flyway/Liquibase in the project, and `docker/postgres/init.sql` only creates the schema. A database created
before this normalisation landed needs a one-off `UPDATE accounts SET email = lower(email);`, or rows written earlier
stay unreachable by the normalised lookups.

## Failure Policy

In an authentication system, the failure mode is a security property, not an operational detail. The rule is:
**fail closed at the security boundary, degrade in a controlled way only for secondary services.**

### Fail-closed — security operations

| Situation                                                 | Behaviour                                                                                           |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Postgres/Redis down while **validating credentials**      | **Deny** the login. Never issue a token.                                                            |
| Redis down while checking the access-token **revocation** | The token is **not validatable → deny** access. It cannot be confirmed as unrevoked.                |
| Failure mid **refresh token rotation**                    | Never issue a new pair without atomic revocation of the old one. When in doubt, **deny**.           |
| Redis down in the **rate limiter**                        | **Deny** the request. Whoever can take Redis down must not gain an unlimited brute-force budget.    |
| Redis down while reading the **failed-attempt counter**   | **Deny** the login. Lockout and CAPTCHA step-up must not be bypassable by making Redis unavailable. |

All of these surface as **503 `INFRASTRUCTURE_UNAVAILABLE`**, logout included: no flow translates an outage into a
business error code. A 500 would tell the client not to retry a failure that is precisely worth retrying — the
revocation did not happen and the token is still live. In practice a total Redis outage never reaches the logout use
case anyway: the revocation lookup in `RevocationAwareJwtDecoder` is the first Redis touch of an authenticated request,
so the denial comes from the filter chain. The use case's own path is reachable only on a partial failure, where reads
work and writes do not.

The rate-limiter row is a genuine trade-off: failing closed accepts a self-DoS risk, in that an outage of the limiter
takes down the endpoints it protects. It is accepted because the cost is small — with Redis down there is no refresh, no
logout, no OTP and no password reset either — while the alternative hands unlimited credential stuffing to anyone who
can degrade one dependency.

The same reasoning applies to the revocation lookup. Failing open there was the previous behaviour, and it is defensible
on availability grounds: an outage denies every authenticated request. It was inverted because it makes `logout` a
suggestion — a revoked token keeps authenticating for the rest of its 15-minute lifetime, and the client cannot tell.

**Clearing the attempt counter is the one exception**, and it is not a hole. If Redis fails while erasing the counter
after a successful login, the login stands and the failure is logged. A counter left uncleared makes the *next* attempt
more restrictive, never less, and denying a login that has already proved its credentials would protect nothing.

### Degradation — secondary services

| Situation                                           | Behaviour                                                                                                                      |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| SMTP down while sending an activation/reset **OTP** | The account is **not** activated without an OTP. The pending account is persisted, the error is retryable, and *resend* works. |
| A non-essential email notification fails            | Logged with context; the main flow continues. The lockout notification is `@Async`, and the login guards it besides.           |
| **Turnstile (CAPTCHA) unreachable**                 | **Fail-open**: the challenged login proceeds.                                                                                  |

The Turnstile exception is deliberate. A Cloudflare outage must not block every login, and the blast radius is small:
the step-up only applies after three failed attempts, and the counter that produces those three is itself fail-closed.
An interpretable rejection from Turnstile (4xx, malformed body) is *not* an outage and fails closed.

The SMTP row asks the caller to retry precisely because the account **is** there: answering 204 would hide a code that
never left. Where that same 503 also marks the address as registered — *resend* and password-reset *initiate*, whose
other branches answer 204 — the channel is knowingly left open: `register` already answers 409 for an active email, so
concealing it here would cost a real caller their only signal in exchange for hiding what the front door hands over on
request, bounded by rate limit.

Which use case decides that a notification is secondary is not the adapter's call. `LoginService` guards the lockout
notification itself, as the three Redis cleanups do, so the login's outcome does not depend on the `@Async` proxy
holding: without it, an SMTP failure would answer 503 on the one attempt that crosses the threshold, and only on that
one — announcing the threshold to whoever is probing it.

### Bounded failure detection

A fail-closed policy is only worth what its detection time. Every outbound call therefore has a deadline: Redis at 2s
with a 1s connect (Lettuce would otherwise wait 60s, so applying the policy would take a minute), Postgres at 2s to
acquire a connection and 5s per statement, Turnstile at 2s connect plus 3s read, SMTP at 2s connect plus 5s to read or
write (JavaMail defaults to no limit, so a mail server that accepts the connection and then goes quiet would hold the
request thread instead of failing into the retryable 503 the degradation table promises).

On top of the deadlines, Redis and Turnstile calls run through a **circuit breaker** (Resilience4j). Once a dependency
is clearly down, the breaker answers without attempting the call, so a request stops paying the timeout to reach a
conclusion already reached. This changes **how fast a failure is answered, never what the answer is**: an open Redis
circuit denies exactly as a Redis failure denies, and an open Turnstile circuit lets the challenged login through
exactly as a Cloudflare outage does. A breaker that altered the verdict would be a policy change wearing a
latency-optimisation costume.

Which is why the `captcha` breaker records only an I/O failure or a 5xx. An uninterpretable 4xx denies the login, and
counting it towards a circuit whose open state **allows** would let a stream of malformed responses switch the step-up
off for everyone — the same verdict change, arrived at through the back door.

The breakers are applied with Resilience4j's functional API from **inside** the adapter, never with `@CircuitBreaker`.
The annotation wraps the method from the outside, which would leave `CallNotPermittedException` uncatchable at the call
site and let it escape as its own type — surfacing as a 500 on the Redis side, and silently flipping the CAPTCHA to
fail-closed on the Turnstile side. `RedisCircuitGuard` is the single place where the seven Redis adapters translate a
failure, an open circuit included, into an `InfrastructureException`.

### Error opacity

Nothing but an `ErrorCode` crosses the boundary — no stack traces, no infrastructure detail, no PII, no cryptographic
material. An infrastructure outage is externally indistinguishable from any other 503.

Opaque is not the same as uninformative: the code must still say which side got it wrong. A request the framework
rejected before it reached a use case answers `MALFORMED_REQUEST` — distinct from `INVALID_FIELD`, which reports a field
that was validated and failed and carries the offending fields — and never `INTERNAL_SERVER_ERROR`, which would report
our failure as the caller's request. `ErrorCodeResolver` is the inverse of `HttpStatusResolver`: where the application
throws, the code decides the status; where the framework rejects, the status decides the code. Both
`GlobalExceptionHandler.handleExceptionInternal` and `GlobalErrorController` resolve through it, so a status means the
same thing whether it was raised above or below the DispatcherServlet. The headers Spring resolved travel with the
response, since for some statuses they are the answer: `Allow` on a 405, `Accept` on a 415.

**Each code states its own status, and `HttpStatusResolver` is the only place it is stated.** The switch is exhaustive
and carries no `default`, so a new code does not compile until someone decides what it answers. The alternative failed
twice over: a category in between took the decision away from the compiler, and every path that fixed a status at the
throw site took it away from the table — which is how `INFRASTRUCTURE_UNAVAILABLE` came to declare a 500 while three
call sites answered 503. Both writers and every handler now resolve, so the two can no longer disagree. Only the
framework-rejection path keeps its own status, and by design: there the status is the input, not the output.

**A single code covers every rejection, not one per status.** The status already tells 400 from 404, 405 and 415, so a
code per status would carry nothing the caller does not have. A code of its own is earned where several causes share a
status **and the caller has to act differently on each** — the three that answer 403 are the case that earns it:
`CAPTCHA_REQUIRED` means show the widget and retry, `ACCOUNT_PENDING_ACTIVATION` means send the user to the OTP screen,
`ACCESS_DENIED` means do not retry. With the status alone none of those flows can be built. The same holds for the six
that answer 401 and the two that answer 429. A code no flow can reach is not a contract but an obligation to invent a
status for it, which is why `AUTHENTICATION_FAILED` was deleted rather than kept: nothing threw it.

**The constant is the contract: the published identifier is the name, lower-cased.** `getCode()` returns
`name().toLowerCase(Locale.ROOT)`, so `CAPTCHA_REQUIRED` answers `captcha_required` and there is no second table to
maintain — one grep finds the enum, the log line, the test and the response. The identifier it replaced was a number,
and the price of the indirection was paid in full: two renumberings, the first across eleven files; five E2E classes
each keeping their own alias row; and comments in `application.yml` naming a code the reader then had to look up.

Once the catalogue ships, a name is as frozen as a number was. The client keeps its own table of what to do with each
one, so an identifier that changes meaning breaks nothing visibly — the client simply does the wrong thing. From the
first release on, adding a code is backwards compatible; renaming or deleting one is a breaking change, and renaming a
constant now moves the wire value with it. That day has not come — nothing is deployed and no client holds such a
table — so the catalogue is still free to be corrected in place.

Exceptions thrown inside a servlet filter never reach `GlobalExceptionHandler`, which only sees what the
DispatcherServlet dispatches; Spring Security's `ExceptionTranslationFilter` only handles
`AuthenticationException`/`AccessDeniedException`, so anything else escapes to the container. `GlobalErrorController`
catches those on `/error` and keeps them opaque, but as a generic 500 `INTERNAL_SERVER_ERROR` — the right answer for a
failure nobody recognises, and the wrong one for an outage, which is a 503 the caller should retry.
`InfrastructureFailureFilter` sits
between the two: registered directly inside the MDC filter, so it wraps the JWT decoder and the rate limiter and logs
with the request id, it is the single place where an outage raised below the controller is turned into its 503.

A persistence failure needs the same treatment for a different reason. The adapter translates whatever its
`try` can see, but not its own **rollback**: a write that fails with Postgres unreachable throws inside the body, and
the transaction proxy then rolls back on a connection the pool already closed, replacing the
`InfrastructureException` with a `JpaSystemException` on the way out — outside every `catch` in the adapter, and so a
500 telling the caller not to retry a failure that is exactly worth retrying.
`GlobalExceptionHandler.handlePersistence` is where that lands: any `DataAccessException` that escaped adapter
translation is an outage and answers **503 `INFRASTRUCTURE_UNAVAILABLE`**. It covers the five `@Transactional` adapter
methods and the deferred INSERT that flushes at commit, without putting transaction plumbing in each of them. Measured
with `PostgresOutageIT`, not reasoned.

The answer without that filter is worse than the generic 500: removing it and pausing Redis on an authenticated request
returns **401 `AUTHENTICATION_REQUIRED`**, because the `/error` dispatch re-enters the chain with no principal and the
entry point answers before `GlobalErrorController` is reached. Measured with `RedisOutageIT`, not reasoned — and it is
not a peculiarity of failing mid-authentication. The same removal on a **public** endpoint, where no authentication
happens at all and the failing filter is the rate limiter, answers the same **401 `AUTHENTICATION_REQUIRED`**; measured
with `RateLimiterOutageIT`. `GlobalErrorController` is therefore effectively unreachable for a filter-level failure, and
`InfrastructureFailureFilter` is the only thing standing between an outage and an ordinary-looking bad credential.

For that to hold, the exception must reach the filter **untouched**. Wrapping it in a `JwtException` inside the
decoder — the natural instinct, since that is what `decode` declares — would have `JwtAuthenticationProvider` translate
it into an `AuthenticationServiceException` and answer the same **401 `AUTHENTICATION_REQUIRED`**, hiding a Redis outage
behind an ordinary authentication failure.

## Configuration Integrity

Every guarantee above is a number in `application.yml` — the deadlines, the breaker thresholds, the lockout tiers, the
fourteen rate-limit rules. A misspelled key does not fail: it falls back to a default in silence, and whatever it fed
quietly stops holding. Two mechanisms close that, one per source of the value.

What the **versioned file** binds is pinned by `ProductionConfigurationTest`, which binds `application.yml` with
Spring's own `Binder` and asserts the values in effect together with the full key set. A typo is not a changed value: it
is a key that stops existing while a stranger appears beside it, which is why the key set is asserted and not only the
numbers.

What a **deployment** binds — environment variables, profile overrides — no test can see, so the application refuses to
start on a configuration that did not bind. The degree of the guard follows the shape of the rule: declarative
constraints where a value must simply be present and well-formed (`JwtProperties`, `RateLimitProperties`,
`CaptchaProperties`), and a compact-constructor guard where the rule is conditional or cross-field — a rule list is
required only while the limiter is enabled, a Turnstile endpoint and secret only while the CAPTCHA is, and
`AsyncProperties`' three pool sizes are only meaningful against each other.

`LoginLockoutProperties` carries neither, deliberately: its invariants belong to `LoginLockoutPolicy`, which
`ApplicationWiring` builds at startup, so an invalid lockout configuration still refuses to start — through the domain,
rather than through a second copy of its rules at the boundary. Those invariants are worth stating, because the file
does not show them: `min-failures` values must be unique, lockouts must be strictly ascending by `min-failures`, and the
challenge threshold must sit **below** the first tier, or the CAPTCHA step-up is unreachable — the account would already
be locked by the time it would trigger. `lockoutFor` then applies the **longest** lockout among the tiers reached, so
the order the tiers appear in the file is legibility, not a guarantee.

Four values never appear in the file at all: `JWT_PRIVATE_KEY`, `TURNSTILE_SECRET`, `DATABASE_PASSWORD` and
`REDIS_PASSWORD` are `${…}` placeholders, and stay that way. A real secret pasted into a versioned file is the one
mistake a revert cannot undo.

## Known Behaviours

Not defects, but things that are expensive to rediscover.

- **A second password reset does not invalidate the first verification token.** Both resolve to the same account, so it
  is not a hole; the abandoned token simply lives out its 10-minute TTL.
- **Re-registering over a pending account orphans its OTP key.** `replace()` deletes the abandoned account's rows but
  not `auth:account_activation_otp:{oldAccountId}`, which lingers until its TTL holding an Argon2 hash. No account
  resolves to that id any more — do not read a stray key as a live code.
- **A rate-limited request still enters the window.** The sliding-window log `ZADD`s before deciding, so a client
  hammering past its limit keeps pushing its own reset forward. Intended.
- **A 406 carries no body.** Writing the error body runs through the same content negotiation that produced the 406, so
  the client gets the status and nothing else. It is the one HTTP failure where no `ErrorCode` reaches the caller at
  all, `MALFORMED_REQUEST` included.
- **A refresh token whose account no longer exists answers 500.** Unreachable today: the only deletion path is the purge
  of `PENDING_ACTIVATION` accounts, which can never hold a refresh token. If a real account-deletion flow ever lands,
  the fix is for that use case to call `accountTokensInvalidator.invalidateAll(...)` — as `CompletePasswordReset`
  already does — **not** to soften the status code. The 500 is the alarm for a genuine integrity breach.
