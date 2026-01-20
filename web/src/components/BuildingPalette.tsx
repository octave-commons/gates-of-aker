import React, { useState } from "react";

export type BuildingType = 
  | "shrine"
  | "wall" 
  | "stockpile"
  | "warehouse"
  | "campfire"
  | "statue_dog"
  | "tree"
  | "wolf"
  | "bear";

interface BuildingConfig {
  id: BuildingType;
  name: string;
  description: string;
  icon: string; // emoji or simple icon representation
  color: string;
  requiresSelection: boolean;
  maxCount?: number;
  configurable?: boolean;
}

interface BuildingPaletteProps {
  onPlaceBuilding: (
    type: BuildingType,
    pos: [number, number],
    config?: any
  ) => void;
  selectedCell: [number, number] | null;
  disabled?: boolean;
}

const BUILDING_CONFIGS: BuildingConfig[] = [
  {
    id: "shrine",
    name: "Shrine",
    description: "Sacred site that channels divine energy. One per world.",
    icon: "⛩️",
    color: "#ffae00",
    requiresSelection: true,
    maxCount: 1,
  },
  {
    id: "wall",
    name: "Wall",
    description: "Defensive structure that requires wood and labor to complete.",
    icon: "🧱",
    color: "#666",
    requiresSelection: true,
  },
  {
    id: "stockpile",
    name: "Stockpile",
    description: "Storage area for resources (wood or food).",
    icon: "📦",
    color: "#8d6e63",
    requiresSelection: true,
    configurable: true,
  },
  {
    id: "warehouse",
    name: "Warehouse",
    description: "Large storage facility with built-in stockpile.",
    icon: "🏭",
    color: "#757575",
    requiresSelection: true,
  },
  {
    id: "campfire",
    name: "Campfire",
    description: "Community gathering place providing warmth and light.",
    icon: "🔥",
    color: "#ff6b00",
    requiresSelection: true,
  },
  {
    id: "statue_dog",
    name: "Dog Statue",
    description: "Guardian statue that protects the settlement.",
    icon: "🗿",
    color: "#9e9e9e",
    requiresSelection: true,
  },
  {
    id: "tree",
    name: "Tree",
    description: "Natural resource that provides wood and spawns fruit.",
    icon: "🌳",
    color: "#2e7d32",
    requiresSelection: true,
  },
  {
    id: "wolf",
    name: "Wolf",
    description: "Wild predator that hunts in the wilderness.",
    icon: "🐺",
    color: "#795548",
    requiresSelection: true,
  },
  {
    id: "bear",
    name: "Bear",
    description: "Powerful predator that commands respect.",
    icon: "🐻",
    color: "#5d4037",
    requiresSelection: true,
  },
];

function BuildingIcon({ config, size = 32 }: { config: BuildingConfig; size?: number }) {
  return (
    <div
      style={{
        width: size,
        height: size,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: size * 0.6,
        backgroundColor: config.color + "20",
        border: `2px solid ${config.color}`,
        borderRadius: 4,
        cursor: "pointer",
      }}
      title={config.name}
    >
      {config.icon}
    </div>
  );
}

function StockpileConfig({
  onConfirm,
  onCancel,
}: {
  onConfirm: (resource: string, capacity: number) => void;
  onCancel: () => void;
}) {
  const [resource, setResource] = useState("wood");
  const [capacity, setCapacity] = useState(100);

  return (
    <div
      style={{
        position: "fixed",
        top: "50%",
        left: "50%",
        transform: "translate(-50%, -50%)",
        backgroundColor: "white",
        border: "2px solid #ccc",
        borderRadius: 8,
        padding: 16,
        boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
        zIndex: 1000,
      }}
    >
      <h3 style={{ margin: "0 0 12px 0" }}>Configure Stockpile</h3>
      
      <div style={{ marginBottom: 12 }}>
        <label style={{ display: "block", marginBottom: 4 }}>
          Resource Type:
        </label>
        <select
          value={resource}
          onChange={(e) => setResource(e.target.value)}
          style={{ width: "100%", padding: 4 }}
        >
          <option value="wood">Wood</option>
          <option value="food">Food</option>
        </select>
      </div>
      
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: "block", marginBottom: 4 }}>
          Capacity (max 1000):
        </label>
        <input
          type="number"
          min={1}
          max={1000}
          value={capacity}
          onChange={(e) => {
            const val = parseInt(e.target.value, 10);
            if (!isNaN(val) && val > 0) setCapacity(val);
          }}
          style={{ width: "100%", padding: 4 }}
        />
      </div>
      
      <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
        <button
          onClick={onCancel}
          style={{
            padding: "6px 12px",
            backgroundColor: "#ccc",
            border: "none",
            borderRadius: 4,
            cursor: "pointer",
          }}
        >
          Cancel
        </button>
        <button
          onClick={() => onConfirm(resource, capacity)}
          style={{
            padding: "6px 12px",
            backgroundColor: "#4CAF50",
            color: "white",
            border: "none",
            borderRadius: 4,
            cursor: "pointer",
          }}
        >
          Place
        </button>
      </div>
    </div>
  );
}

