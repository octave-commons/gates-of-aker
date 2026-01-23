import blessed from "blessed";
import { WorldState, AxialCoords } from "../types";
export declare class MapView {
    private element;
    private state;
    private selectedCell;
    private offsetX;
    private offsetY;
    constructor(parent: blessed.Widgets.Screen);
    setState(state: WorldState | null): void;
    setSelectedCell(cell: AxialCoords | null): void;
    private render;
    private renderMap;
    private renderCell;
    getElement(): blessed.Widgets.BoxElement;
}
//# sourceMappingURL=map-view.d.ts.map