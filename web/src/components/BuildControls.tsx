import { useState } from "react";

type BuildControlsProps = {
  onPlaceWallGhost: (pos: [number, number]) => void;
  onPlaceStockpile: (pos: [number, number], resource: string, maxQty?: number) => void;
  onToggleBuildMode: () => void;
  buildMode: boolean;
  selectedCell: [number, number] | null;
};

export function BuildControls({
  onPlaceWallGhost,
  onPlaceStockpile,
  onToggleBuildMode,
  buildMode,
  selectedCell,
}: BuildControlsProps) {
  const [stockpileResource, setStockpileResource] = useState("wood");
  const [stockpileMaxQty, setStockpileMaxQty] = useState(100);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 8 }}>
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
        <button
          onClick={() => {
            if (buildMode) onToggleBuildMode();
          }}
          style={{
            backgroundColor: !buildMode ? "#4CAF50" : "#ccc",
          }}
        >
          Select Mode
        </button>
        <button
          onClick={() => {
            if (!buildMode) onToggleBuildMode();
          }}
          style={{
            backgroundColor: buildMode ? "#4CAF50" : "#ccc",
          }}
        >
          Build Wall Mode
        </button>
      </div>
      
      <div style={{ 
        padding: 8, 
        border: "1px solid #ccc", 
        borderRadius: 4,
        backgroundColor: "#fafafa"
      }}>
        <div style={{ fontSize: 13, fontWeight: "bold", marginBottom: 6 }}>Stockpile</div>
        <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", fontSize: 12 }}>
          <label>
            Resource:
            <select 
              value={stockpileResource}
              onChange={(e) => setStockpileResource(e.target.value)}
              style={{ marginLeft: 4, padding: "2px 4px" }}
            >
              <option value="wood">Wood</option>
              <option value="food">Food</option>
            </select>
          </label>
          <label>
            Capacity:
            <input
              type="number"
              min={1}
              max={1000}
              value={stockpileMaxQty}
              onChange={(e) => {
                const val = parseInt(e.target.value, 10);
                if (!isNaN(val) && val > 0) setStockpileMaxQty(val);
              }}
              style={{ marginLeft: 4, width: 50, padding: "2px 4px" }}
            />
          </label>
          <button
            onClick={() => {
              if (selectedCell) {
                onPlaceStockpile(selectedCell, stockpileResource, stockpileMaxQty);
              }
            }}
            disabled={!selectedCell}
            style={{
              backgroundColor: selectedCell ? "#FF9800" : "#ccc",
              padding: "4px 8px"
            }}
          >
            Place Stockpile
          </button>
        </div>
      </div>
    </div>
  );
}
