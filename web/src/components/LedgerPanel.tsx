import React, { useState, useMemo } from "react";
import type { CSSProperties } from "react";

type LedgerPanelProps = {
  data: any;
  style?: CSSProperties;
};

type LedgerEntry = {
  buzz: number;
  tradition: number;
  mentions: number;
  event_instances?: any[];
  eventInstances?: any[];
};

function formatNumber(num: number | null | undefined): string {
  if (num === null || num === undefined) return "0";
  return typeof num === "number" ? num.toFixed(3) : "0";
}

function getTraditionLevel(tradition: number): { level: string; color: string } {
  if (tradition >= 10) return { level: "Strong", color: "#d946ef" };
  if (tradition >= 5) return { level: "Growing", color: "#3b82f6" };
  if (tradition >= 1) return { level: "Emerging", color: "#10b981" };
  if (tradition >= 0.1) return { level: "Weak", color: "#f59e0b" };
  return { level: "Fading", color: "#6b7280" };
}

export function LedgerPanel({ data, style = {} }: LedgerPanelProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [sortBy, setSortBy] = useState<"buzz" | "tradition" | "mentions">("tradition");
  const [filterEventType, setFilterEventType] = useState<string>("all");
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

  const ledgerData = useMemo(() => {
    if (!data || typeof data !== "object") return [];

    return Object.entries(data).map(([key, value]) => {
      const [eventType, claim] = key.startsWith("[") 
        ? JSON.parse(key.replace(/'/g, '"')) 
        : [key.split(",")[0], key.split(",")[1] || ""];
      
      const entry = value as LedgerEntry;
      return {
        key,
        eventType: Array.isArray(eventType) ? eventType[0] : eventType,
        claim: Array.isArray(claim) ? claim[0] : claim,
        buzz: entry.buzz || 0,
        tradition: entry.tradition || 0,
        mentions: entry.mentions || 0,
        eventInstances: entry.event_instances || [],
      };
    });
  }, [data]);

  const eventTypes = useMemo(() => {
    const types = new Set(ledgerData.map(entry => entry.eventType));
    return Array.from(types).sort();
  }, [ledgerData]);

  const filteredAndSortedData = useMemo(() => {
    let filtered = ledgerData;
    
    if (filterEventType !== "all") {
      filtered = filtered.filter(entry => entry.eventType === filterEventType);
    }

    return [...filtered].sort((a, b) => {
      switch (sortBy) {
        case "buzz":
          return b.buzz - a.buzz;
        case "tradition":
          return b.tradition - a.tradition;
        case "mentions":
          return b.mentions - a.mentions;
        default:
          return 0;
      }
    });
  }, [ledgerData, filterEventType, sortBy]);

  const toggleExpanded = (key: string) => {
    setExpandedItems(prev => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const stats = useMemo(() => {
    if (ledgerData.length === 0) return { totalEntries: 0, totalMentions: 0, avgTradition: 0 };
    
    const totalMentions = ledgerData.reduce((sum, entry) => sum + entry.mentions, 0);
    const avgTradition = ledgerData.reduce((sum, entry) => sum + entry.tradition, 0) / ledgerData.length;
    
    return {
      totalEntries: ledgerData.length,
      totalMentions,
      avgTradition,
    };
  }, [ledgerData]);

  if (!data || typeof data !== "object") {
    return (
      <div className="ledger-panel" style={style}>
        <div 
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            cursor: "pointer",
            padding: "8px 0",
          }}
          onClick={() => setIsCollapsed(!isCollapsed)}
        >
          <h2 style={{ margin: 0 }}>Ledger</h2>
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
            color: "#999", 
            fontStyle: "italic", 
            padding: 12,
            textAlign: "center" 
          }}>
            No ledger data available
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="ledger-panel" style={style}>
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
        <h2 style={{ margin: 0 }}>Ledger</h2>
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
        <>
          {/* Stats Summary */}
          <div style={{ 
            display: "flex", 
            gap: 16, 
            fontSize: "0.9em", 
            color: "#666",
            padding: 8,
            backgroundColor: "#f9f9f9",
            borderRadius: 4,
            marginBottom: 12
          }}>
            <span><strong>Entries:</strong> {stats.totalEntries}</span>
            <span><strong>Total Mentions:</strong> {stats.totalMentions}</span>
            <span><strong>Avg Tradition:</strong> {formatNumber(stats.avgTradition)}</span>
          </div>

          {/* Controls */}
          <div style={{ 
            display: "flex", 
            gap: 12, 
            marginBottom: 12,
            alignItems: "center"
          }}>
            <div>
              <label style={{ fontSize: "0.9em", marginRight: 6 }}>Event Type:</label>
              <select
                value={filterEventType}
                onChange={(e) => setFilterEventType(e.target.value)}
                style={{
                  padding: "4px 8px",
                  border: "1px solid #ddd",
                  borderRadius: 4,
                  fontSize: "0.9em"
                }}
              >
                <option value="all">All Types</option>
                {eventTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>
            
            <div>
              <label style={{ fontSize: "0.9em", marginRight: 6 }}>Sort by:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                style={{
                  padding: "4px 8px",
                  border: "1px solid #ddd",
                  borderRadius: 4,
                  fontSize: "0.9em"
                }}
              >
                <option value="tradition">Tradition</option>
                <option value="buzz">Buzz</option>
                <option value="mentions">Mentions</option>
              </select>
            </div>
          </div>

          {/* Ledger Entries */}
          <div style={{
            border: "1px solid #ddd",
            borderRadius: 6,
            backgroundColor: "#fff",
            maxHeight: "400px",
            overflow: "auto"
          }}>
            {filteredAndSortedData.length === 0 ? (
              <div style={{ 
                color: "#999", 
                fontStyle: "italic", 
                padding: 12,
                textAlign: "center" 
              }}>
                No entries match current filters
              </div>
            ) : (
              <div>
                {filteredAndSortedData.map((entry) => {
                  const traditionInfo = getTraditionLevel(entry.tradition);
                  const isExpanded = expandedItems.has(entry.key);
                  
                  return (
                    <div key={entry.key} style={{
                      borderBottom: "1px solid #eee",
                      padding: "12px"
                    }}>
                      {/* Main Entry */}
                      <div
                        style={{
                          cursor: "pointer",
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center"
                        }}
                        onClick={() => toggleExpanded(entry.key)}
                      >
                        <div style={{ flex: 1 }}>
                          <div style={{ 
                            fontWeight: "bold", 
                            color: "#333",
                            marginBottom: 4
                          }}>
                            {entry.claim}
                          </div>
                          <div style={{ 
                            fontSize: "0.85em", 
                            color: "#666",
                            marginBottom: 4
                          }}>
                            Type: <span style={{ 
                              backgroundColor: "#e5e7eb", 
                              padding: "2px 6px",
                              borderRadius: 3,
                              fontFamily: "monospace"
                            }}>
                              {entry.eventType}
                            </span>
                          </div>
                        </div>
                        
                        <div style={{ 
                          display: "flex", 
                          gap: 16,
                          fontSize: "0.9em"
                        }}>
                          <div style={{ textAlign: "right" }}>
                            <div style={{ color: "#666", fontSize: "0.8em" }}>Buzz</div>
                            <div style={{ fontWeight: "bold", color: "#059669" }}>
                              {formatNumber(entry.buzz)}
                            </div>
                          </div>
                          <div style={{ textAlign: "right" }}>
                            <div style={{ color: "#666", fontSize: "0.8em" }}>Tradition</div>
                            <div style={{ 
                              fontWeight: "bold", 
                              color: traditionInfo.color
                            }}>
                              {formatNumber(entry.tradition)}
                            </div>
                          </div>
                          <div style={{ textAlign: "right" }}>
                            <div style={{ color: "#666", fontSize: "0.8em" }}>Mentions</div>
                            <div style={{ fontWeight: "bold", color: "#1e40af" }}>
                              {entry.mentions}
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Tradition Level Badge */}
                      <div style={{ marginTop: 8 }}>
                        <span style={{
                          backgroundColor: traditionInfo.color,
                          color: "white",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: "0.75em",
                          fontWeight: "bold"
                        }}>
                          {traditionInfo.level}
                        </span>
                      </div>

                      {/* Expanded Details */}
                      {isExpanded && (
                        <div style={{
                          marginTop: 12,
                          padding: "8px 12px",
                          backgroundColor: "#f8fafc",
                          borderRadius: 4,
                          fontSize: "0.9em"
                        }}>
                          <div style={{ marginBottom: 8 }}>
                            <strong>Raw Key:</strong> 
                            <code style={{ 
                              marginLeft: 8, 
                              fontFamily: "monospace", 
                              fontSize: "0.9em",
                              color: "#666" 
                            }}>
                              {entry.key}
                            </code>
                          </div>
                          
                          {entry.eventInstances && entry.eventInstances.length > 0 && (
                            <div>
                              <strong>Event Instances:</strong>
                              <div style={{ marginLeft: 12, marginTop: 4 }}>
                                {entry.eventInstances.map((instance: any, idx: number) => (
                                  <div key={idx} style={{ 
                                    fontFamily: "monospace", 
                                    fontSize: "0.85em",
                                    color: "#666" 
                                  }}>
                                    {String(instance)}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                          
                          <div style={{ marginTop: 8, display: "flex", gap: 16, fontSize: "0.85em" }}>
                            <div><strong>Buzz Decay:</strong> {formatNumber(entry.buzz * 0.90)}</div>
                            <div><strong>Tradition Decay:</strong> {formatNumber(entry.tradition * 0.995)}</div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}