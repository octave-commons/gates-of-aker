import React from "react";

interface ResourceTotalsPanelProps {
  totals: Record<string, number>;
}

const formatResource = (resource: string) => {
  if (!resource) return "Unknown";
  return resource.charAt(0).toUpperCase() + resource.slice(1);
};

export function ResourceTotalsPanel({ totals }: ResourceTotalsPanelProps) {
  const entries = Object.entries(totals)
    .filter(([, qty]) => Number.isFinite(qty))
    .sort(([a], [b]) => a.localeCompare(b));

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      <div style={{ fontSize: 14, fontWeight: 700, marginBottom: 8 }}>Stockpile Totals</div>
      {entries.length === 0 ? (
        <div style={{ fontSize: 12, opacity: 0.7 }}>No stockpile resources yet.</div>
      ) : (
        <div style={{ display: "grid", gap: 6, fontSize: 12 }}>
          {entries.map(([resource, qty]) => (
            <div key={resource} style={{ display: "flex", justifyContent: "space-between" }}>
              <span>{formatResource(resource)}</span>
              <span>{qty}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
