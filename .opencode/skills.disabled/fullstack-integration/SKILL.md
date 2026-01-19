---
name: fullstack-integration
description: Full stack integration patterns connecting Clojure backend and React frontend
license: MIT
compatibility: opencode
metadata:
  audience: fullstack-developers
  stack: clojure react typescript
---

## What I do
- Connect Clojure backend APIs to React frontend
- Design WebSocket communication patterns
- Set up API contracts and type definitions
- Configure CORS and proxy settings
- Implement authentication and session management
- Debug integration issues across the stack

## When to use me
Use me when integrating full stack applications, especially when:
- Adding new API endpoints and frontend calls
- Setting up WebSocket connections
- Configuring CORS and proxy settings
- Managing authentication and session state
- Debugging communication between backend and frontend

## API contract patterns
- Define API interfaces in shared types file
- Use discriminated unions for response types
- Document endpoints in `/docs/notes`
- Keep payload keys in snake_case on both sides
- Validate input on backend, trust on frontend

## Example types
```typescript
// types/api.ts
export type WsMessage =
  | { op: "tick"; tick: number; agents: Agent[] }
  | { op: "agent-update"; id: number; x: number; y: number }
  | { op: "error"; message: string };

export interface Agent {
  id: number;
  x: number;
  y: number;
  type: "villager" | "shrine";
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
}
```

## HTTP patterns
- Use `fetch` or axios for API calls
- Implement retry logic for failed requests
- Abort requests on component unmount
- Cache responses with React Query or SWR
- Handle loading and error states in UI

## WebSocket patterns
```typescript
// Use WSClient helper
import { WSClient } from "./ws";

const ws = new WSClient("ws://localhost:3000/ws");

// Send messages
ws.send({ op: "reset", seed: 42 });

// Handle messages
ws.on("tick", (data) => {
  setAgents(data.agents);
});

// Clean up
return () => ws.close();
```

## Clojure backend patterns
- Use `reitit` for HTTP routing
- Provide JSON responses with `cheshire`
- Include CORS headers: `{"access-control-allow-origin" "*"}`
- Send WS ops with `:op` keyword: `{:op "tick" :tick 42}`
- Validate input before processing

## CORS configuration
```clojure
;; Backend HTTP handler
(defn json-resp [status body]
  {:status status
   :headers {"content-type" "application/json"
             "access-control-allow-origin" "*"
             "access-control-allow-methods" "GET, POST, PUT, DELETE, OPTIONS"
             "access-control-allow-headers" "content-type"}
   :body (json/generate-string body)})
```

## Vite proxy configuration
```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      "/api": "http://localhost:3000",
      "/ws": {
        target: "ws://localhost:3000",
        ws: true
      }
    }
  }
});
```

## Environment management
- Backend: Use env vars or CLI flags (not hardcoded)
- Frontend: Use `import.meta.env.VITE_*` for client-side vars
- Share environment config in `/docs/notes`
- Never commit secrets or API keys

## Authentication patterns
- Backend: Generate JWT or session tokens
- Frontend: Store token in localStorage or cookie
- Include token in Authorization header: `Bearer {token}`
- Handle token expiration and refresh

## Debugging integration
- Log WS payloads on both sides with structure
- Use browser DevTools Network tab for HTTP
- Check console for WebSocket connection errors
- Verify CORS headers in DevTools
- Test endpoints with `curl` before frontend integration

## State synchronization
- Backend sends state updates via WebSocket
- Frontend keeps local state in sync with server
- Use optimistic updates for better UX
- Revert optimistic updates on server error
- Implement conflict resolution for concurrent updates

## Error handling
- Backend: Return structured errors `{:error "message"}`
- Frontend: Display user-friendly error messages
- Log full errors on backend, short messages on frontend
- Implement retry for transient errors
- Show fallback UI for failed requests

## Testing integration
- Test backend endpoints independently (curl, Postman)
- Test frontend with mock responses (MSW)
- Test full stack with docker-compose
- Record WS payloads for manual verification
- Document smoke-test flows in PRs
