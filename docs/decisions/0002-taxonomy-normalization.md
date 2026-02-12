# ADR-0002: Normalize Documentation Taxonomy

## Status
Accepted

## Context
- Pre-migration documentation was distributed across `docs/notes/**`, `docs/tasks/**`, `spec/**`, and selected external workspace docs.
- Migration controls introduced a normalized taxonomy with canonical destinations: `docs/design/`, `docs/history/`, `docs/planning/`, `docs/decisions/`, and `pseudo/`.
- Inventory and execution results show deterministic outcomes and tracked exceptions rather than ad-hoc movement.

## Decision
- Adopt normalized taxonomy folders as canonical entry points for project documentation.
- Preserve historical material by moving it under `docs/history/**` rather than deleting or flattening context.
- Keep pseudo-code and non-integrated DSL artifacts under `pseudo/**` to avoid mixing implementation guidance with prototypes.
- Record exception handling explicitly in migration artifacts and decision records.

## Out of Scope (Guardrail)
- Reclassifying documentation categories ad hoc outside the normalized taxonomy.
- Mixing active design/planning docs back into a generic `docs/notes/**` catch-all.

## Consequences
- Positive: discovery and maintenance improve through stable top-level categories.
- Positive: historical context remains available without contaminating active design and planning paths.
- Positive: future link and index rewrites can target deterministic destinations.
- Negative: moved spec links may temporarily break until the dedicated link-integrity pass completes.

## Supersession and Dependencies
- Supersedes: mixed-placement taxonomy pattern across legacy `docs/notes/**`, `docs/tasks/**`, and `spec/**`.
- Superseded by: none.
- Depends on: ADR-0001 docs-first scope guardrail.
- Related: `docs/migration/classification-rubric.md`, `docs/migration/inventory.json`.

## Anti-Regression Notes
- Do not reintroduce broad `docs/notes/**` as a mixed catch-all for active and historical docs.
- Preserve category semantics (`design`, `history`, `planning`, `decisions`, `pseudo`) to keep inventory classification deterministic.
