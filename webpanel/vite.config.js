import { defineConfig } from 'vite';

export default defineConfig({
  root: '.',
  server: {
    port: 4000,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:25809',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:25809',
        ws: true,
      },
    },
  },
  preview: {
    allowedHosts: ['story.naturalsmp.net'],
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
