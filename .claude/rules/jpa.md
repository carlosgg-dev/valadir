# JPA Standards — Spring Data JPA & Hibernate

## N+1 prevention
- Proactively identify and prevent N+1 query problems before they reach production.
- Use Join Fetch or Entity Graphs for associations that are always needed together.
- Use explicit projections (interfaces or Records) when only a subset of fields is needed —
  never load full entities just to read two fields.

## Fetch strategy
- `FetchType.LAZY` is the default for all associations. Never override to `EAGER` globally.
- Load eagerly only at the query level (Join Fetch, Entity Graph) when the use case requires it.

## Query hygiene
- Prefer derived query methods or JPQL for simple queries.
- Use native queries only when JPQL cannot express the operation or performance requires it.
  Document the reason inline.
- Never build queries by string concatenation — always use named parameters.