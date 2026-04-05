# JPA Standards — Spring Data JPA / Hibernate

## Naming
- JPA entity classes use the `Entity` suffix (e.g. `UserEntity`, `AccountEntity`) to clearly
  distinguish them from domain objects and prevent accidental boundary leaks.
- Mapper methods use `toDomain` (entity → domain) and `toEntity` (domain → entity).
  Never name them after the technology (e.g. no `toJpa`, `toHibernate`).

## N+1 prevention
- Proactively identify and prevent N+1 query problems before they reach production.
- Use Join Fetch or Entity Graphs for associations that are always needed together.
- Use explicit projections (interfaces or Records) when only a subset of fields is needed —
  never load full entities just to read two fields.

## Fetch strategy
- `FetchType.LAZY` is the default for all associations. Never override to `EAGER` globally.
- Load eagerly only at the query level (Join Fetch, Entity Graph) when the use case requires it.

## Aggregate boundaries and relationships
- Model FK relationships between aggregates of different bounded contexts as plain UUID columns,
  not as JPA object references (`@OneToOne`, `@ManyToOne`, etc.).
- Use JPA object references only when both entities belong to the same aggregate and
  navigating from one to the other is a legitimate infrastructure concern.
- Never use `CascadeType.REMOVE` or `orphanRemoval` to express business rules
  (e.g. "deleting a user deletes their account"). That logic belongs in the use case.
  The DB schema enforces referential integrity; the use case orchestrates the order of deletions.

## Query hygiene
- Prefer derived query methods or JPQL for simple queries.
- Use native queries only when JPQL cannot express the operation or performance requires it.
  Document the reason inline.
- Never build queries by string concatenation — always use named parameters.