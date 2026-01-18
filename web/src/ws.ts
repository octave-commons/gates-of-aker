export type WSMessage =
    | { op: "hello"; state: any }
    | { op: "tick"; data: any }
    | { op: "trace"; data: any }
    | { op: "reset"; state: any }
    | { op: "levers"; levers: any }
    | { op: "shrine"; shrine: any }
    | { op: "mouthpiece"; mouthpiece: any }
    | { op: "tiles"; tiles: any }
    | { op: "stockpiles"; stockpiles: any }
    | { op: "jobs"; jobs: any }
    | { op: "runner_state"; running: boolean; fps: number }
    | { op: "error"; message: string }
    | { op: string; [k: string]: any };

export class WSClient {
  private ws: WebSocket | null = null;

  constructor(
    private url: string,
    private onMessage: (m: WSMessage) => void,
    private onStatus: (s: "open" | "closed" | "error") => void
  ) {}

  connect() {
    this.ws = new WebSocket(this.url);
    this.ws.onopen = () => this.onStatus("open");
    this.ws.onclose = () => this.onStatus("closed");
    this.ws.onerror = () => this.onStatus("error");
    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data);
        this.onMessage(msg);
      } catch {
        // ignore
      }
    };
  }

  send(msg: any) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify(msg));
  }

  close() {
     this.ws?.close();
   }

   sendPlaceWallGhost(pos: [number, number]) {
     this.send({ op: "place_wall_ghost", pos });
   }

   sendPlaceStockpile(pos: [number, number], resource: string, maxQty?: number) {
     this.send({ op: "place_stockpile", pos, resource, max_qty: maxQty });
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
        this.send({ op: "set_fps", fps });
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

