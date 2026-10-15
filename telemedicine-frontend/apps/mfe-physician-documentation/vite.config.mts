import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';
import { dirname } from 'path';
import { telemedicineAliases } from '../../vite.aliases.mts';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: __dirname,
  cacheDir: '../../node_modules/.vite/mfe-physician-documentation',
  plugins: [
    react(),
    federation({
      name: 'mfePhysicianDocumentation',
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
    port: 3012,
    strictPort: true,
    origin: 'http://localhost:3012',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 3012,
    strictPort: true,
  },
  build: {
    outDir: '../../dist/apps/mfe-physician-documentation',
    emptyOutDir: true,
    target: 'esnext',
  },
  resolve: {
    alias: telemedicineAliases(__dirname),
  },
});
