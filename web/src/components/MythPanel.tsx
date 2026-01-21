import React, { memo } from "react";

type DeityData = {
  faith: number;
};

type MythPanelProps = {
  deities: Record<string, DeityData>;
  globalFavor: number;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
};

const DeityName = (deityId: string): string => {
  return deityId
    .replace(/^:/, "")
    .replace(/\//g, ": ")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
};

const BarColor = (value: number): string => {
  if (value < 0.3) return "#e57373";
  if (value < 0.7) return "#fff176";
  return "#81c784";
};

const ProgressBar = ({ value, label }: { value: number; label: string }) => {
  const color = BarColor(value);
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 11 }}>
      <span style={{ color: "#b0bec5", width: 40 }}>{label}:</span>
      <div
        style={{
          flex: 1,
          height: 8,
          backgroundColor: "#37474f",
          borderRadius: 2,
          overflow: "hidden"
        }}
      >
        <div
          style={{
            width: `${Math.min(value * 100, 100)}%`,
            height: "100%",
            backgroundColor: color,
            transition: "width 0.3s ease, background-color 0.3s ease"
          }}
        />
      </div>
      <span style={{ color: "#cfd8dc", width: 35, textAlign: "right" }}>
        {value.toFixed(2)}
      </span>
    </div>
  );
};

export const MythPanel = memo(function MythPanel({
  deities,
  globalFavor,
  collapsed = false,
  onToggleCollapse
}: MythPanelProps) {
  const deityList = Object.entries(deities).sort(([a], [b]) => a.localeCompare(b));

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        gap: 8,
        padding: 8,
        backgroundColor: "#1e1e1e",
        borderRadius: 4,
        border: "1px solid #424242"
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          borderBottom: collapsed ? "1px solid #424242" : "none",
          paddingBottom: 8,
          cursor: "pointer"
        }}
        onClick={() => onToggleCollapse?.()}
      >
        <strong style={{ color: "#e0e0e0", fontSize: 14 }}>
          Pantheon ({deityList.length})
        </strong>
        <span
          style={{
            fontSize: "1.2em",
            color: "#666",
            transition: "transform 0.2s ease",
            transform: collapsed ? "rotate(-90deg)" : "rotate(0deg)"
          }}
        >
          ▼
        </span>
      </div>

      {!collapsed && (
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <div
            style={{
              padding: 8,
              backgroundColor: "#263238",
              borderRadius: 3,
              border: "1px solid #455a64"
            }}
          >
            <div
              style={{
                fontWeight: "bold",
                fontSize: 12,
                color: "#cfd8dc",
                marginBottom: 8
              }}
            >
              Global Favor
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              <ProgressBar value={globalFavor} label="Favor" />
            </div>
          </div>

          {deityList.length === 0 && (
            <div
              style={{
                color: "#757575",
                fontStyle: "italic",
                fontSize: 12,
                padding: 4,
                textAlign: "center"
              }}
            >
              No deities known yet. Build shrines and host rituals to discover the pantheon.
            </div>
          )}

          {deityList.map(([deityId, data]) => (
            <div
              key={deityId}
              style={{
                padding: 8,
                backgroundColor: "#263238",
                borderRadius: 3,
                border: "1px solid #455a64"
              }}
            >
              <div
                style={{
                  fontWeight: "bold",
                  fontSize: 12,
                  color: "#eceff1",
                  marginBottom: 8
                }}
              >
                {DeityName(deityId)}
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                <ProgressBar value={data.faith} label="Faith" />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
});
