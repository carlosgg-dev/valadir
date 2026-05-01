# Logging & Observability

## Never swallow exceptions

Always log with context OR propagate — never do neither.
When wrapping an exception, always chain the original cause so the root stacktrace survives.
A re-thrown exception that loses its cause makes the root failure invisible in production logs.

## Never log AND re-throw

Pick exactly one:

- **Log** in the catch block and handle locally — do not re-throw.
- **Re-throw** (or wrap and re-throw) and let a central handler log it.

Doing both produces duplicate log entries for the same error and pollutes the log stream.

**Exception:** log before re-throwing only when the catch site holds context that the central
handler cannot recover — such as domain-specific identifiers, security event semantics,
or state that disappears after the exception propagates.

## No information leak to the exterior

Error responses returned to clients must be opaque. Never expose:

- Stack traces or internal exception messages.
- Infrastructure details: hostnames, DB names, cache keys, internal paths, port numbers.
- Timing information that could enable timing-based attacks.
- PII in error payloads: emails, names, account IDs, phone numbers.
- Cryptographic material: tokens, keys, hashes, salts.

Internal server-side logs may include identifiers and context for traceability.
The boundary is the API response — nothing sensitive crosses it.

```json
// Wrong — leaks infrastructure detail
{
  "error": "could not connect to redis://internal-host:6379 — connection refused"
}

// Correct — opaque, maps to a known error code
{
  "error": "TOKEN_REVOCATION_FAILED"
}
```

## Log levels

| Level   | When to use                                                                                 |
|---------|---------------------------------------------------------------------------------------------|
| `ERROR` | Unexpected failures, unhandled exceptions, 5xx responses                                    |
| `WARN`  | Expected failures with impact: auth errors, business rule violations, concurrency conflicts |
| `INFO`  | Significant domain events: successful auth flows, state transitions                         |
| `DEBUG` | Internal state useful during development — disabled in production                           |