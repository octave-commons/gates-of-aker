import blessed from "blessed";
export class AgentList {
    element;
    state = null;
    constructor(parent) {
        this.element = blessed.box({
            parent,
            top: 0,
            left: "80%",
            width: "20%",
            height: "100%-1",
            border: { type: "line", fg: "white" },
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
    setState(state) {
        this.state = state;
        this.render();
    }
    render() {
        if (!this.state || !this.state.agents || this.state.agents.length === 0) {
            this.element.setContent("{grey-fg}No agents{/grey-fg}");
            return;
        }
        const lines = [];
        lines.push(`{bold}Total: ${this.state.agents.length}{/bold}`);
        lines.push("");
        this.state.agents?.forEach((agent) => {
            const status = agent.status?.["alive?"] ?? true;
            const statusColor = status ? "green" : "red";
            const posStr = agent.pos ? `[${String(agent.pos[0])},${String(agent.pos[1])}]` : "[-]";
            lines.push(`{${statusColor}-fg}#${agent.id}{/${statusColor}-fg} {cyan-fg}${agent.name?.substring(0, 10) || "Unknown"}{/cyan-fg}`);
            lines.push(`  {yellow-fg}${agent.role}{/yellow-fg} ${posStr}`);
            lines.push("");
        });
        this.element.setContent(lines.join("\n"));
    }
    getElement() {
        return this.element;
    }
}
//# sourceMappingURL=agent-list.js.map