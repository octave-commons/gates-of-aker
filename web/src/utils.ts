import { CONFIG } from "./config/constants";

type UnknownRecord = Record<string, unknown>;

type DeltaSnapshot = {
  delta: true;
  tick: number;
  global_updates: {
    tick: number;
    temperature: number;
    daylight: number;
    calendar?: UnknownRecord;
    levers?: UnknownRecord;
    map?: UnknownRecord;
  };
  changed_agents: Record<string, UnknownRecord>;
  changed_tiles?: Record<string, unknown>;
  changed_items?: Record<string, unknown>;
  changed_stockpiles?: Record<string, unknown>;
  changed_jobs?: unknown;
  combat_events?: UnknownRecord[];
  mentions?: UnknownRecord[];
  traces?: UnknownRecord[];
  attribution?: UnknownRecord;
  social_interactions?: UnknownRecord[];
  books?: UnknownRecord;
  changed_tile_visibility?: Record<string, unknown>;
  changed_revealed_tiles_snapshot?: Record<string, unknown>;
  tile_visibility?: Record<string, unknown>;
  "tile-visibility"?: Record<string, unknown>;
  revealed_tiles_snapshot?: Record<string, unknown>;
  "revealed-tiles-snapshot"?: Record<string, unknown>;
  visibility?: Record<string, unknown>;
  changed_agent_visibility?: Record<number, string[]>;
};

const clamp01 = (x: number): number => Math.max(0, Math.min(1, x));

const fmt = (n: unknown): string => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

const safeStringify = (obj: unknown, space: number | string = 0): string => {
  const seen = new WeakSet();
  return JSON.stringify(obj, (key, value) => {
     if (typeof value === "object" && value !== null) {
       if (seen.has(value)) {
         return "[Circular]";
       }
       seen.add(value);
     }
    return value;
  }, space);
};

const colorForRole = (role?: string): string => {
  switch (role) {
    case "priest":
      return CONFIG.colors.ROLE.priest;
    case "knight":
      return CONFIG.colors.ROLE.knight;
    case "champion":
      return "#0f766e";
    case "wolf":
      return "#795548";
    case "bear":
      return "#5d4037";
    case "deer":
      return "#8d6e63";
    default:
      return CONFIG.colors.ROLE.default;
  }
};

const getAgentIcon = (role?: string): string => {
  switch (role) {
    case "priest":
      return "✝";
    case "knight":
      return "⚔";
    case "champion":
      return "🜂";
    case "wolf":
      return "🐺";
    case "bear":
      return "🐻";
    case "deer":
      return "🦌";
    default:
      return "👤";
  }
};

const getDexterity = (stats?: Record<string, number>): number => {
  if (stats && typeof stats.dexterity === "number") {
    return stats.dexterity;
  }
  return 0.4;
};

const getMovementSteps = (stats?: Record<string, number>): { base: number; road: number } => {
  const dex = getDexterity(stats);
  const base = 1 + Math.floor(dex * 2);
  const roadBonus = 1 + Math.floor(dex * 3);
  return {
    base,
    road: base + roadBonus,
  };
};

const coerceAgentId = (id: string): string | number => {
  const numericId = Number(id);
  if (Number.isFinite(numericId) && String(numericId) === id) {
    return numericId;
  }
  return id;
};

const applyAgentDeltas = (agents: UnknownRecord[], agentDeltas: Record<string, UnknownRecord>): UnknownRecord[] => {
  const deltaIds = new Set<string>(Object.keys(agentDeltas));
  const removedIds = new Set<string>();

  for (const [id, delta] of Object.entries(agentDeltas)) {
    if (delta.removed === true) {
      removedIds.add(id);
    }
  }

  const updatedAgents = agents
    .map(agent => {
      const agentId = String(agent.id);
      const delta = agentDeltas[agentId];
          if (delta && delta.removed !== true) {
            const updated = { ...agent, ...delta };
            if (delta.relationships && typeof delta.relationships === 'object' && !Array.isArray(delta.relationships)) {
              const relMap = delta.relationships as Record<string, UnknownRecord>;
              const relArray = Object.entries(relMap)
                .map(([targetId, rel]) => ({
                  'agent-id': Number(targetId),
                  name: String(targetId),
                  affinity: typeof rel.affinity === "number" ? rel.affinity : 0.5,
                  'last-interaction': rel['last-interaction']
                }))
                .sort((a, b) => (b.affinity || 0.5) - (a.affinity || 0.5))
                .slice(0, 3);
              updated.relationships = relArray;
            }
            return updated;
          }
          if (!removedIds.has(agentId) && !deltaIds.has(agentId)) {
            return agent;
          }
          return null;
        })
        .filter((a): a is UnknownRecord => a !== null);

  const updatedIdKeys = new Set(updatedAgents.map((a) => String(a.id)));
  const toAdd = Object.entries(agentDeltas)
      .filter(([id, delta]) => !updatedIdKeys.has(id) && delta.removed !== true)
      .map(([id, delta]) => ({ id: coerceAgentId(id), ...delta }));

  return [...updatedAgents, ...toAdd];
};

const normalizeTileKey = (rawKey: string) => {
  const trimmed = rawKey.trim();
  if (trimmed.includes(",") && !trimmed.includes("[") && !trimmed.includes("]")) {
    return trimmed.replace(/\s+/g, "");
  }
  const match = trimmed.match(/^\[(-?\d+)[,\s]+(-?\d+)\]$/);
  if (match) {
    return `${match[1]},${match[2]}`;
  }
  return trimmed;
};

