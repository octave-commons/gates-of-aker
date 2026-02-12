# Decision Log

This log indexes migration decisions and provides a machine-scannable supersession trail from legacy paths to canonical taxonomy paths.

## Decision Index

| Decision ID | Title | Status | Date | Supersedes | Superseded By |
| --- | --- | --- | --- | --- | --- |
| ADR-0001 | Docs-First Migration Scope Guardrail | accepted | 2026-02-10 | none | none |
| ADR-0002 | Normalize Documentation Taxonomy | accepted | 2026-02-10 | mixed `docs/notes` and `docs/tasks` placement policy | none |
| ADR-0003 | Terminology Policy and Deferred Code Namespace Rename | accepted | 2026-02-10 | implied global rename pressure during docs migration | none |

## Decision Dependencies

| decision_id | depends_on | rationale |
| --- | --- | --- |
| ADR-0001 | none | Establishes docs-first boundary and out-of-scope constraints. |
| ADR-0002 | ADR-0001 | Taxonomy normalization must remain docs-only to avoid code-scope drift. |
| ADR-0003 | ADR-0001, ADR-0002 | Terminology policy requires docs-first boundary and canonical destinations. |

## Supersession Map (Path-Level)

Notes:
- `status=conflict-exception` means source remained due pre-existing destination content conflict and was recorded as an execution exception.
- Link rewrite is intentionally sequenced after migration execution; moved-doc link breakage is tracked as expected interim state.

| old_path | new_canonical_path | status | replacement_rationale | authority |
| --- | --- | --- | --- | --- |
| `docs/notes/design/README.md` | `docs/design/README.md` | conflict-exception | Destination README is canonical taxonomy scaffold; source superseded by destination index. | `docs/migration/inventory.json` migration_execution.exceptions |
| `docs/notes/historical/README.md` | `docs/history/README.md` | conflict-exception | Destination README is canonical taxonomy scaffold; source superseded by destination index. | `docs/migration/inventory.json` migration_execution.exceptions |
| `docs/notes/planning/README.md` | `docs/planning/README.md` | conflict-exception | Destination README is canonical taxonomy scaffold; source superseded by destination index. | `docs/migration/inventory.json` migration_execution.exceptions |
| `docs/tasks/README.md` | `docs/planning/tasks/README.md` | moved | Task docs are normalized under planning taxonomy. | `docs/migration/inventory.json` |
| `spec/2026-01-15-core-loop.md` | `docs/planning/spec/2026-01-15-core-loop.md` | moved | Planning spec retained under canonical planning/spec location. | `docs/migration/inventory.json` |
| `spec/2026-01-15-myth-engine.md` | `docs/planning/spec/2026-01-15-myth-engine.md` | moved | Planning spec retained under canonical planning/spec location. | `docs/migration/inventory.json` |
| `docs/notes/dsl/2026-01-11-clojure-macro-dsl-for-institutions-groups-as-belief-machines.md` | `pseudo/dsl/2026-01-11-clojure-macro-dsl-for-institutions-groups-as-belief-machines.md` | moved | Non-integrated DSL content is explicitly categorized as pseudo-code. | `docs/migration/inventory.json` |

## Supersession Map (Terminology Policy)

| old_name | new_name | scope | status | authority |
| --- | --- | --- | --- | --- |
| `Fantasia` (product/docs prose) | `Gates of Aker` | docs prose and narrative labels | active | ADR-0003 |
| `fantasia` (doc slugs/tags where normalized) | `gates-of-aker` | docs paths/slugs introduced by migration | active | ADR-0003 |
| `fantasia.*` implementation namespaces | `gates-of-aker.*` | backend/frontend code identifiers | deferred | ADR-0001, ADR-0003 |

## Supersession Map (Machine-Scannable JSON)

```json
[
  {
    "old_path": "docs/notes/design/README.md",
    "new_path": "docs/design/README.md",
    "status": "conflict-exception",
    "authority": "docs/migration/inventory.json"
  },
  {
    "old_path": "docs/notes/historical/README.md",
    "new_path": "docs/history/README.md",
    "status": "conflict-exception",
    "authority": "docs/migration/inventory.json"
  },
  {
    "old_path": "docs/notes/planning/README.md",
    "new_path": "docs/planning/README.md",
    "status": "conflict-exception",
    "authority": "docs/migration/inventory.json"
  },
  {
    "old_path": "docs/tasks/README.md",
    "new_path": "docs/planning/tasks/README.md",
    "status": "moved",
    "authority": "docs/migration/inventory.json"
  },
  {
    "old_path": "spec/2026-01-15-core-loop.md",
    "new_path": "docs/planning/spec/2026-01-15-core-loop.md",
    "status": "moved",
    "authority": "docs/migration/inventory.json"
  },
  {
    "old_path": "spec/2026-01-15-myth-engine.md",
    "new_path": "docs/planning/spec/2026-01-15-myth-engine.md",
    "status": "moved",
    "authority": "docs/migration/inventory.json"
  },
  {
    "old_path": "docs/notes/dsl/2026-01-11-clojure-macro-dsl-for-institutions-groups-as-belief-machines.md",
    "new_path": "pseudo/dsl/2026-01-11-clojure-macro-dsl-for-institutions-groups-as-belief-machines.md",
    "status": "moved",
    "authority": "docs/migration/inventory.json"
  }
]
```

## Terminology Map (Machine-Scannable JSON)

```json
[
  {
    "old_name": "Fantasia",
    "new_name": "Gates of Aker",
    "scope": "docs prose and narrative labels",
    "status": "active",
    "authority": "ADR-0003"
  },
  {
    "old_name": "fantasia",
    "new_name": "gates-of-aker",
    "scope": "docs paths/slugs introduced by migration",
    "status": "active",
    "authority": "ADR-0003"
  },
  {
    "old_name": "fantasia.* implementation namespaces",
    "new_name": "gates-of-aker.*",
    "scope": "backend/frontend code identifiers",
    "status": "deferred",
    "authority": "ADR-0001, ADR-0003"
  }
]
```
