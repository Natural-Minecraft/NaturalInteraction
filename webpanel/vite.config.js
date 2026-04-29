import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  root: '.',
  server: { port: 4000, open: false },
  preview: { port: 4000, allowedHosts: ['story.naturalsmp.net'] },
  build: { outDir: 'dist', emptyOutDir: true },
});
