import blessed from "blessed";
import { AxialCoords, WorldState } from "../../types";
export declare class InfoPanel {
    private element;
    private state;
    private selectedCell;
    private selectedAgentId;
    constructor(parent: blessed.Widgets.Screen);
    setState(state: WorldState | null): void;
    setSelectedCell(cell: AxialCoords | null): void;
    setSelectedAgentId(agentId: number | null): void;
    private render;
    getElement(): blessed.Widgets.BoxElement;
}
//# sourceMappingURL=info-panel.d.ts.map