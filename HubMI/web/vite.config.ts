/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

const backend = process.env.HUBMI_BACKEND ?? 'http://localhost:8080'

export default defineConfig(({ mode }) => ({
  // Served from the site root, except for the demo on GitHub Pages (VITE_BASE=/<repository>/).
  base: loadEnv(mode, process.cwd()).VITE_BASE ?? '/',
  plugins: [react()],
  // Resolve imports of the symlinked Kotlin/JS client (e.g. `ws`) from this project's node_modules.
  resolve: { preserveSymlinks: true },
  server: {
    port: 5173,
    // The Kotlin/JS client lives outside this directory (symlinked npm package).
    fs: { allow: ['..'] },
    // Same-origin API in dev: no CORS needed. In production the reverse proxy does the same.
    proxy: {
      '/api': backend,
      '/health': backend,
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
}))
