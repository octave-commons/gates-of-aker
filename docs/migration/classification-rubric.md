# Migration Classification Rubric

This rubric defines deterministic rules for classifying legacy-fantasia and Gates of Aker documentation during migration.

## Scope

- Input set: markdown docs matched by `(?i)fantasia|gates of aker|gates-of-aker` inside in-scope roots.
- In-scope roots for this migration wave:
  - `docs/**`
  - `spec/**`
  - external placeholder candidates explicitly referenced by the inventory metadata.
- Out-of-scope classes are still recorded in inventory metadata as exclusions.

## Category Definitions

- `design`: canonical architecture/mechanics docs that define how the system should work.
- `history`: archival context, research, brainstorming, and prior-state records kept for traceability.
- `planning`: roadmaps, task specs, issue specs, implementation plans, and execution sequencing docs.
- `decisions`: rationale/assessment artifacts that document why choices were made or evaluated.
- `pseudo`: non-integrated pseudo-code, DSL sketches, and prototype-heavy docs not representing the current codebase.

## Deterministic Rule Order

Apply rules in order; first match wins.

1. If path matches `docs/notes/dsl/**`, classify as `pseudo`.
2. If path matches `docs/notes/design/**`, classify as `design`.
3. If path matches `docs/notes/historical/**`, classify as `history`.
4. If path matches `docs/notes/brainstorming/**`, classify as `history`.
5. If path matches `docs/tasks/**`, classify as `planning`.
6. If path matches `docs/notes/planning/**` or `docs/notes/devops/**`, classify as `planning`.
7. If path matches `spec/**` and basename contains one of:
   - `review`, `assessment`, `status`, `completion`, `implementation-summary`, `analysis`, `code-review`
   classify as `decisions`.
8. If path matches `spec/**` and rule 7 did not match, classify as `planning`.
9. If path matches `docs/notes/**` and no earlier rule matched, classify as `history`.
10. External placeholder docs:
    - `.../docs/notes/fantasia/**` -> `history`
    - `.../gates-of-aker-design-spec/spec/**` -> `planning`
    - `.../gates-of-aker-design-spec/README.md` -> `design`

## Include/Exclude Rules

- Include when all are true:
  - file is markdown,
  - file matches keyword pattern,
  - file is within in-scope roots or approved external placeholder roots,
  - file is not in an excluded class.
- Exclude when any are true:
  - session/tool transcript format (for example `docs/notes/YYYY.MM.DD.*.md`),
  - generated/dependency artifacts,
  - unrelated workspace repositories or notes.

## Proposed Target Path Rules

- `design` -> `docs/design/**`
- `history` -> `docs/history/**`
- `planning` -> `docs/planning/**`
- `decisions` -> `docs/decisions/**`
- `pseudo` -> `pseudo/**`

Path projection uses source subpath preservation when possible (for example `spec/backend-issues/foo.md` -> `docs/planning/backend-issues/foo.md` or `docs/decisions/backend-issues/foo.md` based on category).

## Tie-Breakers

- If a document contains executable-style pseudo-code blocks and is primarily speculative, prefer `pseudo` over `design`.
- If a spec is primarily retrospective/evaluative (review, status, assessment), prefer `decisions` over `planning`.
- If uncertain after path and basename checks, default to `history` and record rationale in inventory.
