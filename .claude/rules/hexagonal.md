# Hexagonal Architecture — Ports & Adapters

## Layers and ownership

| Layer              | Contains                                                                                 | Depends on             |
|--------------------|------------------------------------------------------------------------------------------|------------------------|
| **Domain**         | Entities, value objects, aggregates, domain services, domain events                      | Nothing outside domain |
| **Application**    | Use cases, application services, command/query handlers, port interfaces                 | Domain only            |
| **Infrastructure** | Controllers, repository implementations, messaging adapters, persistence models, mappers | Application + Domain   |

The dependency rule is absolute: outer layers depend on inner layers, never the reverse.

## Ports

- **Ports are interfaces defined in the application layer**, under `application.port.in` and
  `application.port.out` — they express what the use cases need, not how it is implemented.
  The domain stays free of them: a driving port carries the commands and results of a use case, so it
  cannot live in the domain without dragging the application boundary in with it — and once one family
  of ports lives in the application, splitting the other across a second layer buys nothing.
- **Driven ports** (outbound / secondary): what the application requires from outside
  (e.g. `UserRepository`, `PasswordHasher`, `EventPublisher`). Implemented by infrastructure adapters.
  A port shared by two infrastructure modules that must not see each other lives in `common`
  instead (`RateLimiter`); that is the only exception, and it is enforced by the same rule.
- **Driving ports** (inbound / primary): how the outside world triggers the application
  (e.g. use case interfaces called by controllers). Implemented by the application layer.

## Adapters

- **Adapters live in infrastructure** and implement application ports.
- A persistence adapter implements a repository port.
- A REST controller is a driving adapter — it calls application use cases and **must not depend
  on the domain layer at all**. It maps the external request into an application command of
  primitives; constructing domain types (value objects included) is the use case's job, never the
  adapter's. Driving adapters speak only to the application boundary.
- Driven adapters (persistence, security, notifications) implement application ports and
  therefore legitimately reference domain types — to map persistence models to domain objects and
  back. This domain access is confined to driven adapters; it never extends to driving adapters.
- Adapters translate between external representations and domain objects using mappers.
  Persistence models never cross into the domain.

## Domain rules

- Domain objects (entities, aggregates, value objects) may depend on domain services.
- Domain objects must never receive or import application services, use case classes,
  or any infrastructure concern.
- Validation belongs in the domain — enforce invariants in constructors or factory methods,
  not in controllers or services.

## Application rules

- Use cases orchestrate domain objects and call driven ports.
- One use case per user action. No business logic in use cases — delegate to the domain.
- Use cases receive and return application DTOs (commands/results) or primitives at their
  boundary — **never domain types** (value objects, entities, or aggregates). Commands and results
  are application-layer types built from primitives; the use case constructs the domain value
  objects it needs internally (e.g. `Email.from(command.email())`). This keeps the application's
  contract free of domain types so any driving adapter can call it without depending on the domain.
- Domain events are defined in the domain and published through a driven port — never dispatched directly to
  infrastructure.

## Framework independence

Domain and application layers must remain free of framework dependencies.
Frameworks (web, persistence, DI containers, serialization, etc.) are infrastructure concerns.

- Never import or annotate domain or application classes with framework-specific types
  (persistence annotations, HTTP types, DI container annotations, serialization, etc.).
- Framework wiring (dependency injection, lifecycle hooks, request mapping) belongs exclusively
  in the infrastructure layer.
- Unit tests for domain and application logic must run without starting any framework context — no `@SpringBootTest`, no
  Testcontainers.

## Infrastructure DTOs

DTOs (request/response objects) are pure data carriers — they must not contain mapping
or translation logic that depends on types from another layer.
Even when the dependency direction is technically correct (infra → app), embedding a
factory method like `from(ApplicationResult)` inside a DTO moves adapter logic out of
the adapter and into the data structure.

**Translation between layers belongs exclusively in the adapter** (controller, repository
adapter, etc.), not in the DTO itself.

## Common violations to detect and report

- Domain class importing anything from `infrastructure` or `application` packages.
- Driving adapter (controller or any `infrastructure-web` class) depending on the domain layer —
  it must speak only to application use cases with primitives/DTOs.
- Command or result carrying a domain type (value object, entity, or aggregate) instead of
  primitives — the application boundary must stay free of domain types.
- Business logic living in a controller or repository adapter.
- Domain service receiving an application service as a dependency.
- Use case returning a domain entity instead of a DTO.
- DTO containing a factory method or mapping logic that references another layer's types.

## Naming

- Driven adapters follow `[PortName][Technology]Adapter` (e.g. `AccountRepositoryJpaAdapter`,
  `RefreshTokenRepositoryRedisAdapter`).
- Port names must not include the word `Port`.