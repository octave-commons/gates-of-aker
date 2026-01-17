import { Trace } from "../types";

type TraceFeedProps = {
  traces: Trace[];
};

const fmt = (n: any) => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

export function TraceFeed({ traces }: TraceFeedProps) {
  return (
    <div>
      <div style={{ marginBottom: 8 }}>
        <strong>Traces</strong> <span style={{ opacity: 0.7 }}>({traces.length})</span>
      </div>
      <div style={{ display: "grid", gap: 10 }}>
        {[...traces].reverse().map((trace: Trace, idx) => (
          <div
            key={trace["trace/id"] ?? idx}
            data-testid="trace-card"
            style={{ border: "1px solid #aaa", borderRadius: 10, padding: 10 }}
          >
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
              <div>
                <strong>{trace["trace/id"]}</strong>
              </div>
              <div style={{ opacity: 0.7 }}>tick {trace.tick}</div>
              <div style={{ opacity: 0.7 }}>
                {trace.speaker} → {trace.listener}
              </div>
            </div>
            <div style={{ marginTop: 8 }}>
              <strong>packet</strong> <span style={{ opacity: 0.7 }}>{trace.packet?.intent}</span>
              {trace.packet?.facets && (
                <span style={{ marginLeft: 6 }}>
                  facets: {Array.isArray(trace.packet.facets) ? trace.packet.facets.join(", ") : trace.packet.facets}
                </span>
              )}
              {trace.packet?.["claim-hint"] ? (
                <span style={{ marginLeft: 6, opacity: 0.7 }}>hint: {String(trace.packet?.["claim-hint"])} </span>
              ) : null}
            </div>
            <div style={{ marginTop: 8 }}>
              <strong>spread</strong>
              <div style={{ display: "grid", gap: 4, marginTop: 4 }}>
                {(trace.spread ?? []).slice(0, 12).map((entry: any, i: number) => (
                  <div key={i} style={{ fontFamily: "monospace", fontSize: 12 }}>
                    {String(entry.from)} → {String(entry.to)} w={fmt(entry.w)} Δ={fmt(entry.delta)}
                  </div>
                ))}
              </div>
            </div>
            <div style={{ marginTop: 8, fontFamily: "monospace" }}>
              <strong>event</strong> {trace["event-recall"]?.["event-type"]} Δ=
              {fmt(trace["event-recall"]?.delta)} new={fmt(trace["event-recall"]?.new)}
            </div>
            <div style={{ marginTop: 8, fontFamily: "monospace" }}>
              <strong>mention</strong> {trace.mention?.["event-type"]}/{trace.mention?.claim} w=
              {fmt(trace.mention?.weight)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
