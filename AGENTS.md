
## Docs & Knowledge Base

- `HACK.md` captures the creative vision; skim it before implementing new features to preserve thematic intent.
- `docs/notes` should read like a changelog of facts; include API contracts, schemas, and non-obvious algorithms there.
 - When adding future AGENTS files in subdirectories, explicitly reference this file to clarify inheritance.
 
- [[ESTIMATION]]
- [[BUILD]]
- [[BACKEND]]
- [[FRONTEND]]
- [[OBSIDIAN]]  -> `OBSIDIAN.md` - Obsidian Specific Instructions for knowledge graphing and plugins
- [[README]] -> `README.md`- Project overview, basic setup and testing commands, explains how to use it and what it is for.
- [[HACK.md]] - High-level vision and inspirations
- [[HOME]] - Obsidian home page, day to day thinking and note taking. Obsidian data views welcome.
- [[MISSION]]
- [[DIRECTORIES]]
- [[TOOLCHAINS]]
- [[LINTING]]
- [[LOGGING]]
- [[TESTING]]

- [[orphaned files output]]
- [[2026-01-15-roadmap]] - Most current roadmap
- [[docs/notes/planning/2026-01-15-roadmap.md]] - Sprint and milestone roadmap
- [[docs/notes/planning/2026-01-19-milestone3-3.5-progress-review.md]] - Milestone 3 & 3.5 detailed progress
- [[spec/2026-01-15-core-loop.md]] - Day/night cycle mechanics
- [[spec/2026-01-15-myth-engine.md]] - Myth engine

## Shared Conventions & Error Handling
- Naming: kebab-case for files (Clojure namespaces), camelCase for TS variables, SCREAMING_SNAKE_CASE reserved for constants.
- Imports: absolute first (stdlib), third-party libs, then relative paths; leave a blank line between groups.
- Error handling: catch exceptions at IO boundaries, return informative JSON payloads, and surface user-friendly messages in UI banners.
- Logging: use `fantasia.dev.logging` on backend (log-error, log-warn, log-info, log-debug), controlled via `LOG_LEVEL` env var.
- Frontend logging: console methods are automatically controlled during tests via `VITE_LOG_LEVEL` env var; no changes needed in application code.
- Configuration: prefer env vars or CLI flags; do not hardcode secrets or ports beyond the documented 3000/5173 defaults.
- Backend health check: use `/healthz` endpoint (returns `{"ok":true}`) to verify backend is running.
- Data contracts: keep server + UI payloads in sync; document field names when adding new ops to `/docs/notes`.
- Concurrency: guard shared atoms with `swap!`; avoid manual locking.
- Serialization: always encode WS payloads via `cheshire` and decode them once before branching on `:op`/`op`.
- Comments: short, high-signal, describing "why" rather than "what"; remove stale comments promptly.
- Git hygiene: no generated assets in commits (`web/dist`, `web/node_modules`), no direct pushes to `main` without review.
- Dependency changes: update lockfiles (`package-lock.json`) and mention new libs in PR bodies.
- Documentation updates: whenever behavior changes, add a line to `/docs/notes` summarizing the impact.
- Security: treat WS inputs as user-controlled; never trust UI-sent numbers without validation.
- Performance: profile long-running `sim/tick!` operations before optimizing; measure first.
- Internationalization: not required yet; keep strings central for future extraction.


## GitHub Issue and Spec Labeling

- **[[spec/labeling-system.md]]** - Comprehensive labeling system for GitHub issues and spec files
- All GitHub issues MUST use standardized labels for:
  - **Priority** (critical, high, medium, low)
  - **Component** (backend, frontend, testing, ecs, myth, champion, factions, world)
  - **Type** (bug, feature, refactor, performance, security, documentation, design, testing, task)
  - **Status** (proposed, active, in-review, blocked, waiting, done)
  - **Complexity** (trivial, small, medium, large, xlarge)
  - **Milestone** (1-7, 3.5)
- All spec files MUST include frontmatter with:
  - Type (spec, design, review, status, roadmap)
  - Component (backend, frontend, etc.)
  - Priority (critical, high, medium, low)
  - Status (draft, proposed, approved, implemented, deprecated)
  - Related GitHub issue IDs
  - Estimated effort (story points or hours)
- Cross-reference issues and specs using `Related-Issues: [1, 2, 3]` in spec frontmatter
- Update labels when issue status changes (e.g., `status:active` when work starts)
- See `spec/labeling-system.md` for GitHub CLI commands to create all labels

## Labeling System Summary (2026-01-22)

All GitHub issues and spec files now use standardized labeling system defined in `spec/labeling-system.md`.

**Labels Created:**
- Priority: critical (🔴), high (🟠), medium (🟡), low (🟢)
- Component: backend, frontend, testing, ecs, myth, champion, factions, world, infrastructure
- Type: bug, feature, refactor, performance, security, documentation, design, testing, task
- Status: proposed, active, in-review, blocked, waiting, done
- Complexity: trivial, small, medium, large, xlarge
- Milestone: 1, 2, 3, 3.5, 4, 5, 6, 7

**Spec Frontmatter Added:**
- Core specs (core-loop, myth-engine, champion-agency, roadmap)
- Backend issue specs (CRITICAL-001, TEST-001, SECURITY-001, ARCH-001, ARCH-002, STYLE-001)
- ECS migration README
- Frontend code review
- Milestone 3.5 reports

**GitHub Issues Labeled:**
- Milestone 3-7 issues labeled with appropriate priority, component, type, complexity
- Design specs (myth engine, combat, agent architecture, champion, communication) labeled
- Documentation issues labeled
- Status labels set to `proposed` for all active work

**See `spec/labeling-system.md` for complete labeling system documentation.**
