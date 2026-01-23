import { CONFIG } from "./config/constants";

const clamp01 = (x: number): number => Math.max(0, Math.min(1, x));

const fmt = (n: unknown): string => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

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

const getMovementSteps = (stats?: Record<string, number>) => {
  const dex = getDexterity(stats);
  const base = 1 + Math.floor(dex * 2);
  const roadBonus = 1 + Math.floor(dex * 3);
  return {
    base,
    road: base + roadBonus,
  };
};

 type DeltaSnapshot = {
   delta: true;
   tick: number;
   global_updates: {
     tick: number;
     temperature: number;
     daylight: number;
     calendar?: any;
     levers?: any;
     map?: any;
   };
   changed_agents: Record<number, any>;
   changed_tiles?: Record<string, any>;
   changed_items?: Record<string, any>;
   changed_stockpiles?: Record<any, any>;
   changed_jobs?: any;
   combat_events?: any[];
   mentions?: any[];
   traces?: any[];
    attribution?: any;
    social_interactions?: any[];
    books?: any;
    changed_tile_visibility?: Record<string, any>;
    changed_revealed_tiles_snapshot?: Record<string, any>;
    tile_visibility?: Record<string, any>;
    "tile-visibility"?: Record<string, any>;
    revealed_tiles_snapshot?: Record<string, any>;
    "revealed-tiles-snapshot"?: Record<string, any>;
    visibility?: Record<string, any>;
  };

 const applyAgentDeltas = (agents: any[], agentDeltas: Record<string, any>): any[] => {
   const deltaIds = new Set<string>(Object.keys(agentDeltas));
   const removedIds = new Set<number>();

   for (const [id, delta] of Object.entries(agentDeltas)) {
     if (delta.removed) {
       removedIds.add(Number(id));
     }
   }

    const updatedAgents = agents
       .map(agent => {
         const agentId = String(agent.id);
         const delta = agentDeltas[agentId];
         if (delta && !delta.removed) {
           const updated = { ...agent, ...delta };
           if (delta.relationships && typeof delta.relationships === 'object' && !Array.isArray(delta.relationships)) {
             const relMap = delta.relationships as Record<string, any>;
             const relArray = Object.entries(relMap)
               .map(([targetId, rel]) => ({
                 'agent-id': Number(targetId),
                 name: String(targetId),
                 affinity: rel.affinity ?? 0.5,
                 'last-interaction': rel['last-interaction']
               }))
               .sort((a, b) => (b.affinity || 0.5) - (a.affinity || 0.5))
               .slice(0, 3);
             updated.relationships = relArray;
           }
           return updated;
         }
         if (!removedIds.has(agent.id) && !deltaIds.has(agentId)) {
           return agent;
         }
         return null;
       })
       .filter((a): a is any => a !== null);

   const updatedIds = new Set(updatedAgents.map(a => a.id));
   const toAdd = Object.entries(agentDeltas)
     .filter(([id, delta]) => !updatedIds.has(Number(id)) && !delta.removed)
     .map(([id, delta]) => ({ id: Number(id), ...delta }));

   return [...updatedAgents, ...toAdd];
 };

  const applyDelta = (prev: any, delta: DeltaSnapshot): any => {
    console.log("[applyDelta] FULL delta:", JSON.stringify(delta, null, 2));
    console.log("[applyDelta] Delta keys:", Object.keys(delta || {}));
    console.log("[applyDelta] Changed agents:", delta?.changed_agents);
    console.log("[applyDelta] Changed agents count:", Object.keys(delta?.changed_agents || {}).length);
    const updated = { ...(prev || {}) };
 
   if (delta.global_updates) {
     updated.tick = delta.global_updates.tick;
     updated.temperature = delta.global_updates.temperature;
     updated.daylight = delta.global_updates.daylight;
     if (delta.global_updates.calendar) updated.calendar = delta.global_updates.calendar;
     if (delta.global_updates.levers) updated.levers = delta.global_updates.levers;
     if (delta.global_updates.map) updated.map = delta.global_updates.map;
   }
 
   if (delta.changed_agents) {
     updated.agents = applyAgentDeltas(prev.agents || [], delta.changed_agents);
   }
 
    if (delta.changed_tiles) {
      const normalizedTiles: Record<string, any> = {};
      for (const [key, value] of Object.entries(delta.changed_tiles)) {
        const normalizedKey = key.includes("[") 
          ? key.replace(/^\[(-?\d+)[,\s]+(-?\d+)\]$/, (_, q, r) => `${q},${r}`)
          : key;
        normalizedTiles[normalizedKey] = value;
      }
      updated.tiles = { ...(prev.tiles || {}), ...normalizedTiles };
    }
 
    if (delta.changed_items) {
      const normalizedItems: Record<string, any> = {};
      for (const [key, value] of Object.entries(delta.changed_items)) {
        const normalizedKey = key.includes("[") 
          ? key.replace(/^\[(-?\d+)[,\s]+(-?\d+)\]$/, (_, q, r) => `${q},${r}`)
          : key;
        normalizedItems[normalizedKey] = value;
      }
      updated.items = { ...(prev.items || {}), ...normalizedItems };
    }
 
    if (delta.changed_stockpiles) {
      const normalizedStockpiles: Record<string, any> = {};
      for (const [key, value] of Object.entries(delta.changed_stockpiles)) {
        const normalizedKey = key.includes("[") 
          ? key.replace(/^\[(-?\d+)[,\s]+(-?\d+)\]$/, (_, q, r) => `${q},${r}`)
          : key;
        normalizedStockpiles[normalizedKey] = value;
      }
      updated.stockpiles = { ...(prev.stockpiles || {}), ...normalizedStockpiles };
    }
 
   if (delta.changed_jobs) {
     updated.jobs = delta.changed_jobs;
   }
 
   if (delta.combat_events && updated.combat_events) {
     updated.combat_events = [...(prev.combat_events || []), ...delta.combat_events];
   }
 
   if (delta.mentions && updated.mentions) {
     updated.mentions = [...(prev.mentions || []), ...delta.mentions];
   }
 
   if (delta.traces && updated.traces) {
     updated.traces = [...(prev.traces || []), ...delta.traces];
   }
 
   if (delta.attribution) {
     updated.attribution = delta.attribution;
   }
 
   if (delta.social_interactions) {
     updated.social_interactions = [...(prev.social_interactions || []), ...delta.social_interactions];
   }
 
    if (delta.books) {
      updated.books = delta.books;
    }

    if (delta.tile_visibility) {
      updated.tile_visibility = delta.tile_visibility;
    } else if (delta["tile-visibility"]) {
      updated.tile_visibility = delta["tile-visibility"];
    }

    if (delta.revealed_tiles_snapshot) {
      updated.revealed_tiles_snapshot = delta.revealed_tiles_snapshot;
    } else if (delta["revealed-tiles-snapshot"]) {
      updated.revealed_tiles_snapshot = delta["revealed-tiles-snapshot"];
    }

    if (delta.visibility) {
      updated.visibility = delta.visibility;
    }

    return updated;
 };

export { clamp01, fmt, colorForRole, getAgentIcon, getMovementSteps, applyDelta };
