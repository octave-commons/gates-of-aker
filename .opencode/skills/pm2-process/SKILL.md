---
name: pm2-process
description: PM2 process management for Node.js applications in production
license: MIT
compatibility: opencode
metadata:
  audience: devops
  runtime: node
---

## What I do
- Configure PM2 for Node.js application management
- Set up process files and clustering
- Monitor logs, metrics, and process health
- Configure auto-restart strategies and graceful shutdown
- Deploy applications with zero-downtime
- Troubleshoot process crashes and performance issues

## When to use me
Use me when managing Node.js applications in production, especially when:
- Running Node.js services on servers
- Need process clustering and auto-restart
- Monitoring application health and logs
- Deploying with minimal downtime
- Managing environment-specific configurations

## Common commands
- `pm2 start app.js` - Start application
- `pm2 start ecosystem.config.js` - Start with config file
- `pm2 list` - List all processes
- `pm2 logs` - View logs
- `pm2 monit` - Real-time monitoring
- `pm2 reload all` - Zero-downtime reload
- `pm2 restart app` - Restart process
- `pm2 stop all` - Stop all processes
- `pm2 delete all` - Remove all processes

## Ecosystem configuration
```javascript
module.exports = {
  apps: [{
    name: 'my-app',
    script: './dist/index.js',
    instances: 'max',
    exec_mode: 'cluster',
    env: {
      NODE_ENV: 'development',
      PORT: 3000
    },
    env_production: {
      NODE_ENV: 'production',
      PORT: 8080
    },
    error_file: './logs/error.log',
    out_file: './logs/out.log',
    log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
    merge_logs: true,
    autorestart: true,
    max_memory_restart: '1G',
    watch: false
  }]
};
```

## Clustering strategies
- Use `instances: 'max'` for CPU cores
- Use `instances: 4` for fixed cluster size
- Enable `exec_mode: 'cluster'` for multi-instance
- Sticky sessions: `exec_mode: cluster` with load balancer

## Log management
- Configure separate error and output logs
- Rotate logs with `pm2 install pm2-logrotate`
- Set log file size limits and retention
- Use `pm2 flush` to clear logs
- Stream logs to external services (e.g., ELK, Datadog)

## Monitoring
- Use `pm2 monit` for real-time metrics
- Install `pm2-plus` for advanced monitoring
- Set up custom metrics with `pmx`
- Monitor CPU, memory, and event loop lag
- Set up alerts for process crashes

## Graceful shutdown
- Handle SIGTERM signals in application code
- Close database connections before exit
- Drain HTTP connections gracefully
- Use `pm2 reload` instead of `restart` for zero downtime
- Configure `kill_timeout` and `wait_ready`

## Environment management
- Separate configs per environment (`env_production`, `env_staging`)
- Load env vars from `.env` files
- Use `pm2 startup` to generate startup script
- Save process list with `pm2 save`

## Deployment patterns
- Build before starting (`npm run build && pm2 start`)
- Use `pm2 deploy` with git-based deployment
- Implement health checks before accepting traffic
- Rollback with `pm2 reload app --update-env`
