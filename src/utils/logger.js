const timestamp = () => new Date().toISOString();

const write = (stream, level, args) => {
  stream(`${timestamp()} [BOT] [${level}]`, ...args);
};

export const logger = {
  info: (...args) => write(console.log, "INFO", args),
  warn: (...args) => write(console.warn, "WARN", args),
  error: (...args) => write(console.error, "ERROR", args)
};

// Backward-compatible simple log helper
export const log = (...args) => logger.info(...args);