// PM2 ecosystem file to run backend (Clojure) and frontend (Vite dev)
// Backend is watched and restarted on code changes. Frontend runs in dev mode.
module.exports = {
  apps: [
    {
      name: "gates-backend",
      script: "clojure",
      args: "-M:server",
      cwd: "./backend",
      instances: 1,
      autorestart: true,
      // Watch only the backend source and config files; ignore build/artifacts
      watch: ["src", "resources", "dev", "deps.edn"],
      ignore_watch: ["target", "logs"],
      watch_delay: 1000,
      env: {
        // Set JAVA_HOME if you rely on a non-default JVM location. PM2 will pick up real env values.
        JAVA_HOME: process.env.JAVA_HOME || "",
        PORT: 3000,
        NODE_ENV: "development"
      }
    },

    {
      name: "gates-frontend",
      script: "npm",
      args: "run dev",
      cwd: "./web",
      instances: 1,
      autorestart: true,
      // We don't watch frontend here; Vite already handles HMR in dev mode.
      watch: false,
      env: {
        NODE_ENV: "development"
      }
    }
  ]
};
