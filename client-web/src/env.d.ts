/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SELENE_SERVER_API_URL: string;
  readonly VITE_SELENE_WEBSOCKET_URL: string;
  readonly VITE_SELENE_BASE_DOMAIN: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue';

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}
