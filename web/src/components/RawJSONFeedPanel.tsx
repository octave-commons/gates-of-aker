import React from "react";
import type { CSSProperties } from "react";

type RawJSONFeedPanelProps = {
  title: string;
  data: unknown;
  style?: CSSProperties;
};

export function RawJSONFeedPanel({ title, data, style = {} }: RawJSONFeedPanelProps) {
  return (
    <div className="raw-json-feed-panel" style={style}>
      <h2>{title}</h2>
      <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>
  );
}
