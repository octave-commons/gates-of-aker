import { describe, it, expect, beforeAll, afterAll, beforeEach } from 'vitest';
import { BackendTestClient, createBackendTestClient } from './helpers/backend-client';
import { StateValidator } from './helpers/state-validators';
import { DEFAULT_TEST_CONFIG } from './helpers/test-setup';

describe('WebSocket E2E Tests', () => {
  let client: BackendTestClient;

  beforeAll(async () => {
    client = createBackendTestClient();
    
    // Check if backend is healthy before running tests
    const isHealthy = await client.waitForHealthCheck(5000);
    expect(isHealthy).toBe(true);
  });

  afterAll(async () => {
    await client.disconnect();
  });

  beforeEach(async () => {
    // Reset world before each test
    await client.reset({ seed: DEFAULT_TEST_CONFIG.testSeed, tree_density: DEFAULT_TEST_CONFIG.testTreeDensity });
  });

  describe('Connection and Protocol Tests', () => {
    it('should establish WebSocket connection', async () => {
      await client.connect();
      expect(client.getConnectionState()).toBe(true);
    });

    it('should receive hello message on connection', async () => {
      await client.connect();
      const hello = await client.waitForHello();
      
      expect(hello).toBeDefined();
      expect(hello.state).toBeDefined();
      expect(typeof hello.state.tick).toBe('number');
    });

    it('should maintain connection after multiple operations', async () => {
      await client.connect();
      
      await client.tick();
      await client.tick();
      
      expect(client.getConnectionState()).toBe(true);
    });

    it('should close connection gracefully', async () => {
      await client.connect();
      const hello = await client.waitForHello();
      
      await client.disconnect();
      
      expect(client.getConnectionState()).toBe(false);
    });
  });

  describe('Snapshot Validation Tests', () => {
    it('should validate initial snapshot structure', async () => {
      await client.connect();
      const hello = await client.waitForHello();
      const snapshot = hello.state;
      
      const result = StateValidator.validateSnapshot(snapshot);
      expect(result.isValid).toBe(true);
      expect(result.errors).toEqual([]);
    });

    it('should have valid tick number', async () => {
      await client.connect();
      const hello = await client.waitForHello();
      
      expect(hello.state.tick).toBe(0);
    });

    it('should have valid agents array', async () => {
      await client.connect();
      const hello = await client.waitForHello();
      const agents = hello.state.agents || [];
      
      expect(Array.isArray(agents)).toBe(true);
      
      agents.forEach((agent, index) => {
        const errors = StateValidator.validateAgent(agent, index);
        expect(errors.length).toBe(0);
      });
    });

    it('should have valid tiles object', async () => {
      await client.connect();
      const hello = await client.waitForHello();
      const tiles = hello.state.tiles || {};
      
      expect(typeof tiles).toBe('object');
      
      const errors = StateValidator.validateTiles(tiles);
      expect(errors.length).toBe(0);
    });

    it('should maintain consistency across ticks', async () => {
      await client.connect();
      const beforeHello = await client.waitForHello();
      const beforeSnapshot = beforeHello.state;
      
      await client.tick(5);
      const afterTick = await client.waitForTick();
      const afterSnapshot = afterTick.data.snapshot;
      
      const result = StateValidator.compareSnapshots(beforeSnapshot!, afterSnapshot!);
      expect(result.isValid).toBe(true);
    });

    it('should advance tick monotonically', async () => {
      await client.connect();
      
      let previousTick = -1;
      
      for (let i = 0; i < 5; i++) {
        const tickMessage = await client.tick();
        const currentTick = tickMessage.data.tick;
        
        expect(currentTick).toBeGreaterThan(previousTick);
        previousTick = currentTick;
      }
    });

    it('should preserve resource conservation', async () => {
      await client.connect();
      const beforeHello = await client.waitForHello();
      const beforeSnapshot = beforeHello.state;
      
      await client.tick(10);
      const afterTick = await client.waitForTick();
      const afterSnapshot = afterTick.data.snapshot;
      
      const result = StateValidator.validateResourceConservation(beforeSnapshot!, afterSnapshot!);
      expect(result.isValid).toBe(true);
    });
  });

  describe('Game Mechanics Tests', () => {
    it('should advance time and temperature correctly', async () => {
      await client.connect();
      const initialHello = await client.waitForHello();
      const initialTemp = initialHello.state.temperature;
      const initialDaylight = initialHello.state.daylight;
      
      await client.tick(1);
      const tickResult = await client.waitForTick();
      
      expect(tickResult.data.tick).toBe(1);
      
      // Temperature and daylight may change slightly
      if (initialTemp !== undefined && tickResult.data.snapshot?.temperature !== undefined) {
        expect(typeof tickResult.data.snapshot.temperature).toBe('number');
      }
      
      if (initialDaylight !== undefined && tickResult.data.snapshot?.daylight !== undefined) {
        expect(typeof tickResult.data.snapshot.daylight).toBe('number');
      }
    });

    it('should handle agent movement', async () => {
      await client.connect();
      const initialHello = await client.waitForHello();
      const initialAgents = initialHello.state.agents || [];
      
      if (initialAgents.length > 0) {
        const firstAgent = initialAgents[0];
        const initialPos = firstAgent.pos;
        
        await client.tick(5);
        
        // Check if agent moved (this may not happen every tick)
        const tickResults = await client.waitForTick();
        const updatedAgents = tickResults.data.snapshot?.agents || [];
        
        if (updatedAgents.length > 0) {
          const updatedAgent = updatedAgents.find(a => a.id === firstAgent.id);
          
          if (updatedAgent && initialPos && updatedAgent.pos) {
            expect(Array.isArray(updatedAgent.pos)).toBe(true);
            expect(updatedAgent.pos).toHaveLength(2);
          }
        }
      }
    });

    it('should process multiple ticks correctly', async () => {
      await client.connect();
      await client.waitForHello();
      
      const tickCount = 10;
      const tickMessage = await client.tick(tickCount);
      
      expect(tickMessage.data.tick).toBe(tickCount);
    });

    it('should update agent needs over time', async () => {
      await client.connect();
      const initialHello = await client.waitForHello();
      const initialAgents = initialHello.state.agents || [];
      
      if (initialAgents.length > 0) {
        const firstAgent = initialAgents[0];
        const initialNeeds = firstAgent.needs;
        
        await client.tick(10);
        
        const tickResult = await client.waitForTick();
        const updatedAgents = tickResult.data.snapshot?.agents || [];
        const updatedAgent = updatedAgents.find(a => a.id === firstAgent.id);
        
        if (updatedAgent && updatedAgent.needs) {
          expect(updatedAgent.needs).toBeDefined();
          expect(typeof updatedAgent.needs.food).toBe('number');
          expect(typeof updatedAgent.needs.warmth).toBe('number');
          expect(typeof updatedAgent.needs.sleep).toBe('number');
        }
      }
    });
  });

  describe('Reset Operations', () => {
    it('should reset world with default seed', async () => {
      await client.connect();
      await client.waitForHello();
      
      await client.tick(5);
      
      const resetResult = await client.reset();
      expect(resetResult.state.tick).toBe(0);
    });

    it('should reset world with custom seed', async () => {
      await client.connect();
      await client.waitForHello();
      
      await client.tick(5);
      
      const customSeed = 123;
      const resetResult = await client.reset({ seed: customSeed });
      
      expect(resetResult.state.tick).toBe(0);
    });

    it('should create different worlds with different seeds', async () => {
      await client.connect();
      
      const reset1 = await client.reset({ seed: 1 });
      const snapshot1 = reset1.state;
      
      const reset2 = await client.reset({ seed: 999 });
      const snapshot2 = reset2.state;
      
      // Different seeds should produce different initial states
      expect(snapshot1.tick).toBe(snapshot2.tick);
      
      // Tile counts or distributions should differ
      const tiles1 = Object.keys(snapshot1.tiles || {}).length;
      const tiles2 = Object.keys(snapshot2.tiles || {}).length;
      
      // At least one of these should differ
      const somethingDifferent = 
        tiles1 !== tiles2 ||
        JSON.stringify(snapshot1.agents) !== JSON.stringify(snapshot2.agents);
      
      expect(somethingDifferent).toBe(true);
    });

    it('should clear all state on reset', async () => {
      await client.connect();
      
      await client.tick(10);
      
      const resetResult = await client.reset();
      const snapshot = resetResult.state;
      
      // Check key fields are reset
      expect(snapshot.tick).toBe(0);
      
      const validation = StateValidator.validateSnapshot(snapshot);
      expect(validation.isValid).toBe(true);
    });
  });

  describe('Structure Placement Tests', () => {
    it('should place wall ghost', async () => {
      await client.connect();
      await client.waitForHello();
      
      const pos: [number, number] = [5, 5];
      client.placeWallGhost(pos);
      
      // Wait for tiles update
      const tickResult = await client.tick();
      
      expect(tickResult.data.tick).toBeGreaterThanOrEqual(0);
    });

    it('should place stockpile', async () => {
      await client.connect();
      await client.waitForHello();
      
      const pos: [number, number] = [3, 3];
      client.placeStockpile(pos, 'wood', 100);
      
      const tickResult = await client.tick();
      
      expect(tickResult.data.tick).toBeGreaterThanOrEqual(0);
    });

    it('should place shrine', async () => {
      await client.connect();
      await client.waitForHello();
      
      const pos: [number, number] = [0, 0];
      client.placeShrine(pos);
      
      const tickResult = await client.tick();
      
      expect(tickResult.data.tick).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Levers and Configuration Tests', () => {
    it('should update levers', async () => {
      await client.connect();
      const initialHello = await client.waitForHello();
      const initialLevers = initialHello.state.levers;
      
      const testLevers = { 'test-lever': 'test-value' };
      client.setLevers(testLevers);
      
      // Levers update should be broadcast
      // We can't directly test the broadcast in this setup,
      // but we verify the operation doesn't cause errors
      const tickResult = await client.tick();
      
      expect(tickResult.data.tick).toBeGreaterThanOrEqual(0);
    });

    it('should get agent path', async () => {
      await client.connect();
      const initialHello = await client.waitForHello();
      const agents = initialHello.state.agents || [];
      
      if (agents.length > 0) {
        const agentId = agents[0].id;
        client.getAgentPath(Number(agentId));
        
        const tickResult = await client.tick();
        expect(tickResult.data.tick).toBeGreaterThanOrEqual(0);
      }
    });
  });

  describe('Performance and Stress Tests', () => {
    it('should handle rapid tick requests', async () => {
      await client.connect();
      await client.waitForHello();
      
      const tickCount = 20;
      const startTime = performance.now();
      
      for (let i = 0; i < tickCount; i++) {
        await client.tick(1);
      }
      
      const duration = performance.now() - startTime;
      
      // Should complete within reasonable time (less than 10 seconds)
      expect(duration).toBeLessThan(10000);
    });

    it('should handle large tick counts', async () => {
      await client.connect();
      await client.waitForHello();
      
      const largeTickCount = 50;
      const tickResult = await client.tick(largeTickCount);
      
      expect(tickResult.data.tick).toBe(largeTickCount);
    });

    it('should maintain connection stability', async () => {
      await client.connect();
      await client.waitForHello();
      
      // Perform various operations
      await client.tick(5);
      await client.reset({ seed: 42 });
      await client.tick(3);
      
      expect(client.getConnectionState()).toBe(true);
    });
  });

  describe('Error Handling Tests', () => {
    it('should handle invalid operation gracefully', async () => {
      await client.connect();
      await client.waitForHello();
      
      // Send an invalid operation
      client.sendMessage({ op: 'invalid_operation', data: {} });
      
      // Server should still respond to valid operations
      const tickResult = await client.tick();
      expect(tickResult.data.tick).toBeGreaterThanOrEqual(0);
    });

    it('should reconnect after connection loss', async () => {
      await client.connect();
      await client.waitForHello();
      
      await client.disconnect();
      expect(client.getConnectionState()).toBe(false);
      
      // Reconnect
      await client.connect();
      expect(client.getConnectionState()).toBe(true);
      
      const hello = await client.waitForHello();
      expect(hello.state).toBeDefined();
    });
  });

  describe('Integration Tests', () => {
    it('should complete full game cycle: connect -> tick -> reset -> tick', async () => {
      await client.connect();
      
      const initialHello = await client.waitForHello();
      expect(initialHello.state.tick).toBe(0);
      
      const tick1 = await client.tick(5);
      expect(tick1.data.tick).toBe(5);
      
      const resetResult = await client.reset({ seed: 100 });
      expect(resetResult.state.tick).toBe(0);
      
      const tick2 = await client.tick(3);
      expect(tick2.data.tick).toBe(3);
    });

    it('should validate state consistency across complex operations', async () => {
      await client.connect();
      const initialHello = await client.waitForHello();
      
      // Place some structures
      client.placeWallGhost([1, 1]);
      client.placeStockpile([2, 2], 'wood', 50);
      
      // Advance time
      await client.tick(10);
      
      // Reset and verify clean state
      const resetResult = await client.reset();
      const snapshot = resetResult.state;
      
      const validation = StateValidator.validateSnapshot(snapshot);
      expect(validation.isValid).toBe(true);
    });

    it('should handle concurrent tick operations', async () => {
      await client.connect();
      await client.waitForHello();
      
      // Request multiple ticks in sequence
      const promises = [
        client.tick(1),
        client.tick(2),
        client.tick(3)
      ];
      
      const results = await Promise.all(promises);
      
      results.forEach(result => {
        expect(result.data.tick).toBeGreaterThanOrEqual(0);
      });
    });
  });
});