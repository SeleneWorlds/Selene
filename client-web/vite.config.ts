import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'SELENE_');
  const apiTarget = env.SELENE_SERVER_API || 'http://localhost:8080';
  const webSocketTarget = new URL(env.SELENE_SERVER_WEBSOCKET || 'ws://localhost:8148/ws');

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        '/authorize': apiProxy(apiTarget),
        '/token': apiProxy(apiTarget),
        '/oauth': apiProxy(apiTarget),
        '/status': apiProxy(apiTarget),
        '/heartbeat': apiProxy(apiTarget),
        '/client': apiProxy(apiTarget),
        '/bundles': apiProxy(apiTarget),
        '/join': apiProxy(apiTarget),
        '/leave': apiProxy(apiTarget),
        '/ws': {
          target: webSocketTarget.origin,
          changeOrigin: false,
          rewrite: () => `${webSocketTarget.pathname}${webSocketTarget.search}`,
          ws: true,
        },
      },
    },
  };
});

function apiProxy(target: string) {
  return {
    target,
    changeOrigin: false,
  };
}
