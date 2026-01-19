# Contributing to Gates of Aker

Thank you for your interest in contributing! This document provides guidelines for contributing to the Gates of Aker project.

## Project Structure

### Backend (Clojure)

The backend is organized with a clear separation between source code and tests:

```
backend/
├── src/
│   └── fantasia/
│       ├── server.clj              # HTTP/WebSocket server
│       ├── dev/                    # Development tools (logging, watching, coverage)
│       │   ├── coverage.clj
│       │   ├── logging.clj
│       │   └── watch.clj
│       └── sim/                    # Simulation logic (pure functions)
│           ├── ecs/                # Entity Component System
│           │   ├── adapter.clj      # ECS → old-style map adapters
│           │   ├── components.clj   # ECS component definitions
│           │   ├── core.clj        # ECS core functions
│           │   ├── tick.clj        # ECS tick orchestration
│           │   └── systems/        # ECS systems (movement, needs-decay)
│           ├── spatial_facets.clj   # Facet-based perception system
│           ├── memories.clj          # Memory creation and decay
│           └── [other modules]
└── test/                           # ALL test files go here
    └── fantasia/
        └── sim/
            ├── ecs/                # ECS tests mirror src structure
            │   ├── adapter_simple_test.clj
            │   ├── comprehensive_test.clj
            │   └── [other ECS tests]
            ├── agents_test.clj       # Core simulation tests
            ├── world_test.clj
            └── [other tests]
```

**Important Rules:**
- **ALL** test files must be in `/backend/test/`
- Test file structure mirrors `/backend/src/` namespace structure
- Test namespace: `fantasia.some-ns-test` → file: `fantasia/some_ns_test.clj`
- No test files or helper test directories in `/backend/src/`
- Test-only helper modules (e.g., `fantasia.sim.ecs-helpers`) should be in `/backend/test/`

### Frontend (TypeScript + React)

```
web/
├── src/
│   ├── App.tsx                 # Main React component
│   ├── main.tsx                # Application entry point
│   ├── ws.ts                   # WebSocket client helper
│   ├── components/              # (if needed in future)
│   └── utils/                  # (if needed in future)
├── public/                     # Static assets
├── package.json
└── vite.config.ts
```

## Development Workflow

### Prerequisites
- Backend: Clojure CLI, JDK 17+
- Frontend: Node 20 LTS, npm

### Backend Development

```bash
cd backend

# Download dependencies
clojure -P

# Run dev server (http://localhost:3000)
clojure -M:server

# Hot reload loop (restarts on file changes)
clojure -X:watch-server

# Run tests
clojure -X:test

# Run coverage report
clojure -X:coverage
# Report: backend/target/coverage/lcov.info

# Run single test
clojure -X:test :only 'fantasia.sim.core-test/tick-advances-world'

# Lint source code
clj-kondo --lint src
```

### Frontend Development

```bash
cd web

# Install dependencies
npm install

# Run dev server (http://localhost:5173)
npm run dev

# Run tests
npm test
npm run test:watch
npm run test:coverage

# Build for production
npm run build

# Type-check only
npm exec tsc --noEmit
```

### Running Coverage Reports

**Backend:**
```bash
cd backend
clojure -X:coverage
```
Coverage report generated in: `backend/target/coverage/lcov.info`

**Frontend:**
```bash
cd web
npm run test:coverage
```

## Testing Guidelines

### Backend Testing

1. **Test Location**: All tests must be in `/backend/test/`
2. **Naming Convention**:
   - Namespace: `fantasia.sim.core-test` (append `-test` to source namespace)
   - File: `fantasia/sim/core_test.clj` (convert `.` to `/`, add `_test.clj`)
3. **Test Structure**:
   ```clojure
   (ns fantasia.sim.core-test
     (:require [clojure.test :refer :all]
               [fantasia.sim.core :as core]))

   (deftest test-something
     (is (= (core/some-function) expected)))
   ```

4. **Running Tests**:
   - Full suite: `clojure -X:test`
   - Single test: `clojure -X:test :only 'fantasia.sim.core-test/test-something'`

### Frontend Testing

1. **Test Location**: `/web/src/**/*.test.tsx`
2. **Naming Convention**: `*.test.tsx` or `*.test.ts`
3. **Test Structure**:
   ```typescript
   import { describe, it, expect } from 'vitest'

   describe('Component', () => {
     it('should render', () => {
       expect(true).toBe(true)
     })
   })
   ```

## Code Style

### Clojure (Backend)
- Use `clj-kondo --lint src` to check code style
- Follow existing patterns in the codebase
- Prefer pure functions over side effects
- Use descriptive variable names
- Keep functions focused and small

### TypeScript (Frontend)
- Use `npm exec tsc --noEmit` to type-check
- Follow existing React hooks patterns
- Use TypeScript strict mode
- Prefer functional components with hooks

## Submitting Changes

1. **Branch**: Create a feature branch from `main`
2. **Test**: Run all tests and ensure they pass
3. **Coverage**: Run coverage reports and check that coverage doesn't decrease
4. **Commit**: Write clear, descriptive commit messages
5. **PR**: Open a pull request with description of changes

## Getting Help

- Read `AGENTS.md` for detailed agent instructions
- Check `HACK.md` for project vision and inspirations
- Review `docs/notes/` for factual documentation
- Look at existing tests for examples

## Coverage Goals

- Aim for high test coverage on new features
- Document reasons for low coverage in specific modules
- Run coverage reports before submitting PRs
- Use coverage data to identify areas needing tests
