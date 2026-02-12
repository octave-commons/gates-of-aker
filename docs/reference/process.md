# Process Reference

This document defines the task lifecycle used by specs and implementation work.

## State Flow

`Incoming -> Accepted -> Breakdown -> Ready -> Todo -> In Progress -> In Review -> Testing -> Document -> Done`

## State Intent

- `Incoming`: New request is captured but not triaged.
- `Accepted`: The request is approved to be planned.
- `Breakdown`: The work is decomposed into concrete tasks.
- `Ready`: Tasks are prepared and scoped for execution.
- `Todo`: The next executable task is queued.
- `In Progress`: Active implementation is underway.
- `In Review`: Changes are ready for code review.
- `Testing`: Verification is running (tests, lint, build, diagnostics).
- `Document`: Notes/spec updates are finalized.
- `Done`: Work is complete and verified.

## Execution Rules

- Keep tasks small and independently mergeable where possible.
- Do not skip verification before moving to `Done`.
- If scope expands during `Breakdown`, split before moving to `Ready`.
- Record outcome notes in the relevant spec or docs entry.
