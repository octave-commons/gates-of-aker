type LogLevel = "error" | "warn" | "info" | "debug";

const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
  error: 0,
  warn: 1,
  info: 2,
  debug: 3,
};

const parseLogLevel = (value: unknown): LogLevel => {
  if (typeof value !== "string") {
    return "warn";
  }

  const normalized = value.toLowerCase();
  if (normalized === "error" || normalized === "warn" || normalized === "info" || normalized === "debug") {
    return normalized;
  }

  return "warn";
};

const CURRENT_LEVEL = parseLogLevel(import.meta.env.VITE_LOG_LEVEL);

const shouldLog = (level: LogLevel): boolean => LOG_LEVEL_PRIORITY[level] <= LOG_LEVEL_PRIORITY[CURRENT_LEVEL];

export const logError = (...args: unknown[]): void => {
  if (shouldLog("error")) {
    console.error("[ERROR]", ...args);
  }
};

export const logWarn = (...args: unknown[]): void => {
  if (shouldLog("warn")) {
    console.warn("[WARN]", ...args);
  }
};

export const logInfo = (...args: unknown[]): void => {
  if (shouldLog("info")) {
    console.info("[INFO]", ...args);
  }
};

export const logDebug = (...args: unknown[]): void => {
  if (shouldLog("debug")) {
    console.debug("[DEBUG]", ...args);
  }
};
