import type { HexConfig } from "../hex";

export type Trace = Record<string, any>;

export type AxialCoords = [number, number];

export type Agent = {
  id: number;
  name?: string;
  pos: [number, number] | null;
  role: string;
  faction?: string;
  stats?: Record<string, number>;
  needs: Record<string, number>;
  recall: Record<string, number>;
  current_job?: string | number | null;
  status?: AgentStatus;
  [key: string]: any;
};

export type AgentStatus = {
  "alive?"?: boolean;
  alive?: boolean;
  "cause-of-death"?: string;
  causeOfDeath?: string;
};

export type PathPoint = AxialCoords;

export type Tile = {
  biome?: string;
  terrain?: string;
  resource?: string;
  structure?: string;
  [key: string]: any;
};

export type TileData = Record<string, Tile>;

export type ItemEntry = Record<string, number>;

export type ItemData = Record<string, ItemEntry>;

export type Stockpile = {
  resource?: string;
  ":resource"?: string;
  currentQty?: number;
  "current-qty"?: number;
  maxQty?: number;
  "max-qty"?: number;
};

export type StockpileData = Record<string, Stockpile>;

export type Job = {
  id: string | number;
  type: string;
  target: [number, number];
  worker?: number;
  progress?: number;
  [key: string]: any;
};

export type Calendar = {
  day?: number;
  year?: number;
  season?: string;
  [key: string]: any;
};

export type Deity = {
  name?: string;
  favor?: number;
  domains?: string[];
  [key: string]: any;
};

export type Deities = Record<string, Deity>;

export type Snapshot = {
  tick?: number;
  temperature?: number;
  daylight?: number;
  agents?: Agent[];
  tiles?: TileData;
  items?: ItemData;
  stockpiles?: StockpileData;
  jobs?: Job[];
  ledger?: Record<string, any>;
  shrine?: [number, number];
  calendar?: Calendar;
  favor?: number;
  deities?: Deities;
  map?: HexConfig;
  [key: string]: any;
};

export type TickData = {
  tick: number;
  snapshot?: Snapshot;
  [key: string]: any;
};

export const hasPos = (agent?: Agent | null): agent is Agent & { pos: [number, number] } =>
  !!agent && Array.isArray(agent.pos) && agent.pos.length === 2 && agent.pos.every((v) => typeof v === "number");
