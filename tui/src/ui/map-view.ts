import blessed from "blessed";
import { WorldState, Agent, AxialCoords } from "../types";

export class MapView {
  private element: blessed.Widgets.BoxElement;
  private state: WorldState | null = null;
  private selectedCell: AxialCoords | null = null;
  private offsetX = 0;
  private offsetY = 0;

  constructor(parent: blessed.Widgets.Screen) {
    this.element = blessed.box({
      parent,
      top: 0,
      left: 0,
      width: "60%",
      height: "100%-1",
      border: { type: "line", fg: "white" as any },
      label: " World Map ",
      style: {
        bg: "black",
        fg: "white",
      },
      scrollable: true,
      alwaysScroll: true,
    });
  }

  setState(state: WorldState | null): void {
    this.state = state;
    this.render();
  }

  setSelectedCell(cell: AxialCoords | null): void {
    this.selectedCell = cell;
    this.render();
  }

  private render(): void {
    if (!this.state) {
      this.element.setContent("{center}{grey-fg}No world state{/grey-fg}{/center}");
      return;
    }

    const lines = this.renderMap();
    const content = lines.join("\n");
    this.element.setContent(content);
  }

  private renderMap(): string[] {
    const lines: string[] = [];
    const tiles = this.state?.tiles ?? {};
    const agents = this.state?.agents ?? [];

    const tilesWithAgents = new Map<string, Agent[]>();
    agents.forEach((agent: Agent) => {
      if (agent.pos && Array.isArray(agent.pos) && agent.pos.length === 2) {
        const key = `${agent.pos[0]},${agent.pos[1]}`;
        if (!tilesWithAgents.has(key)) {
          tilesWithAgents.set(key, []);
        }
        tilesWithAgents.get(key)!.push(agent);
      }
    });

    const tileKeys = Object.keys(tiles).sort();
    const bounds = (this.state as any)?.map?.bounds;

    if (bounds?.shape === "rect" && bounds.w && bounds.h) {
      for (let r = 0; r < bounds.h; r++) {
        let line = "";
        for (let q = 0; q < bounds.w; q++) {
          const key = `${q},${r}`;
          const tile = tiles[key];
          const cellAgents = tilesWithAgents.get(key);

          line += this.renderCell(q, r, tile, cellAgents);
        }
        lines.push(line);
      }
    } else {
      const coords = tileKeys.map((key) => {
        const [q, r] = key.split(",").map(Number);
        return { q, r };
      });

      const minR = Math.min(...coords.map((c) => c.r));
      const maxR = Math.max(...coords.map((c) => c.r));

      for (let r = minR; r <= maxR; r++) {
        let line = "";
        const rowCoords = coords.filter((c) => c.r === r).sort((a, b) => a.q - b.q);

        let prevQ = rowCoords[0]?.q ?? 0;
        for (let i = 0; i < rowCoords.length; i++) {
          const { q } = rowCoords[i];
          const key = `${q},${r}`;
          const tile = tiles[key];
          const cellAgents = tilesWithAgents.get(key);

          while (prevQ < q - 1) {
            line += " ";
            prevQ++;
          }

          line += this.renderCell(q, r, tile, cellAgents);
          prevQ = q;
        }
        lines.push(line);
      }
    }

    return lines;
  }

  private renderCell(
    q: number,
    r: number,
    tile: any,
    agents: Agent[] | undefined
  ): string {
    const isSelected =
      this.selectedCell && this.selectedCell[0] === q && this.selectedCell[1] === r;

    let char = ".";
    let fg = "grey";
    let bg = isSelected ? "blue" : "black";

    if (tile?.structure) {
      switch (tile.structure) {
        case "campfire":
        case "shrine":
          char = "▲";
          fg = "yellow";
          break;
        case "wall":
          char = "#";
          fg = "white";
          break;
        case "house":
          char = "H";
          fg = "green";
          break;
        default:
          char = "?";
          fg = "cyan";
      }
    } else if (tile?.terrain) {
      switch (tile.terrain) {
        case "grass":
          char = "\"";
          fg = "green";
          break;
        case "dirt":
          char = ",";
          fg = "yellow";
          break;
        case "water":
          char = "~";
          fg = "blue";
          break;
        default:
          char = ".";
          fg = "grey";
      }
    }

    if (agents && agents.length > 0) {
      char = "A";
      fg = "red";
      if (agents.length > 1) {
        char = String(agents.length);
      }
    }

    return `{${fg}-fg}{${bg}-bg}${char}{/${bg}-bg}{/${fg}-fg}`;
  }

  getElement(): blessed.Widgets.BoxElement {
    return this.element;
  }
}
