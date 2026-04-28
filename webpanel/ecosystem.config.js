module.exports = {
  apps: [
    {
      name: 'ni-webpanel',
      script: 'npm',
      args: 'run preview -- --host 0.0.0.0 --port 4000',
      cwd: './',
      watch: false,
      env: {
        NODE_ENV: 'production',
      },
    },
  ],
};
