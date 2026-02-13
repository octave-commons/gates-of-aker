
## Docs & Knowledge Base

**🚨 AGENT INSTRUCTIONS: Always read linked documents before implementing features!**

- `HACK.md` captures the creative vision; **skim this first** before implementing new features to preserve thematic intent.
- `docs/notes` should read like a changelog of facts; include API contracts, schemas, and non-obvious algorithms there.
- When adding future AGENTS files in subdirectories, **explicitly reference this file** to clarify inheritance.

### Core Documentation (Read These First)
- [README](README.md) -> `README.md` - Project overview, basic setup and testing commands
- [HACK.md](HACK.md) - High-level vision and inspirations  
- [[MISSION]] - Scope and governance principles
- [[ROADMAP.md]] -> `ROADMAP.md` - Current milestones and priorities

### Development Workflow
- [[ESTIMATION]] -> `ESTIMATION.md` - Story points and estimation guidelines
- [[BUILD]] -> `BUILD.md` - Build/run/watch commands for both frontend and backend
- [[TESTING]] -> `TESTING.md` - Testing procedures and framework usage
- [`web/src/__tests__/e2e/README.md`](web/src/__tests__/e2e/README.md) - WebSocket E2E testing guide
- [[LINTING]] -> `LINTING.md` - Code style and static analysis rules
- [[LOGGING]] -> `LOGGING.md` - Backend logging configuration and best practices

### Technical Architecture  
- [[BACKEND]] -> `BACKEND.md` - Clojure backend patterns and conventions
- [[FRONTEND]] -> `FRONTEND.md` - TypeScript/React frontend style guide
- [[DIRECTORIES]] -> `DIRECTORIES.md` - Project structure and organization
- [[TOOLCHAINS]] -> `TOOLCHAINS.md` - Development tools and environment setup
- [ecosystem.pm2.edn](ecosystem.pm2.edn) -> `ecosystem.pm2.edn` - PM2 process management configuration

### Knowledge Management
- [[OBSIDIAN]] -> `OBSIDIAN.md` - Obsidian specific instructions for knowledge graphing
- [[HOME]] -> `HOME.md` - Obsidian home page for daily notes and thinking
- [[orphaned files output]] -> `ORPHANED-FILES.md` - Generated files and cleanup procedures

### Specifications (Critical for Implementation)
- [[docs/planning/2026-01-15-roadmap.md]] - Sprint and milestone roadmap
- [[docs/planning/2026-01-19-milestone3-3.5-progress-review.md]] - Milestone 3 & 3.5 detailed progress  
- [[spec/2026-01-15-core-loop.md]] - Day/night cycle mechanics
- [[spec/2026-01-15-myth-engine.md]] - Myth engine specification
- [[spec/labeling-system.md]] - GitHub issue and spec labeling system

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

## Development Server Management

**🚨 IMPORTANT: Both backend and frontend run automatically via PM2 and restart on code changes!**

- **NEVER manually restart the backend or frontend** while developing
- PM2 automatically detects file changes and restarts the services
- Backend runs as `gates-backend` process, frontend as `gates-frontend`
- If services are unresponsive, check log files instead of restarting:
  - Backend logs: `backend/logs/backend.log`, `backend/logs/backend-error.log`
  - Frontend logs: `backend/logs/frontend.log`, `backend/logs/frontend-error.log`
  - For recent logs: `tail -50 backend/logs/backend-error.log`
- PM2 handles all process management, including crash recovery
- Focus on writing code - PM2 handles the rest
- Serialization: always encode WS payloads via `cheshire` and decode them once before branching on `:op`/`op`.
- Comments: short, high-signal, describing "why" rather than "what"; remove stale comments promptly.
- Status reporting: Use conservative, factual language. Avoid declaring work "complete" or "final" when any dependencies, TODOs, or integration work remains. State exactly what was accomplished and emphasize what work remains next.
- Git hygiene: no generated assets in commits (`web/dist`, `web/node_modules`), no direct pushes to `main` without review.
- Dependency changes: update lockfiles (`package-lock.json`) and mention new libs in PR bodies.
- Documentation updates: whenever behavior changes, add a line to `/docs/notes` summarizing the impact.
- Security: treat WS inputs as user-controlled; never trust UI-sent numbers without validation.
- Performance: profile long-running `sim/tick!` operations before optimizing; measure first.
- Internationalization: not required yet; keep strings central for future extraction.
- **Testing**: Run backend tests with `cd backend && clojure -X:test`, backend coverage with `cd backend && clojure -X:coverage`, and frontend tests with `cd web && npm test`. Run WebSocket E2E tests with `cd web && npm run test:websocket:e2e` to validate game fundamentals against a real backend instance.
- **linting** Backend tests are ran with `cd backend && clojure -X:lint`


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

## RELEVANT SKILLS
These skills are configured for this directory's technology stack and workflow.

### clojure-namespace-architect
Resolves Clojure namespace-path mismatches and classpath errors with definitive path conversion

### clojure-quality
Auto-fix Clojure delimiters and validate syntax with OpenCode tools.

### clojure-syntax-rescue
Protocol to recover from Clojure/Script syntax errors, specifically bracket mismatches and EOF errors.

### git-safety-check
Protocol to ensure safe git operations and avoid detached HEAD or dirty commits.

### github-integration
Perform GitHub operations across all tracked repositories in orgs/**, including issue/PR management, repository synchronization, and automation workflows

### submodule-ops
Make safe, consistent changes in a workspace with many git submodules under orgs/**

### test-preservation
Protocol to forbid deleting or skipping tests to make builds pass.

### testing-bun
Set up and write tests using Bun's built-in test runner for maximum performance and TypeScript support

### testing-clojure-cljs
Set up and write tests for Clojure and ClojureScript projects using cljs.test, cljs-init-tests, and shadow-cljs

### testing-e2e
Write end-to-end tests that verify complete user workflows and critical system paths across the full stack

### testing-general
Apply testing best practices, choose appropriate test types, and establish reliable test coverage across the codebase

### testing-integration
Write integration tests that verify multiple components work together correctly with real dependencies

### testing-nx
Configure and run tests across multiple projects using Nx affected detection for efficient workspace testing

### testing-typescript-ava
Set up and write tests using Ava test runner for TypeScript with minimal configuration and fast execution

### testing-typescript-vitest
Set up and write tests using Vitest for TypeScript projects with proper configuration and TypeScript support

### testing-unit
Write fast, focused unit tests for individual functions, classes, and modules with proper isolation and mocking

### work-on-in_progress-task
Execute the best next work for a task currently in `in_progress`.

### work-on-todo-task
Execute the best next work for a task currently in `todo`.

### workspace-lint
Lint all TypeScript and markdown files across the entire workspace, including all submodules under orgs/**

### workspace-typecheck
Type check all TypeScript files across the entire workspace, including all submodules under orgs/**, using strict TypeScript settings
