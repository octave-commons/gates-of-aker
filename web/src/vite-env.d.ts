/// <reference types="vite/client" />

declare interface ImportMetaEnv {
  readonly VITE_BACKEND_ORIGIN?: string;
}

declare interface ImportMeta {
  readonly env: ImportMetaEnv;
}
