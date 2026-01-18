---
name: vite-dev
description: Vite development server setup, building, hot reload, and plugin configuration
license: MIT
compatibility: opencode
metadata:
  audience: frontend-developers
  bundler: vite
---

## What I do
- Set up Vite projects with React and TypeScript
- Configure Vite plugins and optimization options
- Run dev server with hot module replacement
- Build production bundles and analyze output
- Configure proxy settings for backend APIs
- Debug build issues and dependency conflicts

## When to use me
Use me when working with Vite-based frontend projects, especially when:
- Setting up new Vite projects or configurations
- Configuring dev server and proxy settings
- Building and optimizing production bundles
- Debugging HMR or build issues
- Adding Vite plugins

## Common commands
- `npm run dev` - Start dev server (default port 5173)
- `npm run build` - Build for production
- `npm run preview` - Preview production build locally
- `vite optimize` - Pre-bundle dependencies
- `vite --debug` - Run with debug output

## Configuration patterns
- `server.port` - Change dev server port
- `server.proxy` - Proxy backend requests (e.g., `{"/api": "http://localhost:3000"}`)
- `build.outDir` - Output directory (default: `dist`)
- `plugins` - Add plugins (e.g., `@vitejs/plugin-react`)

## TypeScript integration
- Enable strict mode in tsconfig.json
- Use Vite's `tsconfig.paths` support
- Type-check with `tsc --noEmit` before building
- Import with `.js` extensions (Vite transpiles)

## Environment variables
- Load from `.env`, `.env.local`, `.env.production`
- Access via `import.meta.env.VARIABLE_NAME`
- Prefix with `VITE_` for client-side variables

## Performance tips
- Lazy load routes and components
- Use dynamic imports for code splitting
- Configure `build.chunkSizeWarningLimit` for large bundles
- Analyze bundle size with `rollup-plugin-visualizer`

## HMR troubleshooting
- Check console for HMR connection errors
- Verify `vite.config.ts` exports default config
- Ensure file watchers work (increase limit on Linux)
- Restart dev server if HMR fails repeatedly
