import React, { useState } from "react";
import { EventCard } from "./EventCard";

type EventFeedProps = {
  events: any[];
  title?: string;
  compact?: boolean;
  collapsible?: boolean;
};

export function EventFeed({ 
  events, 
  title = "Events", 
  compact = false, 
  collapsible = false 
}: EventFeedProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);

  if (collapsible) {
    return (
      <div style={{ marginTop: 12 }}>
        <div 
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            cursor: "pointer",
            padding: "8px 0",
            borderBottom: isCollapsed ? "1px solid #ddd" : "none"
          }}
          onClick={() => setIsCollapsed(!isCollapsed)}
        >
          <strong style={{ margin: 0 }}>{title} ({events.length})</strong>
          <span style={{ 
            fontSize: "1.2em", 
            color: "#666",
            transition: "transform 0.2s ease",
            transform: isCollapsed ? "rotate(-90deg)" : "rotate(0deg)"
          }}>
            ▼
          </span>
        </div>
        
        {!isCollapsed && (
          <div style={{ 
            maxHeight: compact ? 200 : 300, 
            overflowY: "auto", 
            border: "1px solid #ccc", 
            borderRadius: 8, 
            padding: 8,
            marginTop: 8
          }}>
            {events.length === 0 ? (
              <div style={{ opacity: 0.6, fontSize: 13, textAlign: "center", padding: 20 }}>
                No events recorded
              </div>
            ) : (
              [...events].reverse().map((event, idx) => (
                <EventCard key={event.id ?? idx} event={event} compact={compact} />
              ))
            )}
          </div>
        )}
      </div>
    );
  }

  // Original non-collapsible behavior
  return (
    <div style={{ marginTop: 12 }}>
      <strong>{title} ({events.length})</strong>
      <div style={{ 
        maxHeight: compact ? 200 : 300, 
        overflowY: "auto", 
        border: "1px solid #ccc", 
        borderRadius: 8, 
        padding: 8 
      }}>
        {events.length === 0 ? (
          <div style={{ opacity: 0.6, fontSize: 13, textAlign: "center", padding: 20 }}>
            No events recorded
          </div>
        ) : (
          [...events].reverse().map((event, idx) => (
            <EventCard key={event.id ?? idx} event={event} compact={compact} />
          ))
        )}
      </div>
    </div>
  );
}