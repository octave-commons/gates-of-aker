type LogLevel = 'error' | 'warn' | 'info' | 'debug';

const logLevels: Record<LogLevel, number> = {
  error: 0,
  warn: 1,
  info: 2,
  debug: 3,
};

const currentLevel: number =
  logLevels[(import.meta.env.VITE_LOG_LEVEL as LogLevel) || 'warn'];

function shouldLog(level: LogLevel): boolean {
  return logLevels[level] <= currentLevel;
}

export function logError(...args: unknown[]): void {
  if (shouldLog('error')) {
    console.error('[ERROR]', ...args);
  }
}

export function logWarn(...args: unknown[]): void {
  if (shouldLog('warn')) {
    console.warn('[WARN]', ...args);
  }
}

export function logInfo(...args: unknown[]): void {
  if (shouldLog('info')) {
    console.info('[INFO]', ...args);
  }
}

export function logDebug(...args: unknown[]): void {
  if (shouldLog('debug')) {
    console.debug('[DEBUG]', ...args);
  }
}

export function getLogLevel(): string {
  return (import.meta.env.VITE_LOG_LEVEL as LogLevel) || 'warn';
}
