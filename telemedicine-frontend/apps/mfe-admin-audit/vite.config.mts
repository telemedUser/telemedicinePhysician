import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';
import { dirname } from 'path';
import { telemedicineAliases } from '../../vite.aliases.mts';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: __dirname,
  cacheDir: '../../node_modules/.vite/mfe-admin-audit',
  plugins: [
    react(),
    federation({
      name: 'mfeAdminAudit',
      filename: 'remoteEntry.js',
      exposes: {
        './App': './src/App.tsx',
      },
      shared: {
        react: { singleton: true },
        'react-dom': { singleton: true },
      },
      dts: false,
    }),
  ],
  server: {
    port: 3023,
    strictPort: true,
    origin: 'http://localhost:3023',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 3023,
    strictPort: true,
  },
  build: {
    outDir: '../../dist/apps/mfe-admin-audit',
    emptyOutDir: true,
    target: 'esnext',
  },
  resolve: {
    alias: telemedicineAliases(__dirname),
  },
});
