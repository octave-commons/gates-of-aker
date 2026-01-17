import { EventCard } from "./EventCard";

type EventFeedProps = {
  events: any[];
  title?: string;
  compact?: boolean;
};

export function EventFeed({ events, title = "Events", compact = false }: EventFeedProps) {
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