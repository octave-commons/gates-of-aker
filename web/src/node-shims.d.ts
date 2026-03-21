declare namespace NodeJS {
  type Timeout = ReturnType<typeof setTimeout>;
}

declare const process: {
  env: Record<string, string | undefined>;
};

declare const global: typeof globalThis;
