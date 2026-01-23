import blessed from "blessed";
export class InfoPanel {
    element;
    state = null;
    selectedCell = null;
    selectedAgentId = null;
    constructor(parent) {
        this.element = blessed.box({
            parent,
            top: 0,
            left: "60%",
            width: "20%",
            height: "100%-1",
            border: { type: "line", fg: "white" },
            label: " Selection ",
            style: {
                bg: "black",
                fg: "white",
            },
            scrollable: true,
            alwaysScroll: true,
        });
        this.render();
    }
    setState(state) {
        this.state = state;
        this.render();
    }
    setSelectedCell(cell) {
        this.selectedCell = cell;
        this.render();
    }
    setSelectedAgentId(agentId) {
        this.selectedAgentId = agentId;
        this.render();
    }
    render() {
        if (!this.state) {
            this.element.setContent("{grey-fg}No data{/grey-fg}");
            return;
        }
        const lines = [];
        if (this.selectedAgentId) {
            const agent = this.state.agents?.find((a) => a.id === this.selectedAgentId);
            if (agent) {
                lines.push(`{bold}Agent #${agent.id}{/bold}`);
                lines.push(`Name: {cyan-fg}${agent.name || "Unknown"}{/cyan-fg}`);
                lines.push(`Role: {yellow-fg}${agent.role}{/yellow-fg}`);
                lines.push(`Faction: ${agent.faction || "None"}`);
                if (agent.pos) {
                    lines.push(`Pos: [${agent.pos[0]}, ${agent.pos[1]}]`);
                }
                lines.push("");
                lines.push("{bold}Needs:{/bold}");
                if (agent.needs) {
                    Object.entries(agent.needs).forEach(([need, value]) => {
                        const percentage = (value * 100).toFixed(0);
                        lines.push(`  ${need}: ${percentage}%`);
                    });
                }
                if (agent.stats) {
                    lines.push("");
                    lines.push("{bold}Stats:{/bold}");
                    Object.entries(agent.stats).forEach(([stat, value]) => {
                        lines.push(`  ${stat}: ${value}`);
                    });
                }
            }
            else {
                lines.push("{red-fg}Agent not found{/red-fg}");
            }
        }
        else if (this.selectedCell) {
            const [q, r] = this.selectedCell;
            const tileKey = `${q},${r}`;
            const tile = this.state.tiles?.[tileKey];
            lines.push(`{bold}Cell [${String(q)}, ${String(r)}]{/bold}`);
            lines.push("");
            if (tile) {
                if (tile.terrain) {
                    lines.push(`Terrain: {green-fg}${tile.terrain}{/green-fg}`);
                }
                if (tile.structure) {
                    lines.push(`Structure: {yellow-fg}${tile.structure}{/yellow-fg}`);
                }
                const cellAgents = this.state.agents?.filter((a) => a.pos && a.pos[0] === q && a.pos[1] === r);
                if (cellAgents && cellAgents.length > 0) {
                    lines.push("");
                    lines.push(`{bold}Agents (${cellAgents.length}):{/bold}`);
                    cellAgents.forEach((agent) => {
                        lines.push(`  #${agent.id}: ${agent.name || "Unknown"} (${agent.role})`);
                    });
                }
            }
            else {
                lines.push("{grey-fg}Empty cell{/grey-fg}");
            }
        }
        else {
            lines.push("{grey-fg}No selection{/grey-fg}");
            lines.push("");
            lines.push("Click a cell or agent to select");
        }
        this.element.setContent(lines.join("\n"));
    }
    getElement() {
        return this.element;
    }
}
//# sourceMappingURL=info-panel.js.map