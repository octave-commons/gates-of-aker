import { logDebug, logError } from "./logging";
import type { Snapshot, TickData, Trace } from "./types";

type Payload = Record<string, unknown>;
type TickHealth = {
  targetMs: number;
  tickMs: number;
  "target-ms"?: number;
  "tick-ms"?: number;
  health: "healthy" | "degraded" | "unhealthy" | "unknown";
};

export type WSMessage =
  | { op: "hello"; state: Snapshot }
  | { op: "tick"; data: TickData }
  | { op: "tick_delta"; data: Payload }
  | { op: "trace"; data: Trace }
  | { op: "reset"; state: Snapshot }
  | { op: "levers"; levers: Payload }
  | { op: "shrine"; shrine: Payload }
  | { op: "mouthpiece"; mouthpiece: Payload }
  | { op: "tiles"; tiles: Payload }
  | { op: "stockpiles"; stockpiles: Payload }
  | { op: "jobs"; jobs: Payload | ReadonlyArray<Payload> }
  | { op: "books"; data: { books: Record<string, unknown> } }
  | { op: "agent_path"; agent_id: number; path: ReadonlyArray<[number, number]> }
  | {
      op: "social_interaction";
      data: {
        agent_1_id: number;
        agent_2_id: number;
        interaction_type?: string;
      };
    }
  | { op: "combat_event"; data: { type?: string; [k: string]: unknown } }
  | { op: "runner_state"; running: boolean; fps: number }
  | { op: "tick_health"; data: TickHealth }
  | { op: "error"; message: string };

function asRecord(value: unknown): Payload | null {
  if (typeof value !== "object" || value === null) {
    return null;
  }
  return value as Payload;
}

function isValidMessage(msg: unknown): msg is WSMessage {
  return (
    typeof msg === "object" &&
    msg !== null &&
    "op" in msg &&
    typeof (msg as WSMessage).op === "string"
  );
}

export class WSClient<TMessage extends WSMessage = WSMessage> {
  private ws: WebSocket | null = null;
  private isConnected = false;
  private shouldReconnect = true;
  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly reconnectBaseDelayMs = 250;
  private readonly reconnectMaxDelayMs = 5000;

  constructor(
    private url: string,
    private onMessage: (m: TMessage) => void,
    private onStatus: (s: "open" | "closed" | "error") => void,
    private WebSocketClass: typeof WebSocket = WebSocket
  ) {}

  connect() {
    if (
      this.isConnected ||
      (this.ws !== null && this.ws.readyState === this.WebSocketClass.CONNECTING)
    ) {
      return;
    }

    this.clearReconnectTimer();
    this.shouldReconnect = true;

    this.ws = new this.WebSocketClass(this.url);
    this.ws.onopen = () => {
      this.isConnected = true;
      this.reconnectAttempts = 0;
      this.clearReconnectTimer();
      this.onStatus("open");
    };
    this.ws.onclose = () => {
      this.isConnected = false;
      this.ws = null;
      this.onStatus("closed");
      this.scheduleReconnect();
    };
    this.ws.onerror = () => this.onStatus("error");
    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data);
        logDebug("[WS] Raw message received:", msg);
        if (isValidMessage(msg)) {
          logDebug("[WS] Valid message, op:", msg.op);
          
          // Type-safe logging based on message operation
          if (msg.op === "hello" && msg.state) {
            const state = asRecord(msg.state);
            const agents = Array.isArray(state?.agents) ? state.agents : [];
            const tiles = asRecord(state?.tiles) ?? {};
            logDebug("[WS] Hello message - agents:", agents.length, "tiles:", Object.keys(tiles).length);
          } else if ((msg.op === "tick" || msg.op === "tick_delta") && "data" in msg) {
            const data = asRecord(msg.data);
            const snapshot = asRecord(data?.snapshot);
            const agents = Array.isArray(snapshot?.agents) ? snapshot.agents : [];
            const tiles = asRecord(snapshot?.tiles) ?? {};
            logDebug("[WS] Tick message - agents:", agents.length, "tiles:", Object.keys(tiles).length);
          }
          
          this.onMessage(msg as TMessage);
        } else {
          logError("[WS] Invalid message format:", msg);
        }
      } catch (e) {
        logError("[WS] Failed to parse message:", e, "Raw data:", ev.data);
      }
    };
  }

  send(msg: unknown) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify(msg));
  }

  close() {
    this.shouldReconnect = false;
    this.clearReconnectTimer();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.isConnected = false;
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private scheduleReconnect() {
    if (!this.shouldReconnect || this.reconnectTimer !== null || this.isConnected) {
      return;
    }
    const delay = Math.min(
      this.reconnectMaxDelayMs,
      this.reconnectBaseDelayMs * Math.pow(2, this.reconnectAttempts)
    );
    this.reconnectAttempts += 1;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.shouldReconnect && !this.isConnected) {
        this.connect();
      }
    }, delay);
  }

   sendPlaceWallGhost(pos: [number, number]) {
     this.send({ op: "place_wall_ghost", pos });
   }

   sendPlaceStockpile(pos: [number, number], resource: string, maxQty?: number) {
     this.send({ op: "place_stockpile", pos, resource, max_qty: maxQty });
   }

   sendPlaceBuilding(type: string, pos: [number, number], config?: Payload) {
      const msg: Payload = { op: `place_${type}`, pos };
      if (config) {
        Object.assign(msg, config);
      }
      this.send(msg);
    }

    sendQueueBuild(structure: string, pos: [number, number], stockpile?: { resource?: string; max_qty?: number }) {
      const msg: Payload = { op: "queue_build", structure, pos };
      if (stockpile) {
        msg["stockpile"] = stockpile;
      }
      this.send(msg);
    }

    sendAssignJob(jobType: string, targetPos: [number, number], agentId: number) {
      this.send({ op: "assign_job", job_type: jobType, target_pos: targetPos, agent_id: agentId });
    }

    sendConfigFacets(facetLimit: number, visionRadius: number) {
      this.send({ op: "config_facets", facet_limit: facetLimit, vision_radius: visionRadius });
    }

     sendStartRun() {
       this.send({ op: "start_run" });
     }

     sendStopRun() {
       this.send({ op: "stop_run" });
     }

      sendSetFps(fps: number) {
        this.send({
          op: "set_fps",
          fps,
        });
      }

       sendGetAgentPath(agentId: number) {
         this.send({ op: "get_agent_path", agent_id: agentId });
       }

        sendSetTreeSpreadLevers(spreadProbability: number, minInterval: number, maxInterval: number) {
         this.send({
           op: "set_tree_spread_levers",
           spread_probability: spreadProbability,
           min_interval: minInterval,
           max_interval: maxInterval,
         });
       }

    // Testing hooks
    /** Get current WebSocket instance (for testing) */
    getWebSocket(): WebSocket | null {
      return this.ws;
    }

    /** Get current connection state (for testing) */
    getConnectionState(): boolean {
      return this.isConnected;
    }

    /** Simulate receiving a message (for testing) */
    simulateMessage(message: TMessage) {
      if (this.onMessage) {
        this.onMessage(message);
      }
    }

    /** Simulate WebSocket status change (for testing) */
    simulateStatus(status: "open" | "closed" | "error") {
      if (this.onStatus) {
        this.onStatus(status);
      }
      if (status === "open") {
        this.isConnected = true;
      } else if (status === "closed") {
        this.isConnected = false;
      }
    }
    }