const normalizeKeyedMap = <T,>(input: Record<string, T> | null | undefined): Record<string, T> => {
  if (!input || typeof input !== "object") return {} as Record<string, T>;
  const normalized: Record<string, T> = {};
  for (const [key, value] of Object.entries(input)) {
    const normalizedKey = normalizeTileKey(key);
    normalized[normalizedKey] = value;
  }
  return normalized;
};

const appendBounded = <T>(items: readonly T[], item: T, limit: number): T[] => {
  if (limit <= 0) {
    return [];
  }
  if (items.length < limit) {
    return [...items, item];
  }
  const next = items.slice(1);
  next.push(item);
  return next;
};

const appendManyBounded = <T>(items: readonly T[], incoming: readonly T[], limit: number): T[] => {
  if (limit <= 0) {
    return [];
  }
  if (incoming.length === 0) {
    return [...items];
  }
  if (incoming.length >= limit) {
    return incoming.slice(incoming.length - limit);
  }
  const overflow = items.length + incoming.length - limit;
  if (overflow <= 0) {
    return [...items, ...incoming];
  }
  return [...items.slice(overflow), ...incoming];
};

const applyDelta = (prev: UnknownRecord | null | undefined, delta: DeltaSnapshot): UnknownRecord => {
  const previous = (prev ?? {}) as UnknownRecord;
  const updated: UnknownRecord = { ...previous };

  if (delta.global_updates) {
    updated.tick = delta.global_updates.tick;
    updated.temperature = delta.global_updates.temperature;
    updated.daylight = delta.global_updates.daylight;
    if (delta.global_updates.calendar) updated.calendar = delta.global_updates.calendar;
    if (delta.global_updates.levers) updated.levers = delta.global_updates.levers;
    if (delta.global_updates.map) updated.map = delta.global_updates.map;
    }

  if (delta.changed_agents) {
    const prevAgents = Array.isArray(previous.agents) ? (previous.agents as UnknownRecord[]) : [];
    updated.agents = applyAgentDeltas(prevAgents, delta.changed_agents);
  }

  if (delta.changed_tiles) {
       const normalizedTiles: Record<string, unknown> = {};
       for (const [key, value] of Object.entries(delta.changed_tiles)) {
         const normalizedKey = normalizeTileKey(key);
         normalizedTiles[normalizedKey] = value;
       }
       const prevTiles = (previous.tiles && typeof previous.tiles === "object") ? (previous.tiles as Record<string, unknown>) : {};
       updated.tiles = { ...prevTiles, ...normalizedTiles };
  }

  if (delta.changed_items) {
       const normalizedItems: Record<string, unknown> = {};
       for (const [key, value] of Object.entries(delta.changed_items)) {
         const normalizedKey = normalizeTileKey(key);
         normalizedItems[normalizedKey] = value;
       }
       const prevItems = (previous.items && typeof previous.items === "object") ? (previous.items as Record<string, unknown>) : {};
       updated.items = { ...prevItems, ...normalizedItems };
  }

  if (delta.changed_stockpiles) {
       const normalizedStockpiles: Record<string, unknown> = {};
       for (const [key, value] of Object.entries(delta.changed_stockpiles)) {
         const normalizedKey = normalizeTileKey(key);
         normalizedStockpiles[normalizedKey] = value;
       }
       const prevStockpiles = (previous.stockpiles && typeof previous.stockpiles === "object") ? (previous.stockpiles as Record<string, unknown>) : {};
       updated.stockpiles = { ...prevStockpiles, ...normalizedStockpiles };
  }

  if (delta.changed_jobs) {
      updated.jobs = delta.changed_jobs;
    }

  if (Array.isArray(delta.combat_events)) {
      const prevCombatEvents = Array.isArray(previous.combat_events) ? (previous.combat_events as UnknownRecord[]) : [];
      updated.combat_events = appendManyBounded(prevCombatEvents, delta.combat_events, CONFIG.data.MAX_EVENTS);
    }

  if (Array.isArray(delta.mentions)) {
      const prevMentions = Array.isArray(previous.mentions) ? (previous.mentions as UnknownRecord[]) : [];
      updated.mentions = [...prevMentions, ...delta.mentions];
    }

  if (Array.isArray(delta.traces)) {
      const prevTraces = Array.isArray(previous.traces) ? (previous.traces as UnknownRecord[]) : [];
      updated.traces = appendManyBounded(prevTraces, delta.traces, CONFIG.data.MAX_TRACES);
    }

  if (delta.attribution) {
      updated.attribution = delta.attribution;
    }

    if (delta.social_interactions) {
      const prevSocialInteractions = Array.isArray(previous.social_interactions)
        ? (previous.social_interactions as UnknownRecord[])
        : [];
      updated.social_interactions = [...prevSocialInteractions, ...delta.social_interactions];
    }

    if (delta.books) {
      updated.books = delta.books;
    }

  if (delta.changed_agent_visibility) {
      updated.changed_agent_visibility = delta.changed_agent_visibility;
  }

  if (delta.revealed_tiles_snapshot) {
      updated.revealed_tiles_snapshot = delta.revealed_tiles_snapshot;
    }

  if (delta["revealed-tiles-snapshot"]) {
      updated.revealed_tiles_snapshot = delta["revealed-tiles-snapshot"];
    }

  if (delta.tile_visibility) {
      updated.tile_visibility = delta.tile_visibility;
    } else if (delta["tile-visibility"]) {
      updated.tile_visibility = delta["tile-visibility"];
    }

  return updated;
};

export {
  clamp01,
  fmt,
  colorForRole,
  getAgentIcon,
  getMovementSteps,
  applyDelta,
  safeStringify,
  normalizeKeyedMap,
  appendBounded,
  appendManyBounded,
};
