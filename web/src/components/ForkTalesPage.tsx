import React from "react";
import { ForkTalesPanel } from "./ForkTalesPanel";

type ForkTalesPageProps = {
  onBack: () => void;
};

export function ForkTalesPage({ onBack }: ForkTalesPageProps) {
  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        backgroundColor: "#1a1a2e",
        color: "#ffffff",
        overflow: "auto",
        padding: "2rem",
      }}
    >
      <div style={{ maxWidth: 960, margin: "0 auto", display: "grid", gap: 16 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
          <div>
            <h1 style={{ margin: 0, color: "#ffd700", letterSpacing: "0.08em" }}>Fork Tales</h1>
            <p style={{ margin: "8px 0 0 0", opacity: 0.8 }}>
              Preview or write the next chapter directly from Gates of Aker.
            </p>
          </div>
          <button
            type="button"
            onClick={onBack}
            style={{
              padding: "0.8rem 1.4rem",
              fontSize: "0.95rem",
              backgroundColor: "rgba(255,255,255,0.1)",
              color: "#ffffff",
              border: "1px solid rgba(255,255,255,0.2)",
              borderRadius: 8,
              cursor: "pointer",
              textTransform: "uppercase",
              letterSpacing: "0.08em",
            }}
          >
            Back to Menu
          </button>
        </div>

        <ForkTalesPanel />
      </div>
    </div>
  );
}
