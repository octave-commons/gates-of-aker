import { useState } from "react";
import type { CSSProperties } from "react";

type AttributionPanelProps = {
  data: any;
  style?: CSSProperties;
};

export function AttributionPanel({ data, style = {} }: AttributionPanelProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [expandedPaths, setExpandedPaths] = useState<Set<string>>(new Set(["root"]));
  const [searchTerm, setSearchTerm] = useState("");

  const toggleExpanded = (path: string) => {
    setExpandedPaths(prev => {
      const next = new Set(prev);
      if (next.has(path)) {
        next.delete(path);
      } else {
        next.add(path);
      }
      return next;
    });
  };

  const renderTree = (data: any, path: string = "root", level: number = 0) => {
    const isObject = data !== null && typeof data === "object";
    const isArray = Array.isArray(data);
    const hasChildren = isObject && Object.keys(data).length > 0;
    const isExpanded = expandedPaths.has(path);

    if (!hasChildren) {
      const formatValue = (value: any): string => {
        if (value === null) return "null";
        if (value === undefined) return "undefined";
        if (typeof value === "string") return `"${value}"`;
        if (typeof value === "number") return value.toString();
        if (typeof value === "boolean") return value.toString();
        return String(value);
      };

      const nodeType = typeof data === "string" ? "string" : 
                      typeof data === "number" ? "number" : 
                      typeof data === "boolean" ? "boolean" : "unknown";

      return (
        <div key={path} style={{ paddingLeft: level * 16, minHeight: 24 }}>
          <span style={{ color: "#666", marginRight: 8 }}>
            {path.split(".").pop()}:
          </span>
          <span style={{ 
            color: nodeType === "string" ? "#0a7e0a" : 
                   nodeType === "number" ? "#1a1aa1" : 
                   nodeType === "boolean" ? "#a01a1a" : "#333"
          }}>
            {formatValue(data)}
          </span>
        </div>
      );
    }

    return (
      <div key={path}>
        <div 
          style={{ 
            paddingLeft: level * 16, 
            minHeight: 24,
            cursor: "pointer",
            backgroundColor: isExpanded ? "#f0f0f0" : "transparent",
            borderRadius: 4,
            padding: "2px 4px"
          }}
          onClick={() => toggleExpanded(path)}
        >
          <span style={{ marginRight: 8, color: "#333" }}>
            {isExpanded ? "▼" : "▶"}
          </span>
          <span style={{ color: "#666", marginRight: 8 }}>
            {path.split(".").pop()}:
          </span>
          <span style={{ color: "#333", fontStyle: "italic" }}>
            {isArray ? `Array[${data.length}]` : `Object{${Object.keys(data).length}}`}
          </span>
        </div>
        
        {isExpanded && (
          <div>
            {Object.entries(data).map(([key, value]) => 
              renderTree(value, `${path}.${key}`, level + 1)
            )}
          </div>
        )}
      </div>
    );
  };

  const getStats = (data: any): { keys: number; depth: number; totalNodes: number } => {
    let totalNodes = 0;
    let maxDepth = 0;

    const traverse = (obj: any, depth: number = 0) => {
      maxDepth = Math.max(maxDepth, depth);
      totalNodes++;
      
      if (obj !== null && typeof obj === "object") {
        Object.values(obj).forEach(value => traverse(value, depth + 1));
      }
    };

    traverse(data);
    
    const keys = data !== null && typeof data === "object" ? Object.keys(data).length : 0;
    
    return { keys, depth: maxDepth, totalNodes };
  };

  const stats = data ? getStats(data) : { keys: 0, depth: 0, totalNodes: 0 };

  return (
    <div className="attribution-panel" style={style}>
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
        <h2 style={{ margin: 0 }}>Attribution</h2>
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
          {data && (
            <div style={{ marginBottom: 12 }}>
              <div style={{ 
                display: "flex", 
                gap: 16, 
                fontSize: "0.9em", 
                color: "#666",
                padding: 8,
                backgroundColor: "#f9f9f9",
                borderRadius: 4
              }}>
                <span><strong>Keys:</strong> {stats.keys}</span>
                <span><strong>Depth:</strong> {stats.depth}</span>
                <span><strong>Nodes:</strong> {stats.totalNodes}</span>
              </div>
              
              <input
                type="text"
                placeholder="Search attribution data..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{
                  width: "100%",
                  padding: "6px 8px",
                  marginTop: 8,
                  border: "1px solid #ddd",
                  borderRadius: 4,
                  fontSize: "0.9em"
                }}
              />
            </div>
          )}

          <div style={{
            border: "1px solid #ddd",
            borderRadius: 6,
            backgroundColor: "#fff",
            maxHeight: "400px",
            overflow: "auto",
            padding: "8px"
          }}>
            {data ? (
              renderTree(data)
            ) : (
              <div style={{ color: "#999", fontStyle: "italic", padding: 12 }}>
                No attribution data available
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}