export function BuildingPalette({
  onPlaceBuilding,
  selectedCell,
  disabled = false,
}: BuildingPaletteProps) {
  const [selectedBuilding, setSelectedBuilding] = useState<BuildingType | null>(null);
  const [showStockpileConfig, setShowStockpileConfig] = useState(false);

  const handleBuildingClick = (config: BuildingConfig) => {
    if (disabled) return;
    
    if (config.configurable) {
      setSelectedBuilding(config.id);
      setShowStockpileConfig(true);
    } else {
      setSelectedBuilding(config.id);
    }
  };

  const handleCellClick = () => {
    if (!selectedBuilding || !selectedCell || disabled) return;
    
    if (selectedBuilding === "stockpile") {
      setShowStockpileConfig(true);
    } else {
      onPlaceBuilding(selectedBuilding, selectedCell);
      setSelectedBuilding(null);
    }
  };

  const handleStockpileConfirm = (resource: string, capacity: number) => {
    if (selectedCell) {
      onPlaceBuilding("stockpile", selectedCell, { resource, capacity });
    }
    setShowStockpileConfig(false);
    setSelectedBuilding(null);
  };

  const handleStockpileCancel = () => {
    setShowStockpileConfig(false);
    setSelectedBuilding(null);
  };

  return (
    <>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 12,
          padding: 12,
          backgroundColor: "#f5f5f5",
          border: "1px solid #ccc",
          borderRadius: 8,
          minWidth: 280,
        }}
      >
        <div style={{ fontSize: 16, fontWeight: "bold", marginBottom: 4 }}>
          Building Palette
        </div>
        
        {!selectedCell && selectedBuilding && (
          <div style={{ 
            fontSize: 12, 
            color: "#d32f2f",
            backgroundColor: "#ffebee",
            padding: 6,
            borderRadius: 4,
            border: "1px solid #ffcdd2"
          }}>
            Please select a cell on the map first
          </div>
        )}
        
        {selectedCell && !selectedBuilding && (
          <div style={{ 
            fontSize: 12, 
            color: "#388e3c",
            backgroundColor: "#e8f5e8",
            padding: 6,
            borderRadius: 4,
            border: "1px solid #c8e6c9"
          }}>
            Selected: [{selectedCell[0]}, {selectedCell[1]}]
          </div>
        )}
        
        {selectedCell && selectedBuilding && (
          <div style={{ 
            fontSize: 12, 
            color: "#1976d2",
            backgroundColor: "#e3f2fd",
            padding: 6,
            borderRadius: 4,
            border: "1px solid #bbdefb"
          }}>
            Ready to place {BUILDING_CONFIGS.find(b => b.id === selectedBuilding)?.name}
          </div>
        )}

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(3, 1fr)",
            gap: 8,
          }}
        >
          {BUILDING_CONFIGS.map((config) => (
            <div
              key={config.id}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 4,
                cursor: disabled ? "not-allowed" : "pointer",
                opacity: disabled ? 0.5 : 1,
              }}
              onClick={() => handleBuildingClick(config)}
              title={`${config.name}: ${config.description}`}
            >
              <BuildingIcon config={config} size={40} />
              <div style={{ fontSize: 10, textAlign: "center" }}>
                {config.name}
              </div>
            </div>
          ))}
        </div>

        {selectedBuilding && selectedCell && (
          <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
            <button
              onClick={() => setSelectedBuilding(null)}
              style={{
                flex: 1,
                padding: "6px 12px",
                backgroundColor: "#ccc",
                border: "none",
                borderRadius: 4,
                cursor: "pointer",
              }}
            >
              Cancel
            </button>
            <button
              onClick={handleCellClick}
              disabled={!selectedCell}
              style={{
                flex: 2,
                padding: "6px 12px",
                backgroundColor: selectedCell ? "#4CAF50" : "#ccc",
                color: "white",
                border: "none",
                borderRadius: 4,
                cursor: selectedCell ? "pointer" : "not-allowed",
              }}
            >
              Place {BUILDING_CONFIGS.find(b => b.id === selectedBuilding)?.name}
            </button>
          </div>
        )}
      </div>

      {showStockpileConfig && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0,0,0,0.3)",
            zIndex: 999,
          }}
          onClick={handleStockpileCancel}
        >
          <div onClick={(e) => e.stopPropagation()}>
            <StockpileConfig
              onConfirm={handleStockpileConfirm}
              onCancel={handleStockpileCancel}
            />
          </div>
        </div>
      )}
    </>
  );
}