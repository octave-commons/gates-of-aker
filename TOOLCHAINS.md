## Toolchains & Dependencies
- Backend: `clojure` CLI (deps.edn) with http-kit, reitit, cheshire, and project-local namespaces.
- Backend Java requirement: JDK 17+ recommended to match deps.edn features; set `JAVA_HOME` explicitly when using SDKMAN.
- Frontend: Node 20 LTS + npm; Vite handles dev/build, TypeScript is `strict` per tsconfig.
- UI dependencies kept minimal (React, ReactDOM); add new libs only when debug UI truly needs them.
- Package installs happen per-project (`npm install` inside `web`); avoid hoisting to repo root.
- No Cursor (.cursor/rules) or GitHub Copilot instruction files presently exist; encode all rules here.
- Preferred editors: IntelliJ + Cursive for Clojure, VS Code / WebStorm for TS; configure formatting to match sections below.
- Containerized workflows should mount repo root and forward ports 3000 (backend) and 5173 (web dev).