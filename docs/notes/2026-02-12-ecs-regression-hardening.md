## 2026-02-12 ECS Regression Hardening

- Fixed dead-agent lifecycle guards so dead entities no longer move or continue job execution.
- Expanded mortality cleanup to requeue claimed jobs and clear stale `JobAssignment`/`Path` even when entities are already dead before mortality runs.
- Ensured job assignment and idle-build candidate selection use dual liveness checks (`AgentStatus` + `DeathState`).
- Updated ECS snapshot tile assembly to merge ECS tile structures into outgoing snapshot tiles and normalize tile-visibility keys.
- Improved UI naming fallbacks to prefer readable names and avoid exposing full UUID-like identifiers in visibility/selected/thought panels.
- Updated job queue presentation to make priority explicit and sort active jobs by priority.
