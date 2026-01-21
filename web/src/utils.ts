import { CONFIG } from "./config/constants";

const clamp01 = (x: number): number => Math.max(0, Math.min(1, x));

const fmt = (n: unknown): string => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

const colorForRole = (role?: string): string => {
  switch (role) {
    case "priest":
      return CONFIG.colors.ROLE.priest;
    case "knight":
      return CONFIG.colors.ROLE.knight;
    case "champion":
      return "#0f766e";
    case "wolf":
      return "#795548";
    case "bear":
      return "#5d4037";
    case "deer":
      return "#8d6e63";
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
    case "champion":
      return "🜂";
    case "wolf":
      return "🐺";
    case "bear":
      return "🐻";
    case "deer":
      return "🦌";
    default:
      return "👤";
  }
};

const getDexterity = (stats?: Record<string, number>): number => {
  if (stats && typeof stats.dexterity === "number") {
    return stats.dexterity;
  }
  return 0.4;
};

const getMovementSteps = (stats?: Record<string, number>) => {
  const dex = getDexterity(stats);
  const base = 1 + Math.floor(dex * 2);
  const roadBonus = 1 + Math.floor(dex * 3);
  return {
    base,
    road: base + roadBonus,
  };
};

export { clamp01, fmt, colorForRole, getAgentIcon, getMovementSteps };
