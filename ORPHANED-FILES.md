## Orphaned Files Output

This document captures files that are generated or output but not tracked in the main codebase organization.

### Generated Files to Ignore
- `web/dist/` - Built frontend assets
- `web/node_modules/` - Frontend dependencies
- `backend/target/` - Clojure build artifacts
- `.myth/` - Myth engine internal state and deity definitions

### Temporary Output Files
- Logs and stdout from development servers
- Test output files
- Profiling data
- Temporary simulation state dumps

### Cleanup Commands
```bash
# Clean frontend build artifacts
rm -rf web/dist

# Clean backend build artifacts  
rm -rf backend/target

# Clean all generated files
git clean -fd  # Use with caution - removes untracked files
```

### When to Commit Generated Files
- Never commit `web/dist` or `backend/target`
- Only commit changes to `.myth/` if they represent designed myth content
- Documentation generated from code should be committed if it provides value
- Test output should only be committed if it represents test fixtures or expected results