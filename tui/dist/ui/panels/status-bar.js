import blessed from "blessed";
export class StatusBar {
    element;
    status = "closed";
    tick = 0;
    isRunning = false;
    constructor(parent) {
        this.element = blessed.box({
            parent,
            bottom: 0,
            left: 0,
            right: 0,
            height: 1,
            tags: true,
            style: {
                bg: "blue",
                fg: "white",
            },
        });
        this.render();
    }
    setStatus(status) {
        this.status = status;
        this.render();
    }
    setTick(tick) {
        this.tick = tick;
        this.render();
    }
    setRunning(running) {
        this.isRunning = running;
        this.render();
    }
    render() {
        const statusText = this.getStatusText();
        const runningText = this.isRunning ? "{green-fg}RUNNING{/green-fg}" : "{yellow-fg}PAUSED{/yellow-fg}";
        const content = ` ${statusText} | Tick: ${this.tick} | ${runningText} | Q: Quit | T: Tick | R: Run/Stop`;
        this.element.setContent(content);
    }
    getStatusText() {
        switch (this.status) {
            case "open":
                return "{green-fg}CONNECTED{/green-fg}";
            case "error":
                return "{red-fg}ERROR{/red-fg}";
            case "closed":
            default:
                return "{yellow-fg}DISCONNECTED{/yellow-fg}";
        }
    }
    getElement() {
        return this.element;
    }
}
//# sourceMappingURL=status-bar.js.map