# Security Architecture

What this system guarantees, and how it behaves when it cannot.

## Token Strategy

Authentication is based on two tokens with different roles:

- **Access token** — JWT signed with ECDSA P-256 (ES256), short-lived (15 min), stateless, sent on every request.
  Validated by signature and expiry.
- **Refresh token** — opaque UUID, long-lived (7 days), stored server-side in Redis. Carries no claims.

The asymmetric key pair allows other services to verify access tokens using only the public key, without access to the
signing key.

## Redis Usage

| Repository               | Type      | Purpose                                                                                                                                                               |
|--------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RefreshTokenRepository` | Whitelist | Tracks active refresh tokens. Long-lived tokens require explicit server-side revocation on logout or reuse detection. Each entry has a TTL matching the token expiry. |
| `AccessTokenBlacklist`   | Blacklist | Tracks revoked access tokens. Consulted on every request to reject tokens invalidated before expiry. Each entry has a TTL equal to the remaining token lifetime.      |

The access token uses a blacklist (not a whitelist) because it is used on every request — querying a whitelist on each
call would add unnecessary latency. The refresh token uses a whitelist because its long lifetime would make a blacklist
grow indefinitely without TTL-based cleanup.

## Refresh Token Rotation

Every refresh operation consumes the current refresh token and issues a new pair. A token not found in Redis is treated
as invalid — the user must log in again.

The system supports multiple active sessions. Each login issues a new refresh token without invalidating existing ones,
allowing concurrent sessions across different devices. All sessions can be explicitly revoked via a dedicated endpoint.

## Session Ownership

`auth:user_tokens:{accountId}` holds the set of live sessions of one account. Four flows mutate it, and each upholds one
property:

| Flow                      | Property                                        |
|---------------------------|-------------------------------------------------|
| Login                     | **adds** a session; existing ones stay alive    |
| Refresh                   | rotates **exactly one** member of the set       |
| Logout                    | revokes **exactly one** session                 |
| Password reset (complete) | revokes **every** refresh token of that account |

Every operation is scoped to the account resolved from the authenticated principal, never to an identifier carried in
the request body. Logout checks that the refresh token it is asked to revoke belongs to the authenticated account
(`GET KEYS[2] == accountId` in `logout_invalidate_tokens.lua`); without that guard, an access token of account A plus a
refresh token of account B would kill B's session.

**Access tokens are not revoked by a password reset.** `complete` revokes refresh tokens only, so an access token issued
before the reset keeps authenticating for up to its 15-minute lifetime — precisely in the flow a user runs when they
suspect the account is compromised. Closing this requires a registry of live `jti` per account, which does not exist
today.

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

| Situation                                                | Behaviour                                                                                           |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Postgres/Redis down while **validating credentials**     | **Deny** the login. Never issue a token.                                                            |
| Redis down while checking the access-token **blacklist** | The token is **not validatable → deny** access. It cannot be confirmed as unrevoked.                |
| Failure mid **refresh token rotation**                   | Never issue a new pair without atomic revocation of the old one. When in doubt, **deny**.           |
| Redis down in the **rate limiter**                       | **Deny** the request. Whoever can take Redis down must not gain an unlimited brute-force budget.    |
| Redis down while reading the **failed-attempt counter**  | **Deny** the login. Lockout and CAPTCHA step-up must not be bypassable by making Redis unavailable. |

All of these surface as **503 `INFRA-001`**.

The rate-limiter row is a genuine trade-off: failing closed accepts a self-DoS risk, in that an outage of the limiter
takes down the endpoints it protects. It is accepted because the cost is small — with Redis down there is no refresh, no
logout, no OTP and no password reset either — while the alternative hands unlimited credential stuffing to anyone who
can degrade one dependency.

The same reasoning applies to the blacklist. Failing open there was the previous behaviour, and it is defensible on
availability grounds: an outage denies every authenticated request. It was inverted because it makes `logout` a
suggestion — a revoked token keeps authenticating for the rest of its 15-minute lifetime, and the client cannot tell.

### Degradation — secondary services

| Situation                                           | Behaviour                                                                                                                      |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| SMTP down while sending an activation/reset **OTP** | The account is **not** activated without an OTP. The pending account is persisted, the error is retryable, and *resend* works. |
| A non-essential email notification fails            | Logged with context; the main flow continues. Lockout notifications are `@Async` and best-effort.                              |
| **Turnstile (CAPTCHA) unreachable**                 | **Fail-open**: the challenged login proceeds.                                                                                  |

The Turnstile exception is deliberate. A Cloudflare outage must not block every login, and the blast radius is small:
the step-up only applies after three failed attempts, and the counter that produces those three is itself fail-closed.
An interpretable rejection from Turnstile (4xx, malformed body) is *not* an outage and fails closed.

### Error opacity

Nothing but an `ErrorCode` crosses the boundary — no stack traces, no infrastructure detail, no PII, no cryptographic
material. An infrastructure outage is externally indistinguishable from any other 503.

Exceptions thrown inside a servlet filter never reach `GlobalExceptionHandler`, which only sees what the
DispatcherServlet dispatches; Spring Security's `ExceptionTranslationFilter` only handles
`AuthenticationException`/`AccessDeniedException`, so anything else escapes to the container. `GlobalErrorController`
catches those on `/error` and keeps them opaque, but as a generic 500 `SYS-001` — the right answer for a failure nobody
recognises, and the wrong one for an outage, which is a 503 the caller should retry. `InfrastructureFailureFilter` sits
between the two: registered directly inside the MDC filter, so it wraps the JWT decoder and the rate limiter and logs
with the request id, it is the single place where an outage raised below the controller is turned into its 503.

## Known Behaviours

Not defects, but things that are expensive to rediscover.

- **A second password reset does not invalidate the first verification token.** Both resolve to the same account, so it
  is not a hole; the abandoned token simply lives out its 10-minute TTL.
- **Re-registering over a pending account orphans its OTP key.** `replace()` deletes the abandoned account's rows but
  not `auth:account_activation_otp:{oldAccountId}`, which lingers until its TTL holding an Argon2 hash. No account
  resolves to that id any more — do not read a stray key as a live code.
- **A rate-limited request still enters the window.** The sliding-window log `ZADD`s before deciding, so a client
  hammering past its limit keeps pushing its own reset forward. Intended.
- **A refresh token whose account no longer exists answers 500.** Unreachable today: the only deletion path is the purge
  of `PENDING_ACTIVATION` accounts, which can never hold a refresh token. If a real account-deletion flow ever lands,
  the fix is for that use case to call `refreshTokenRepository.revokeAllForAccount(...)` — as `CompletePasswordReset`
  already does — **not** to soften the status code. The 500 is the alarm for a genuine integrity breach.
- **Logout answers 500 `SEC-002` on an infrastructure failure**, where every other flow answers 503 `INFRA-001`. It is
  fail-closed (it never reports a revocation that did not happen), but the status is inconsistent with the policy.
