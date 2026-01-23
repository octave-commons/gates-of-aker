import blessed from "blessed";
import { ConnectionState } from "../../types";

export class StatusBar {
  private element: blessed.Widgets.BoxElement;
  private status: ConnectionState = "closed";
  private tick: number = 0;
  private isRunning: boolean = false;

  constructor(parent: blessed.Widgets.Screen) {
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

  setStatus(status: ConnectionState): void {
    this.status = status;
    this.render();
  }

  setTick(tick: number): void {
    this.tick = tick;
    this.render();
  }

  setRunning(running: boolean): void {
    this.isRunning = running;
    this.render();
  }

  private render(): void {
    const statusText = this.getStatusText();
    const runningText = this.isRunning ? "{green-fg}RUNNING{/green-fg}" : "{yellow-fg}PAUSED{/yellow-fg}";
    const content = ` ${statusText} | Tick: ${this.tick} | ${runningText} | Q: Quit | T: Tick | R: Run/Stop`;
    this.element.setContent(content);
  }

  private getStatusText(): string {
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

  getElement(): blessed.Widgets.BoxElement {
    return this.element;
  }
}
