/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import prerenderLanding from './vite-plugin-prerender-landing';

// Dev server proxies API calls to the Spring Boot backend so the frontend can
// call /api/* directly without CORS friction during local development.
//
// At build time, `prerenderLanding` emits crawlable static HTML for the apex
// landing route only (authenticated app routes are excluded); it runs as part
// of `npm run build` after the client bundle is written.
export default defineConfig({
  plugins: [react(), prerenderLanding()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // Vitest configuration for property (fast-check) and component
  // (jsdom + testing-library) tests.
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    include: ['src/**/*.{test,spec}.{js,jsx}'],
    css: true,
  },
});
