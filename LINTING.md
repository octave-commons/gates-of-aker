## Linting & Static Analysis
- Backend linting: use `clojure -X:lint` from backend directory (uses clj-kondo with type checking enabled).
- clj-kondo configuration in `.clj-kondo/config.edn` includes type inference, unresolved symbol checks, unused binding warnings, and redundant expression detection.
- Static type checks rely on runtime invariants; prefer `clojure.spec`, `malli`, or custom predicate helpers in new code.
- Frontend linting currently manual; align with ESLint defaults if introduced later.
- For now, rely on TypeScript strict mode plus Vite build errors; fix `tsc` issues before committing.
- Keep imports ordered: built-ins (React, std libs) -> internal modules -> relative files.
- Enforce prettier-like style manually: two-space indentation, double quotes, trailing semicolons, single blank line between blocks.
- Add `.editorconfig` if formatting drift becomes a problem; otherwise cite this section in reviews.
- Document any temporary style deviations inline so future automation can encode them.