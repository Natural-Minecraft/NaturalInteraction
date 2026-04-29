import { defineConfig } from 'vite';

export default defineConfig({
  root: '.',
  server: {
    port: 4000,
    open: false,
  },
  preview: {
    port: 4000,
    allowedHosts: ['story.naturalsmp.net'],
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
