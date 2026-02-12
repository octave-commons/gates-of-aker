# ADR-0001: Docs-First Migration Scope Guardrail

## Status
Accepted

## Context
- The migration inventory records a documentation-heavy change set: `files.include=157` with `154` moved and `3` external copies.
- Migration execution also recorded `3` README exceptions caused by target conflicts (`docs/design/README.md`, `docs/history/README.md`, `docs/planning/README.md`).
- Live implementation namespaces remain `fantasia` in backend code paths (`backend/src/fantasia/**`, `backend/test/fantasia/**`).
- The current phase objective is documentation normalization and traceability, not code namespace refactoring.

## Decision
- Limit this phase to docs-first migration work under `docs/**`, `spec/**` (as documentation relocation), and `pseudo/**`.
- Prohibit namespace, package, or filesystem renames in backend/frontend implementation trees during this phase.
- Require explicit decision records and supersession mappings to preserve migration intent and prevent accidental scope expansion.

## Out of Scope (Guardrail)
- Any code-path rename under `backend/src/fantasia/**` or `backend/test/fantasia/**`.
- Namespace/package identifier rewrites in implementation code.
- Runtime contract changes coupled to naming migration.

## Consequences
- Positive: migration remains reversible, auditable, and low-risk for runtime behavior.
- Positive: documentation taxonomy can stabilize before any code-level rename planning.
- Negative: docs and code naming are temporarily mixed (`Gates of Aker` docs with `fantasia.*` code namespaces).
- Follow-up: code rename planning must be staged as a separate, test-backed effort with dependency sequencing.

## Supersession and Dependencies
- Supersedes: implicit migration practice without an explicit docs-first scope boundary.
- Superseded by: none.
- Depends on: `docs/migration/inventory.json` as the migration execution ledger.
- Related: ADR-0002 (taxonomy targets), ADR-0003 (terminology and deferred rename rationale).

## Anti-Regression Notes
- Do not treat docs naming normalization as approval for global `fantasia -> gates-of-aker` code renames.
- Any future namespace migration must include a dedicated compatibility and test plan across backend, frontend, and deployment scripts.
