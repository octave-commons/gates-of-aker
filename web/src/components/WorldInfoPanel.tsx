import React from "react";

  type WorldInfoPanelProps = {
    calendar?: Record<string, unknown> | null;
  };

  const titleCase = (value: string | null | undefined) => {
   if (!value) return "Unknown";
   return value.charAt(0).toUpperCase() + value.slice(1);
 };
 
 const formatHour = (hour?: number) => {
   if (typeof hour !== "number" || Number.isNaN(hour)) return "--:--";
   const whole = Math.floor(hour);
   const minutes = Math.floor((hour - whole) * 60);
   return `${String(whole).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
 };
 
 const formatPercent = (value?: number) => {
   if (typeof value !== "number" || Number.isNaN(value)) return "--";
   return `${Math.round(value * 100)}%`;
 };
 
  const formatValue = (value?: number) => {
    if (typeof value !== "number" || Number.isNaN(value)) return "--";
    return value.toFixed(2);
  };

  const asString = (value: unknown): string | undefined =>
    typeof value === "string" ? value : undefined;

  const asNumber = (value: unknown): number | undefined =>
    typeof value === "number" ? value : undefined;
 
 export function WorldInfoPanel({ calendar }: WorldInfoPanelProps) {
    const timeOfDay = titleCase(asString(calendar?.["time-of-day"] ?? calendar?.timeOfDay));
    const season = titleCase(asString(calendar?.season));
    const day = asNumber(calendar?.day) ?? "--";
    const year = asNumber(calendar?.year) ?? "--";
    const hour = formatHour(asNumber(calendar?.hour));
    const temperature = formatValue(asNumber(calendar?.temperature));
    const daylight = formatPercent(asNumber(calendar?.daylight));
    const coldSnap = formatPercent(asNumber(calendar?.["cold-snap"] ?? calendar?.coldSnap));
 
   return (
     <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
       <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
         <div>
           <div style={{ fontSize: 16, fontWeight: 700 }}>{timeOfDay}</div>
           <div style={{ fontSize: 12, opacity: 0.7 }}>{hour}</div>
         </div>
         <div style={{ textAlign: "right", fontSize: 12 }}>
           <div>Day {day}</div>
           <div>{season} • Year {year}</div>
         </div>
       </div>
       <div style={{ marginTop: 8, display: "grid", gap: 6, fontSize: 12 }}>
         <div style={{ display: "flex", justifyContent: "space-between" }}>
           <span>Temperature</span>
           <span>{temperature}</span>
         </div>
         <div style={{ display: "flex", justifyContent: "space-between" }}>
           <span>Daylight</span>
           <span>{daylight}</span>
         </div>
         <div style={{ display: "flex", justifyContent: "space-between" }}>
           <span>Cold Snap</span>
           <span>{coldSnap}</span>
         </div>
       </div>
     </div>
   );
 }
