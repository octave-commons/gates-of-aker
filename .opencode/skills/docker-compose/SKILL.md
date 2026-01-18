---
name: docker-compose
description: Docker Compose orchestration for multi-container applications
license: MIT
compatibility: opencode
metadata:
  audience: devops
  tools: docker
---

## What I do
- Create docker-compose.yml for service orchestration
- Define multi-stage builds and service dependencies
- Configure volumes, networks, and environment variables
- Set up health checks and restart policies
- Debug container startup and networking issues
- Manage container lifecycle (start, stop, rebuild)

## When to use me
Use me when orchestrating multi-container applications, especially when:
- Setting up local development environments
- Configuring production deployments
- Managing service dependencies and networking
- Debugging container communication issues
- Optimizing image sizes and build times

## Common commands
- `docker-compose up -d` - Start services in detached mode
- `docker-compose down` - Stop and remove containers
- `docker-compose build` - Build images
- `docker-compose logs -f` - Follow logs
- `docker-compose exec service sh` - Execute command in container
- `docker-compose ps` - List running containers

## Service configuration
```yaml
services:
  app:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
    volumes:
      - ./data:/app/data
    depends_on:
      - db
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

## Networking patterns
- Create custom networks for service isolation
- Use service names as hostnames (e.g., `db:5432`)
- Expose ports only where necessary (internal vs external)
- Configure DNS resolution for service discovery

## Volume strategies
- Named volumes for persistent data
- Bind mounts for live code reloading
- Temporary volumes for caches and logs
- Backup named volumes before major changes

## Build optimization
- Use multi-stage builds to reduce image size
- Cache dependencies with COPY layers
- Use `.dockerignore` to exclude unnecessary files
- Build once, run multiple times (no build in production containers)

## Health checks
- Define health checks for critical services
- Use `depends_on: {condition: service_healthy}` for ordering
- Implement readiness endpoints in applications
- Configure appropriate timeouts and retries

## Debugging tips
- Use `docker-compose logs --tail=100 service` for recent logs
- Enter container with `docker-compose exec service sh`
- Check networking with `docker network inspect`
- Verify environment variables with `docker-compose config`
- Use `--no-cache` flag when troubleshooting builds
