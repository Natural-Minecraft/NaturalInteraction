module.exports = {
  apps: [
    {
      name: 'ni-webpanel',
      script: 'npx',
      args: 'vite --host 0.0.0.0 --port 3000',
      cwd: './',
      watch: false,
      env: {
        NODE_ENV: 'production',
      },
    },
  ],
};
