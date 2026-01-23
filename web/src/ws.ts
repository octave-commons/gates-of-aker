import type { Snapshot, TickData } from "./types";

export type WSMessage =
  | { op: "hello"; state: Snapshot }
  | { op: "tick"; data: TickData }
  | { op: "tick_delta"; data: any }
  | { op: "trace"; data: any }
  | { op: "reset"; state: Snapshot }
  | { op: "levers"; levers: any }
  | { op: "shrine"; shrine: any }
  | { op: "mouthpiece"; mouthpiece: any }
  | { op: "tiles"; tiles: any }
  | { op: "stockpiles"; stockpiles: any }
  | { op: "jobs"; jobs: any }
  | { op: "runner_state"; running: boolean; fps: number }
  | { op: "tick_health"; data: { targetMs: number; tickMs: number; health: "healthy" | "degraded" | "unhealthy" | "unknown" } }
  | { op: "error"; message: string }
  | { op: string; [k: string]: any };

function isValidMessage(msg: unknown): msg is WSMessage {
  return (
    typeof msg === "object" &&
    msg !== null &&
    "op" in msg &&
    typeof (msg as WSMessage).op === "string"
  );
}

export class WSClient {
  private ws: WebSocket | null = null;
  private isConnected = false;

  constructor(
    private url: string,
    private onMessage: (m: WSMessage) => void,
    private onStatus: (s: "open" | "closed" | "error") => void
  ) {}

  connect() {
    if (this.isConnected) {
      return;
    }

    this.ws = new WebSocket(this.url);
    this.ws.onopen = () => {
      this.isConnected = true;
      this.onStatus("open");
    };
    this.ws.onclose = () => {
      this.isConnected = false;
      this.onStatus("closed");
    };
    this.ws.onerror = () => this.onStatus("error");
    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data);
        if (isValidMessage(msg)) {
          this.onMessage(msg);
        } else {
          console.error("[WS] Invalid message format:", msg);
        }
      } catch (e) {
        console.error("[WS] Failed to parse message:", e);
      }
    };
  }

  send(msg: any) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify(msg));
  }

  close() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.isConnected = false;
  }

   sendPlaceWallGhost(pos: [number, number]) {
     this.send({ op: "place_wall_ghost", pos });
   }

   sendPlaceStockpile(pos: [number, number], resource: string, maxQty?: number) {
     this.send({ op: "place_stockpile", pos, resource, max_qty: maxQty });
   }

   sendPlaceBuilding(type: string, pos: [number, number], config?: any) {
      const msg: any = { op: `place_${type}`, pos };
      if (config) {
        Object.assign(msg, config);
      }
      this.send(msg);
    }

    sendQueueBuild(structure: string, pos: [number, number], stockpile?: { resource?: string; max_qty?: number }) {
      const msg: any = { op: "queue_build", structure, pos };
      if (stockpile) {
        msg.stockpile = stockpile;
      }
      this.send(msg);
    }

    sendAssignJob(jobType: string, targetPos: [number, number], agentId: number) {
      this.send({ op: "assign_job", job_type: jobType, target_pos: targetPos, agent_id: agentId });
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
    }
