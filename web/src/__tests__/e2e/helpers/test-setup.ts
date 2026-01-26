import type { Snapshot, Agent, Tile, Stockpile } from '../../../types';

export interface TestConfig {
  backendUrl: string;
  timeout: number;
  testSeed: number;
  testTreeDensity: number;
}

export const DEFAULT_TEST_CONFIG: TestConfig = {
  backendUrl: process.env.VITE_BACKEND_ORIGIN ? 
    `${process.env.VITE_BACKEND_ORIGIN.replace('http://', 'ws://').replace('https://', 'wss://')}/ws` : 
    'ws://localhost:3000/ws',
  timeout: 10000,
  testSeed: 42,
  testTreeDensity: 0.1
};

export const SAMPLE_SNAPSHOTS = {
  INITIAL: {
    tick: 0,
    temperature: 20.0,
    daylight: 1.0,
    agents: [
      {
        id: 1,
        pos: [0, 0],
        role: 'colonist',
        needs: { warmth: 0.8, food: 0.7, sleep: 0.9 },
        inventory: { wood: 0, food: 1 },
        recall: {},
        current_job: null
      }
    ],
    tiles: {
      '0,0': { terrain: 'ground', biome: 'plains' },
      '1,0': { terrain: 'ground', biome: 'plains', resource: 'tree' },
      '0,1': { terrain: 'ground', biome: 'plains' },
      '-1,1': { terrain: 'ground', biome: 'plains', resource: 'grain' }
    },
    stockpiles: {},
    jobs: [],
    shrine: [5, 5],
    calendar: { day: 1, year: 1, season: 'spring' },
    favor: 50,
    deities: {}
  } as Snapshot,

  AFTER_TICK: {
    tick: 1,
    temperature: 20.1,
    daylight: 0.99,
    agents: [
      {
        id: 1,
        pos: [1, 0],
        role: 'colonist',
        needs: { warmth: 0.79, food: 0.68, sleep: 0.89 },
        inventory: { wood: 1, food: 1 },
        recall: {},
        current_job: null
      }
    ],
    tiles: {
      '0,0': { terrain: 'ground', biome: 'plains' },
      '1,0': { terrain: 'ground', biome: 'plains' }, // Tree harvested
      '0,1': { terrain: 'ground', biome: 'plains' },
      '-1,1': { terrain: 'ground', biome: 'plains', resource: 'grain' }
    },
    stockpiles: {},
    jobs: [],
    shrine: [5, 5],
    calendar: { day: 1, year: 1, season: 'spring' },
    favor: 50,
    deities: {}
  } as Snapshot
};

export const EXPECTED_OPERATIONS = {
  CONNECTION: ['hello'],
  TICK: ['tick', 'tick_delta', 'trace', 'event'],
  RESET: ['reset'],
  LEVERS: ['levers'],
  BUILDING: ['tiles', 'stockpiles', 'jobs'],
  ERROR: ['error']
};

export const VALID_AGENT_ROLES = ['colonist', 'priest', 'builder', 'gatherer', 'craftsman'];
export const VALID_TERRAIN_TYPES = ['ground', 'mountain', 'water', 'forest'];
export const VALID_BIOMES = ['plains', 'forest', 'desert', 'tundra', 'mountains'];
export const VALID_RESOURCES = ['wood', 'stone', 'food', 'grain', 'ore'];
export const VALID_STRUCTURES = ['wall', 'house', 'workshop', 'stockpile', 'shrine'];

export const BOUNDS = {
  MIN_TICK: 0,
  MAX_TICK: Number.MAX_SAFE_INTEGER,
  MIN_COORD: -50,
  MAX_COORD: 50,
  MIN_AGENTS: 0,
  MAX_AGENTS: 100,
  MIN_NEED: 0.0,
  MAX_NEED: 1.0,
  MIN_TEMP: -20.0,
  MAX_TEMP: 50.0,
  MIN_DAYLIGHT: 0.0,
  MAX_DAYLIGHT: 1.0
};

export function createTestPosition(offset: number = 0): [number, number] {
  const basePositions: [number, number][] = [
    [0, 0], [1, 0], [0, 1], [-1, 1], [-1, 0], [0, -1]
  ];
  const index = offset % basePositions.length;
  const [q, r] = basePositions[index];
  const ring = Math.floor(offset / basePositions.length);
  return [q + ring, r];
}

export function createTestAgent(id: number, pos: [number, number]): Agent {
  return {
    id,
    pos,
    role: VALID_AGENT_ROLES[id % VALID_AGENT_ROLES.length],
    needs: {
      warmth: 0.5 + (Math.random() * 0.5),
      food: 0.5 + (Math.random() * 0.5),
      sleep: 0.5 + (Math.random() * 0.5)
    },
    inventory: {
      wood: Math.floor(Math.random() * 10),
      food: Math.floor(Math.random() * 5)
    },
    recall: {},
    current_job: null
  };
}

export function createTestTile(pos: [number, number]): Tile {
  const biome = VALID_BIOMES[Math.floor(Math.random() * VALID_BIOMES.length)];
  const terrain = VALID_TERRAIN_TYPES[Math.floor(Math.random() * VALID_TERRAIN_TYPES.length)];
  
  const tile: Tile = {
    biome,
    terrain
  };

  // Add resources randomly
  if (Math.random() < 0.3) {
    tile.resource = VALID_RESOURCES[Math.floor(Math.random() * VALID_RESOURCES.length)];
  }

  // Add structures randomly
  if (Math.random() < 0.1) {
    tile.structure = VALID_STRUCTURES[Math.floor(Math.random() * VALID_STRUCTURES.length)];
  }

  return tile;
}

export function createTestStockpile(pos: [number, number], resource?: string): Stockpile {
  return {
    resource: resource || VALID_RESOURCES[Math.floor(Math.random() * VALID_RESOURCES.length)],
    currentQty: Math.floor(Math.random() * 100),
    maxQty: 100 + Math.floor(Math.random() * 100)
  };
}

export function createPerformanceTracker() {
  const start = performance.now();
  const checkpoints: Record<string, number> = {};
  
  return {
    checkpoint(name: string) {
      checkpoints[name] = performance.now() - start;
    },
    
    getDuration(name?: string): number {
      if (name) {
        return checkpoints[name] || 0;
      }
      return performance.now() - start;
    },
    
    getAllCheckpoints(): Record<string, number> {
      return { ...checkpoints };
    }
  };
}