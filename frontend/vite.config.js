import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development the browser talks to Vite (port 5173), and Vite forwards
// every /api/... request to Spring Boot (port 8080). To the browser it looks like
// ONE server, so CORS never comes into play and you don't have to change the backend.
export default defineConfig({
  plugins: [react()],
  // `npm run build` writes the finished website into Spring Boot's static folder,
  // so the backend can serve it on port 8080 (see the root README).
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true, // required because the folder is outside the frontend project
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
