---
name: opencode-plugin-dev
description: OpenCode plugin development guidance including structure, events, tools, patterns, and best practices
license: MIT
compatibility: opencode
metadata:
  audience: developers
  category: development
---

# OpenCode Plugin Development

## Plugin Fundamentals

### What is a Plugin?
A plugin is a JavaScript/TypeScript module that extends OpenCode by hooking into events, adding custom tools, and modifying behavior. Plugins can integrate with external services, enforce policies, and automate workflows.

### Plugin Structure
A plugin exports one or more functions that receive a context object and return a hooks object:

```typescript
import type { Plugin } from "@opencode-ai/plugin"

export const MyPlugin: Plugin = async ({ project, client, $, directory, worktree }) => {
  return {
    // Hook implementations
  }
}
```

### Context Object
Plugins receive:
- `project`: Current project information
- `directory`: Current working directory
- `worktree`: Git worktree path
- `client`: OpenCode SDK client for AI interaction
- `$`: Bun's shell API for executing commands

## Event Hooks

### Tool Events
- `tool.execute.before`: Before any tool executes
- `tool.execute.after`: After any tool executes

### Session Events
- `session.created`: New session created
- `session.updated`: Session metadata updated
- `session.deleted`: Session deleted
- `session.status`: Session status changes
- `session.error`: Session errors
- `session.idle`: Session becomes idle
- `session.compacted`: Session compacted
- `session.diff`: Session diff generated

### Message Events
- `message.updated`: Message content changed
- `message.removed`: Message deleted
- `message.part.updated`: Message part changed
- `message.part.removed`: Message part deleted

### File Events
- `file.edited`: File modified
- `file.watcher.updated`: File watcher detected change

### Command Events
- `command.executed`: Slash command executed

### Server Events
- `server.connected`: Client connected to server

### Permission Events
- `permission.updated`: Permissions changed
- `permission.replied`: Permission request responded to

### TUI Events
- `tui.prompt.append`: Text appended to prompt
- `tui.command.execute`: Command executed
- `tui.toast.show`: Toast notification shown

### Installation Events
- `installation.updated`: Installation status changed

### LSP Events
- `lsp.client.diagnostics`: LSP diagnostics available
- `lsp.updated`: LSP status updated

### Todo Events
- `todo.updated`: Todo list changed

## Plugin Installation

### Local Installation
Place files in:
- `.opencode/plugins/` - Project-level
- `~/.config/opencode/plugins/` - Global

### NPM Installation
Add to `opencode.json`:
```json
{
  "plugin": ["my-plugin", "@scope/custom-plugin"]
}
```

### Dependencies
Local plugins can use npm packages. Create `.opencode/package.json`:
```json
{
  "dependencies": {
    "shescape": "^2.1.0"
  }
}
```

OpenCode runs `bun install` at startup to install dependencies.

## Custom Tools

### Basic Tool Definition
```typescript
import { type Plugin, tool } from "@opencode-ai/plugin"

export const MyPlugin: Plugin = async (ctx) => {
  return {
    tool: {
      mytool: tool({
        description: "Tool description",
        args: {
          foo: tool.schema.string().describe("Parameter"),
        },
        async execute(args, ctx) {
          return `Hello ${args.foo}!`
        },
      }),
    },
  }
}
```

### Tool Naming Convention
- Single export: Filename becomes tool name
- Multiple exports: `<filename>_<exportname>` (e.g., `math_add`)

### Tool Context
Tools receive:
- `agent`: Agent ID
- `sessionID`: Current session ID
- `messageID`: Current message ID

### Multi-Tool File
```typescript
import { tool } from "@opencode-ai/plugin"

export const add = tool({
  description: "Add two numbers",
  args: {
    a: tool.schema.number().describe("First number"),
    b: tool.schema.number().describe("Second number"),
  },
  async execute(args) {
    return args.a + args.b
  },
})

export const multiply = tool({
  description: "Multiply two numbers",
  args: {
    a: tool.schema.number(),
    b: tool.schema.number(),
  },
  async execute(args) {
    return args.a * args.b
  },
})
```

## Common Patterns

### Environment Protection
```javascript
export const EnvProtection = async ({ project, client, $, directory, worktree }) => {
  return {
    "tool.execute.before": async (input, output) => {
      if (input.tool === "read" && output.args.filePath.includes(".env")) {
        throw new Error("Do not read .env files")
      }
    },
  }
}
```

### Notifications
```javascript
export const NotificationPlugin = async ({ project, client, $, directory, worktree }) => {
  return {
    event: async ({ event }) => {
      if (event.type === "session.idle") {
        await $`osascript -e 'display notification "Session completed!" with title "opencode"'`
      }
    },
  }
}
```

