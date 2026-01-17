type EventCardProps = {
  event: any;
  compact?: boolean;
};

export function EventCard({ event, compact = false }: EventCardProps) {
  if (!event) return null;

  const { id, type, tick, pos, impact, witnessScore, witnesses } = event;

  return (
    <div style={{
      backgroundColor: "#fff",
      border: "1px solid #ccc",
      borderRadius: 6,
      padding: 8,
      marginBottom: compact ? 4 : 8,
      fontSize: 13
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
        <div style={{ fontWeight: "bold", color: "#333" }}>
          {String(type ?? "unknown")}
        </div>
        <div style={{
          backgroundColor: "#e8e8e8",
          padding: "2px 6px",
          borderRadius: 4,
          fontSize: 11,
          fontFamily: "monospace"
        }}>
          tick {tick ?? "?"}
        </div>
      </div>
      
      {pos && (
        <div style={{ fontSize: 12, color: "#555", marginBottom: 4 }}>
          position: ({pos[0]}, {pos[1]})
        </div>
      )}
      
      <div style={{ display: "flex", gap: 12, fontSize: 12, color: "#555" }}>
        {impact !== undefined && (
          <div>impact: {typeof impact === "number" ? impact.toFixed(3) : String(impact)}</div>
        )}
        {witnessScore !== undefined && (
          <div>witness: {typeof witnessScore === "number" ? witnessScore.toFixed(3) : String(witnessScore)}</div>
        )}
      </div>
      
      {witnesses && Array.isArray(witnesses) && witnesses.length > 0 && (
        <div style={{
          marginTop: 6,
          display: "flex",
          flexWrap: "wrap",
          gap: 4
        }}>
          <span style={{ fontSize: 11, color: "#888" }}>witnesses:</span>
          {witnesses.slice(0, compact ? 3 : 6).map((witness: any, idx: number) => (
            <span key={idx} style={{
              backgroundColor: "#f0f0f0",
              padding: "2px 6px",
              borderRadius: 3,
              fontSize: 11,
              color: "#444"
            }}>
              {String(witness)}
            </span>
          ))}
          {witnesses.length > (compact ? 3 : 6) && (
            <span style={{ fontSize: 11, color: "#888" }}>+{witnesses.length - (compact ? 3 : 6)} more</span>
          )}
        </div>
      )}
      
      {id && (
        <div style={{ fontSize: 11, color: "#888", marginTop: 4 }}>
          id: {String(id)}
        </div>
      )}
    </div>
  );
}