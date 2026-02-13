# ADR-0003: Terminology Policy and Deferred Code Namespace Rename

## Status
Accepted

## Context
- Documentation now targets `Gates of Aker` naming for product prose and `gates-of-aker` for slugs/paths where appropriate.
- Runtime code remains implemented under `fantasia` namespaces and paths (for example `backend/src/fantasia/**`).
- Migration notes already identify expected short-term issues (for example moved-doc link breakage) that are scheduled for a later link-integrity task.
- Immediate broad code renames would couple docs migration with runtime refactors, increasing risk and verification scope.

## Decision
- Apply terminology normalization policy to documentation only in this phase.
- Keep live code namespace/path identifiers unchanged until a dedicated rename plan is approved.
- When legacy names are required for factual accuracy (paths, historical quotes, code references), retain them with context rather than rewriting implementation truth.

## Out of Scope (Guardrail)
- Broad `fantasia` -> `gates-of-aker` implementation rename in backend/frontend code.
- API/message/schema contract rewrites tied to namespace migration in this docs phase.

## Consequences
- Positive: docs become user-facing consistent without destabilizing backend/frontend runtime surfaces.
- Positive: deferred rename allows staged planning for namespace, API contract, test fixtures, and deployment updates.
- Negative: temporary naming duality persists between docs and code.
- Required follow-up: produce a staged namespace migration spec that includes compatibility boundaries, test gates, and rollback strategy.

## Supersession and Dependencies
- Supersedes: ad hoc terminology usage that conflated docs naming with implementation namespace refactors.
- Superseded by: none.
- Depends on: ADR-0001 for docs-only scope guardrail and ADR-0002 for canonical docs destinations.
- Related: `docs/decisions/decision-log.md` terminology map.

## Anti-Regression Notes
- Reject broad search-and-replace renames across code until a dedicated staged plan exists.
- Guard against accidental namespace drift by treating `fantasia` implementation identifiers as authoritative until the dedicated migration executes.
