import { useState, type ReactNode, type CSSProperties } from "react";

type Tab = {
  id: string;
  label: string;
  icon?: string;
  content: ReactNode;
};

type TabPanelProps = {
  tabs: Tab[];
  defaultTabId?: string;
  style?: CSSProperties;
};

export function TabPanel({ tabs, defaultTabId, style = {} }: TabPanelProps) {
  const [activeTabId, setActiveTabId] = useState(defaultTabId ?? tabs[0]?.id ?? "");
  const activeTab = tabs.find((t) => t.id === activeTabId);

  return (
    <div style={style}>
      <div style={{ display: "flex", gap: 4, marginBottom: 12, borderBottom: "1px solid #ddd" }}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTabId(tab.id)}
            style={{
              padding: "6px 12px",
              border: "none",
              background: activeTabId === tab.id ? "#2196F3" : "transparent",
              color: activeTabId === tab.id ? "#fff" : "#666",
              cursor: "pointer",
              borderRadius: "4px 4px 0 0",
              fontSize: "12px",
              fontWeight: activeTabId === tab.id ? 600 : 400,
              display: "flex",
              alignItems: "center",
              gap: 4,
              transition: "all 0.2s ease"
            }}
          >
            {tab.icon && <span>{tab.icon}</span>}
            <span>{tab.label}</span>
          </button>
        ))}
      </div>
      <div style={{ minHeight: 100 }}>
        {activeTab?.content}
      </div>
    </div>
  );
}
