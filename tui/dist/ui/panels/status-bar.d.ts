import blessed from "blessed";
import { ConnectionState } from "../../types";
export declare class StatusBar {
    private element;
    private status;
    private tick;
    private isRunning;
    constructor(parent: blessed.Widgets.Screen);
    setStatus(status: ConnectionState): void;
    setTick(tick: number): void;
    setRunning(running: boolean): void;
    private render;
    private getStatusText;
    getElement(): blessed.Widgets.BoxElement;
}
//# sourceMappingURL=status-bar.d.ts.map