# WebSocket E2E Tests

This directory contains end-to-end tests that run against a real backend instance to validate game fundamentals and WebSocket protocol compliance.

## Prerequisites

- Backend server must be running on `http://localhost:3000`
- Health check endpoint must be available at `http://localhost:3000/healthz`
- WebSocket endpoint must be available at `ws://localhost:3000/ws`

## Running the Tests

### Run all E2E tests

```bash
cd web
npm run test:websocket:e2e
```

### Run specific test suite

```bash
cd web
npx vitest run src/__tests__/e2e/websocket-e2e.test.ts --reporter=verbose
```

### Run with backend auto-start (using Docker Compose)

```bash
cd web
npm run test:e2e
```

## Test Configuration

Configure the backend URL via environment variable:

```bash
VITE_BACKEND_ORIGIN=http://localhost:3000 npm run test:websocket:e2e
```

## Test Structure

### Helper Files

- `helpers/backend-client.ts` - WebSocket client wrapper for testing
- `helpers/state-validators.ts` - Snapshot validation utilities
- `helpers/test-setup.ts` - Test configuration and fixtures

### Test Suites

- `websocket-e2e.test.ts` - Main E2E test suite covering:
  - Connection and protocol tests
  - Snapshot validation tests
  - Game mechanics tests
  - Reset operations
  - Structure placement tests
  - Levers and configuration tests
  - Performance and stress tests
  - Error handling tests
  - Integration tests

## Test Categories

### Connection and Protocol Tests
- WebSocket connection establishment
- Hello message structure
- Message handling and parsing

### Snapshot Validation Tests
- Initial state completeness
- Tick advancement consistency
- Agent state integrity
- Tile state validation
- Resource conservation

### Game Mechanics Tests
- Time advancement
- Agent movement
- Need updates
- Multi-tick processing

### Operations Tests
- World reset with different seeds
- Structure placement (walls, stockpiles, shrines)
- Lever adjustments
- Agent path queries

### Performance Tests
- Rapid tick requests
- Large tick counts
- Connection stability

## Test Fixtures

Test fixtures are defined in `helpers/test-setup.ts`:

- `DEFAULT_TEST_CONFIG` - Default test configuration
- `SAMPLE_SNAPSHOTS` - Example valid snapshots
- `EXPECTED_OPERATIONS` - Valid WebSocket operations
- `BOUNDS` - Validation bounds for game state

## Validation

State validators in `helpers/state-validators.ts` check:

- Snapshot schema completeness
- Agent structure and values
- Tile coordinates and properties
- Stockpile quantities
- Job state transitions
- Resource conservation across ticks

## Debugging

To run tests in watch mode with verbose output:

```bash
npx vitest src/__tests__/e2e/websocket-e2e.test.ts --reporter=verbose
```

To debug specific tests, use `test.only` in the test file:

```typescript
it.only('should establish connection', async () => {
  // This test will be the only one run
});
```

## CI/CD Integration

These tests should run in CI/CD pipelines after:

1. Backend server is started and healthy
2. Frontend is built (if needed)
3. Health check passes

Example CI configuration:

```bash
# Start backend
cd backend && clojure -M:server &

# Wait for backend to be healthy
while ! curl -s http://localhost:3000/healthz; do sleep 1; done

# Run E2E tests
cd web && npm run test:websocket:e2e
```

## Troubleshooting

### Connection Refused

Ensure the backend is running:
```bash
curl http://localhost:3000/healthz
```

### Test Timeouts

Increase timeout in `helpers/test-setup.ts`:
```typescript
export const DEFAULT_TEST_CONFIG: TestConfig = {
  timeout: 20000, // Increase from 10000
  // ...
};
```

### State Validation Failures

Check backend logs for errors:
```bash
pm2 logs gates-backend --lines 100
```

## Adding New Tests

1. Add test case to `websocket-e2e.test.ts` following the existing pattern
2. Use `expectValidSnapshot` or `StateValidator` helpers for validation
3. Add any needed fixtures to `helpers/test-setup.ts`
4. Update this README if new test categories are added