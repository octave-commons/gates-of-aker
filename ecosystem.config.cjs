// PM2 ecosystem file to run backend (Clojure) and frontend (Vite dev)
// Backend is watched and restarted on code changes. Frontend runs in dev mode.
module.exports = {
  apps: [
      {
          name: "gates-backend",
          interpreter:"bash",
          script: "clojure",
          args: ["-M:server"],
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
          name:"gates-web",
          interpreter: "bash",
          script:"npm",
          args:["run", "dev"],
          cwd: "./web",
          instances: 1,
          autorestart: false,
          // No need to watch frontend files; Vite handles HMR internally
          env: {
              PORT: 5173,
              NODE_ENV: "development"
          }
      }
  ]
};
