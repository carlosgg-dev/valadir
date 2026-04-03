# Architecture Decision Rule - Java & Spring Boot

Activate this rule when: starting a new module, discussing project structure,
or when the user asks which architectural style to apply.

## Before proposing any structure, ask:

1. **Scale**: is this a small focused service, a medium multi-module app, or a large domain-rich system?
2. **Team**: solo or multiple developers?
3. **External integrations**: how many? (databases, queues, third-party APIs...)
4. **Expected lifespan and change rate**: throwaway prototype or long-term maintained product?

Do not assume. Ask these questions explicitly if the answers are not already in context.

## Decision criteria

| Signal | Suggested style |
|---|---|
| Small service, few integrations, short lifespan | Layered (Controller → Service → Repository) |
| Medium complexity, needs testability, some domain logic | Layered with clear package boundaries |
| Rich domain logic, multiple adapters, long lifespan | Hexagonal (Ports & Adapters) |
| Large monolith being decomposed | Feature-based modules, each with its own internal style |

## Rules regardless of style chosen

- Agree on the structure **before** creating any class or package.
- Document the decision in a brief ADR (Architecture Decision Record) comment in the README
  if the choice is non-obvious.
- Never mix styles within the same module — pick one and stay consistent.
- If the project already has an established style, follow it unless there is an explicit
  refactor task. Flag deviations as tech debt.

## Smell check before implementing

Before writing code for a new feature or module, scan the existing codebase for:
- Duplicated logic across layers (DRY violation)
- Services doing persistence logic directly (layer leakage)
- Domain entities carrying HTTP or JPA annotations (boundary violation)
- God classes / services with more than one clear responsibility (SRP violation)

Report findings and propose a remediation plan before proceeding.