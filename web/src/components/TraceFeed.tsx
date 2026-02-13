import { memo } from "react";
import { Trace } from "../types";
import { fmt } from "../utils";

type TracePacket = {
  intent?: string;
  facets?: string[] | string;
  "claim-hint"?: string;
};

type TraceSpreadEntry = {
  from?: string | number;
  to?: string | number;
  w?: number | string;
  delta?: number | string;
};

type TraceEventRecall = {
  "event-type"?: string;
  delta?: number | string;
  new?: number | string;
};

type TraceMention = {
  "event-type"?: string;
  claim?: string;
  weight?: number | string;
};

type DisplayTrace = Trace & {
  "trace/id"?: string | number;
  tick?: number;
  speaker?: string;
  listener?: string;
  packet?: TracePacket;
  spread?: TraceSpreadEntry[];
  "event-recall"?: TraceEventRecall;
  mention?: TraceMention;
};

type TraceFeedProps = {
  traces: Trace[];
};

const TraceCard = memo(function TraceCard({ trace, idx }: { trace: DisplayTrace; idx: number }) {
  if (!trace) return null;

  try {
    return (
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
            {(trace.spread ?? []).slice(0, 12).map((entry: TraceSpreadEntry, i: number) => (
              <div key={`${String(entry.from)}-${String(entry.to)}-${String(entry.delta)}-${i}`} style={{ fontFamily: "monospace", fontSize: 12 }}>
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
    );
  } catch (error) {
    console.error("Error rendering trace card:", error, trace);
    return (
      <div style={{ border: "1px solid #f44336", borderRadius: 10, padding: 10, color: "#d32f2f" }}>
        <strong>Error rendering trace {trace["trace/id"] ?? idx}</strong>
      </div>
    );
  }
});

export function TraceFeed({ traces }: TraceFeedProps) {
  if (!traces || traces.length === 0) {
    return (
      <div>
        <div style={{ marginBottom: 8 }}>
          <strong>Traces</strong> <span style={{ opacity: 0.7 }}>(0)</span>
        </div>
        <div style={{ opacity: 0.6, fontSize: 12 }}>No traces yet</div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: 8 }}>
        <strong>Traces</strong> <span style={{ opacity: 0.7 }}>({traces.length})</span>
      </div>
      <div style={{ display: "grid", gap: 10 }}>
        {[...traces].reverse().map((trace: Trace, idx) => (
          <TraceCard key={(trace as DisplayTrace)["trace/id"] ?? idx} trace={trace as DisplayTrace} idx={idx} />
        ))}
      </div>
    </div>
  );
}
