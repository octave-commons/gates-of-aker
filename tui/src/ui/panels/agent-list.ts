import blessed from "blessed";
import { Agent, WorldState } from "../../types";

export class AgentList {
  private element: blessed.Widgets.BoxElement;
  private state: WorldState | null = null;

  constructor(parent: blessed.Widgets.Screen) {
    this.element = blessed.box({
      parent,
      top: 0,
      left: "80%",
      width: "20%",
      height: "100%-1",
      border: { type: "line", fg: "white" as any },
      label: " Agents ",
      style: {
        bg: "black",
        fg: "white",
      },
      scrollable: true,
      alwaysScroll: true,
    });

    this.render();
  }

  setState(state: WorldState | null): void {
    this.state = state;
    this.render();
  }

  private render(): void {
    if (!this.state || !this.state.agents || this.state.agents.length === 0) {
      this.element.setContent("{grey-fg}No agents{/grey-fg}");
      return;
    }

    const lines: string[] = [];
    lines.push(`{bold}Total: ${this.state.agents.length}{/bold}`);
    lines.push("");

    this.state.agents?.forEach((agent: Agent) => {
      const status = (agent.status as any)?.["alive?"] ?? true;
      const statusColor = status ? "green" : "red";
      const posStr = agent.pos ? `[${String(agent.pos[0])},${String(agent.pos[1])}]` : "[-]";

      lines.push(
        `{${statusColor}-fg}#${agent.id}{/${statusColor}-fg} {cyan-fg}${agent.name?.substring(0, 10) || "Unknown"}{/cyan-fg}`
      );
      lines.push(`  {yellow-fg}${agent.role}{/yellow-fg} ${posStr}`);
      lines.push("");
    });

    this.element.setContent(lines.join("\n"));
  }

  getElement(): blessed.Widgets.BoxElement {
    return this.element;
  }
}
