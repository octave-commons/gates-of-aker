import blessed from "blessed";
import { WSClient } from "./ws-client";
import { WSMessage, WorldState, AxialCoords } from "./types";
import { StatusBar } from "./ui/panels/status-bar";
import { MapView } from "./ui/map-view";
import { InfoPanel } from "./ui/panels/info-panel";
import { AgentList } from "./ui/panels/agent-list";

export class App {
  private screen: blessed.Widgets.Screen;
  private wsClient: WSClient;
  private statusBar: StatusBar;
  private mapView: MapView;
  private infoPanel: InfoPanel;
  private agentList: AgentList;
  private state: WorldState | null = null;
  private selectedCell: AxialCoords | null = null;
  private selectedAgentId: number | null = null;
  private isRunning = false;

  constructor() {
    this.screen = blessed.screen({
      smartCSR: true,
      title: "Fantasia TUI",
    });

    const backendOrigin = process.env.BACKEND_ORIGIN || "http://localhost:3000";
    const wsUrl = backendOrigin.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";

    this.wsClient = new WSClient(
      wsUrl,
      this.handleMessage.bind(this),
      this.handleStatusChange.bind(this)
    );

    this.statusBar = new StatusBar(this.screen);
    this.mapView = new MapView(this.screen);
    this.infoPanel = new InfoPanel(this.screen);
    this.agentList = new AgentList(this.screen);

    this.setupKeybindings();
    this.wsClient.connect();

    this.screen.render();
  }

  private handleMessage(msg: WSMessage): void {
    switch (msg.op) {
      case "hello":
        this.state = msg.state;
        this.statusBar.setTick(this.state?.tick ?? 0);
        this.updateUI();
        break;

      case "tick":
        this.state = msg.data.snapshot;
        this.statusBar.setTick(msg.data.tick ?? 0);
        this.updateUI();
        break;

      case "tick_delta":
        if (this.state) {
          this.state.tick = (msg.data as any).tick ?? this.state.tick;
          this.statusBar.setTick(this.state.tick);
          this.updateUI();
        }
        break;

      case "reset":
        this.state = msg.state;
        this.statusBar.setTick(this.state?.tick ?? 0);
        this.selectedCell = null;
        this.selectedAgentId = null;
        this.updateUI();
        break;

      case "runner_state":
        this.isRunning = msg.running;
        this.statusBar.setRunning(this.isRunning);
        break;

      case "error":
        console.error("Backend error:", msg.message);
        this.statusBar.setStatus("error");
        break;

      default:
        break;
    }
  }

  private handleStatusChange(status: "open" | "closed" | "error"): void {
    this.statusBar.setStatus(status);
  }

  private updateUI(): void {
    this.mapView.setState(this.state);
    this.infoPanel.setState(this.state);
    this.infoPanel.setSelectedCell(this.selectedCell);
    this.infoPanel.setSelectedAgentId(this.selectedAgentId);
    this.agentList.setState(this.state);
    this.screen.render();
  }

  private setupKeybindings(): void {
    this.screen.key(["q", "C-c"], () => {
      this.wsClient.close();
      process.exit(0);
    });

    this.screen.key(["t"], () => {
      if (!this.isRunning) {
        this.wsClient.send({ op: "tick", n: 1 });
      }
    });

    this.screen.key(["r"], () => {
      if (this.isRunning) {
        this.wsClient.send({ op: "stop_run" });
      } else {
        this.wsClient.send({ op: "start_run" });
      }
    });

    this.screen.key(["R", "C-r"], () => {
      const seed = Math.floor(Math.random() * 1000000);
      this.wsClient.send({ op: "reset", seed });
    });

    this.screen.key(["escape"], () => {
      this.selectedCell = null;
      this.selectedAgentId = null;
      this.updateUI();
    });

    this.mapView.getElement().key(["up", "down", "left", "right"], (ch, key) => {
      this.handleMapNavigation(key!.name as string);
    });

    this.mapView.getElement().key(["enter"], () => {
      this.handleMapSelection();
    });

    this.mapView.getElement().on("click", () => {
      this.handleMapSelection();
    });
  }

  private handleMapNavigation(direction: string): void {
    if (!this.state) return;

    const bounds = this.state.map?.bounds;
    if (!bounds) return;

    if (!this.selectedCell) {
      this.selectedCell = [Math.floor((bounds.w || 20) / 2), Math.floor((bounds.h || 20) / 2)];
    } else {
      const [q, r] = this.selectedCell;

      switch (direction) {
        case "up":
          this.selectedCell = [q, r - 1];
          break;
        case "down":
          this.selectedCell = [q, r + 1];
          break;
        case "left":
          this.selectedCell = [q - 1, r];
          break;
        case "right":
          this.selectedCell = [q + 1, r];
          break;
      }
    }

    const [q, r] = this.selectedCell!;

    if (bounds.shape === "rect") {
      if (q < 0 || q >= (bounds.w || 0) || r < 0 || r >= (bounds.h || 0)) {
        this.selectedCell = null;
      }
    }

    this.selectedAgentId = null;
    this.updateUI();
  }

  private handleMapSelection(): void {
    if (!this.selectedCell || !this.state) return;

    const [q, r] = this.selectedCell;
    const tileKey = `${q},${r}`;

    const cellAgents = this.state.agents?.filter(
      (a) => a.pos && a.pos[0] === q && a.pos[1] === r
    );

    if (cellAgents && cellAgents.length > 0) {
      this.selectedAgentId = cellAgents[0].id;
      this.updateUI();
    }
  }

  start(): void {
    this.screen.render();
  }
}
