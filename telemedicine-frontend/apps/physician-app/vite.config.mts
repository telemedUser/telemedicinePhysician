import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { dirname } from 'path';
import { telemedicineAliases } from '../../vite.aliases.mts';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

// Standalone physician application. Intentionally NOT a Module Federation host or remote:
// every physician screen is a local route in a single deployable app.
export default defineConfig({
  root: __dirname,
  cacheDir: '../../node_modules/.vite/physician-app',
  plugins: [react()],
  server: {
    port: 3015,
    strictPort: true,
    proxy: {
      // Pointed directly at physician-service (:8114) for local review, bypassing the gateway.
      // The physician-service serves /api/v1/physicians/** and the chat WebSocket under /ws/chat.
      '/api': {
        target: 'http://localhost:8114',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8114',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  preview: {
    port: 3015,
    strictPort: true,
  },
  build: {
    outDir: '../../dist/apps/physician-app',
    emptyOutDir: true,
    target: 'esnext',
  },
  resolve: {
    alias: telemedicineAliases(__dirname),
  },
});