### Command Escaping
```typescript
import { escape } from "shescape"

export const MyPlugin = async (ctx) => {
  return {
    "tool.execute.before": async (input, output) => {
      if (input.tool === "bash") {
        output.args.command = escape(output.args.command)
      }
    },
  }
}
```

### Structured Logging
```typescript
export const MyPlugin = async ({ client }) => {
  await client.app.log({
    service: "my-plugin",
    level: "info",
    message: "Plugin initialized",
    extra: { foo: "bar" },
  })
  return {}
}
```

Log levels: `debug`, `info`, `warn`, `error`

## Compaction Hooks

### Add Context to Compaction
```typescript
import type { Plugin } from "@opencode-ai/plugin"

export const CompactionPlugin: Plugin = async (ctx) => {
  return {
    "experimental.session.compacting": async (input, output) => {
      output.context.push(`
## Custom Context
- Current task status
- Important decisions made
- Files being actively worked on
      `)
    },
  }
}
```

### Replace Compaction Prompt
```typescript
import type { Plugin } from "@opencode-ai/plugin"

export const CustomCompactionPlugin: Plugin = async (ctx) => {
  return {
    "experimental.session.compacting": async (input, output) => {
      output.prompt = `You are generating a continuation prompt for a multi-agent swarm session.
Summarize:
1. The current task and its status
2. Which files are being modified and by whom
3. Any blockers or dependencies between agents
4. The next steps to complete the work
      `
    },
  }
}
```

## TypeScript Support

### Import Types
```typescript
import type { Plugin } from "@opencode-ai/plugin"

export const MyPlugin: Plugin = async ({ project, client, $, directory, worktree }) => {
  return {
    // Type-safe implementations
  }
}
```

### Zod Schema
```typescript
import { z } from "zod"

export default {
  description: "Tool description",
  args: {
    param: z.string().describe("Parameter description"),
  },
  async execute(args, context) {
    return "result"
  },
}
```

## Load Order

Plugins load in this order:
1. Global config (`~/.config/opencode/opencode.json`)
2. Project config (`opencode.json`)
3. Global plugin directory (`~/.config/opencode/plugins/`)
4. Project plugin directory (`.opencode/plugins/`)

Duplicate npm packages with same name and version load once. Local and npm plugins with similar names load separately.

## Publishing Plugins

### Package Structure
```json
{
  "name": "opencode-my-plugin",
  "version": "1.0.0",
  "type": "module",
  "exports": {
    ".": "./dist/index.js",
    "./package.json": "./package.json"
  },
  "peerDependencies": {
    "@opencode-ai/plugin": "*"
  }
}
```

### Build Process
```bash
# TypeScript
npm install typescript @types/node
npx tsc

# Or use Bun
bun build ./src/index.ts --outdir ./dist --target node
```

### NPM Publishing
```bash
npm login
npm publish
```

Users install via:
```json
{
  "plugin": ["opencode-my-plugin"]
}
```

## Best Practices

### Error Handling
Always throw descriptive errors:
```typescript
"tool.execute.before": async (input, output) => {
  if (someCondition) {
    throw new Error("Descriptive error message")
  }
}
```

### Performance
- Avoid expensive operations in synchronous hooks
- Use `await` for async operations
- Cache expensive results

### Security
- Validate all input
- Sanitize commands before execution
- Never log sensitive data
- Use environment variables for secrets

### Compatibility
- Test with multiple OpenCode versions
- Provide fallbacks for missing features
- Document required permissions

### Debugging
Use structured logging:
```typescript
await client.app.log({
  service: "my-plugin",
  level: "debug",
  message: "Debug information",
  extra: { input, output },
})
```

## Troubleshooting

### Plugin Not Loading
1. Verify file is in correct directory
2. Check TypeScript syntax
3. Review load order conflicts
4. Check permissions in `opencode.json`

### Dependencies Not Found
1. Create `.opencode/package.json`
2. Run `bun install` manually
3. Verify npm package name

### Hook Not Firing
1. Verify event name spelling
2. Check if event is supported
3. Ensure hook returns correctly

## Reference URLs

- [Main Docs](https://opencode.ai/docs)
- [Plugin Docs](https://opencode.ai/docs/plugins/)
- [SDK Docs](https://opencode.ai/docs/sdk/)
- [Server Docs](https://opencode.ai/docs/server/)
- [Skills Guide](https://opencode.ai/docs/skills/)
- [Custom Tools](https://opencode.ai/docs/custom-tools/)
- [Ecosystem](https://opencode.ai/docs/ecosystem/)
