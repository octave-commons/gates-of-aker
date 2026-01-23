import blessed from "blessed";
import { WorldState } from "../../types";
export declare class AgentList {
    private element;
    private state;
    constructor(parent: blessed.Widgets.Screen);
    setState(state: WorldState | null): void;
    private render;
    getElement(): blessed.Widgets.BoxElement;
}
//# sourceMappingURL=agent-list.d.ts.map