## Directory Map
- `/backend`: Clojure server exposing HTTP/WebSocket endpoints plus simulation logic in `fantasia.sim.*`.
  - `/backend/src`: Source code organized by namespace (no test files here).
  - `/backend/test`: All test files, matching namespace structure of `src`.
- `/web`: React 19 + Vite + TypeScript UI for viewing and steering the myth debugger.
- `/docs`: Notes, inbox thoughts, and long-form design commentary (`HACK.md`).
- `/docs/notes`: Evergreen facts; prefer appending instead of in-place rewrites.