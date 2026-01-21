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
  [key: string]: any;
};

export type PathPoint = AxialCoords;

export const hasPos = (agent?: Agent | null): agent is Agent & { pos: [number, number] } =>
  !!agent && Array.isArray(agent.pos) && agent.pos.length === 2 && agent.pos.every((v) => typeof v === "number");
