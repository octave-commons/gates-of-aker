export type AxialCoords = [number, number];
export type Tile = {
    pos: AxialCoords;
    terrain: string;
    structure?: string;
    items?: Record<string, any>;
};
export type Agent = {
    id: number;
    name?: string;
    pos: AxialCoords | null;
    role: string;
    faction?: string;
    stats?: Record<string, number>;
    needs: Record<string, number>;
    recall: Record<string, number>;
    [key: string]: any;
};
export type WorldState = {
    tick: number;
    agents: Agent[];
    tiles: Record<string, Tile>;
    items: Record<string, any>;
    stockpiles: Record<string, any>;
    jobs: any[];
    ledger: Record<string, any>;
    map: {
        bounds: {
            shape: "rect" | "radius";
            w?: number;
            h?: number;
            r?: number;
        };
    };
    shrine: AxialCoords;
    [key: string]: any;
};
export type WSMessage = {
    op: "hello";
    state: WorldState;
} | {
    op: "tick";
    data: {
        tick: number;
        snapshot: WorldState;
    };
} | {
    op: "tick_delta";
    data: any;
} | {
    op: "trace";
    data: any;
} | {
    op: "reset";
    state: WorldState;
} | {
    op: "levers";
    levers: any;
} | {
    op: "shrine";
    shrine: any;
} | {
    op: "mouthpiece";
    mouthpiece: any;
} | {
    op: "tiles";
    tiles: Record<string, Tile>;
} | {
    op: "stockpiles";
    stockpiles: Record<string, any>;
} | {
    op: "jobs";
    jobs: any[];
} | {
    op: "runner_state";
    running: boolean;
    fps: number;
} | {
    op: "tick_health";
    data: {
        targetMs: number;
        tickMs: number;
        health: "healthy" | "degraded" | "unhealthy" | "unknown";
    };
} | {
    op: "error";
    message: string;
} | {
    op: string;
    [k: string]: any;
};
export type ConnectionState = "open" | "closed" | "error";
export type Selection = {
    cell: AxialCoords | null;
    agentId: number | null;
};
//# sourceMappingURL=types.d.ts.map