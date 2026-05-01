# Hexagonal Architecture — Ports & Adapters

## Layers and ownership

| Layer | Contains | Depends on |
|---|---|---|
| **Domain** | Entities, value objects, aggregates, domain services, domain events, port interfaces | Nothing outside domain |
| **Application** | Use cases, application services, command/query handlers | Domain only |
| **Infrastructure** | Controllers, repository implementations, messaging adapters, persistence models, mappers | Application + Domain |

The dependency rule is absolute: outer layers depend on inner layers, never the reverse.

## Ports

- **Ports are interfaces defined in the domain** — they express what the domain needs,
  not how it is implemented.
- **Driven ports** (outbound / secondary): what the application requires from outside
  (e.g. `UserRepository`, `PasswordEncoder`, `EventPublisher`). Implemented by infrastructure adapters.
- **Driving ports** (inbound / primary): how the outside world triggers the application
  (e.g. use case interfaces called by controllers). Implemented by the application layer.

## Adapters

- **Adapters live in infrastructure** and implement domain ports.
- A persistence adapter implements a domain repository port.
- A REST controller is a driving adapter — it calls application use cases, never domain objects directly.
- Adapters translate between external representations and domain objects using mappers.
  Persistence models never cross into the domain.

## Domain rules

- Domain objects (entities, aggregates, value objects) may depend on domain services and port interfaces.
- Domain objects must never receive or import application services, use case classes,
  or any infrastructure concern.
- Validation belongs in the domain — enforce invariants in constructors or factory methods,
  not in controllers or services.

## Application rules

- Use cases orchestrate domain objects and call driven ports.
- One use case per user action. No business logic in use cases — delegate to the domain.
- Use cases receive and return DTOs or primitives at their boundary, never domain objects.
- Domain events are defined in the domain and published through a driven port — never dispatched directly to infrastructure.

## Framework independence

Domain and application layers must remain free of framework dependencies.
Frameworks (web, persistence, DI containers, serialization, etc.) are infrastructure concerns.

- Never import or annotate domain or application classes with framework-specific types
  (persistence annotations, HTTP types, DI container annotations, serialization, etc.).
- Framework wiring (dependency injection, lifecycle hooks, request mapping) belongs exclusively
  in the infrastructure layer.
- Unit tests for domain and application logic must run without starting any framework context — no `@SpringBootTest`, no Testcontainers.

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
- Controller calling a domain object directly, bypassing the use case.
- Business logic living in a controller or repository adapter.
- Domain service receiving an application service as a dependency.
- Use case returning a domain entity instead of a DTO.
- DTO containing a factory method or mapping logic that references another layer's types.