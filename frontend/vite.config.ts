import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Vite dev server for the PhotoConnect SPA.
//
//   * Tailwind v4 plugs in directly — no postcss/tailwind config files needed,
//     all of Tailwind's setup is the `@import "tailwindcss"` in index.css.
//
//   * The /api proxy forwards every front-end fetch to the gateway on :8080.
//     This is purely a dev convenience so the SPA can call `/api/v1/auth/login`
//     without worrying about CORS at all during development.
//     In production the SPA is served from the same origin (or CORS at the
//     gateway covers the cross-origin case — already configured).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
