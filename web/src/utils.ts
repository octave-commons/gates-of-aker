import { CONFIG } from "./config/constants";

const clamp01 = (x: number): number => Math.max(0, Math.min(1, x));

const fmt = (n: unknown): string => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

const colorForRole = (role?: string): string => {
  switch (role) {
    case "priest":
      return CONFIG.colors.ROLE.priest;
    case "knight":
      return CONFIG.colors.ROLE.knight;
    case "wolf":
      return "#795548";
    case "bear":
      return "#5d4037";
    default:
      return CONFIG.colors.ROLE.default;
  }
};

const getAgentIcon = (role?: string): string => {
  switch (role) {
    case "priest":
      return "✝";
    case "knight":
      return "⚔";
    case "wolf":
      return "🐺";
    case "bear":
      return "🐻";
    default:
      return "👤";
  }
};

export { clamp01, fmt, colorForRole, getAgentIcon };
