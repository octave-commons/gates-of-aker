---
name: clojure-dev
description: Clojure development workflows including REPL, deps.edn, testing, and linting
license: MIT
compatibility: opencode
metadata:
  audience: backend-developers
  language: clojure
---

## What I do
- Set up Clojure development environment (deps.edn, CLI aliases)
- Start interactive REPLs and connect to running processes
- Run tests with clojure.test (single test, namespace, or full suite)
- Apply clj-kondo linting and interpret its output
- Manage dependencies and resolve version conflicts
- Debug runtime issues using REPL-driven development

## When to use me
Use me when working with Clojure projects, especially when:
- Setting up or modifying deps.edn configurations
- Writing or running tests
- Debugging Clojure code interactively
- Adding new dependencies or resolving conflicts
- Linting code with clj-kondo

## Common commands
- `clojure -P` - Download dependencies without running
- `clojure -M:alias` - Run with alias (e.g., `:server`, `:test`)
- `clojure -X:alias` - Run with exec-style alias (key-value args)
- `clojure -M:repl` - Start interactive REPL
- `clojure -X:test` - Run tests (when configured)
- `clj-kondo --lint src` - Lint source code

## Testing patterns
- Single test: `(clojure.test/test-var #'namespace/function-name)`
- Namespace: `(clojure.test/run-tests 'namespace)`
- Full suite: configured via test runner alias (e.g., `:test`)
- Use deterministic seeds for reproducible tests

## REPL best practices
- Connect to running REPLs via nREPL or socket REPL
- Use `require` with `:reload` to reload modified code
- Evaluate expressions in the REPL to verify changes
- Keep REPL sessions open for iterative development

## Project structure
- `deps.edn` - Dependencies and aliases
- `src/` - Source code following namespace convention
- `test/` - Test files (`-test` suffix)
- `resources/` - Configuration files and static assets
