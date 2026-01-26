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
          ignore_watch: ["target","logs"], // if you don't include the logs ,it will restart every time a log is ggenerated, not ideal.
          watch_delay: 1000,
          // Output logs to backend/logs directory
          log_file: "./logs/backend.log",
          out_file: "./logs/backend-out.log",
          error_file: "./logs/backend-error.log",
          log_date_format: "YYYY-MM-DD HH:mm:ss Z",
          env: {
              // Set JAVA_HOME if you rely on a non-default JVM location. PM2 will pick up real env values.
              JAVA_HOME: process.env.JAVA_HOME || "",
              PORT: 3000,
              NODE_ENV: "development"
          }
      },
      {
          name: "gates-frontend",
          interpreter: "node",
          script: "npm",
          args: ["run", "dev"],
          cwd: "./web",
          instances: 1,
          autorestart: true,
          watch: ["src", "index.html"],
          watch_delay: 1000,
          // Output logs to backend/logs directory
          log_file: "../backend/logs/frontend.log",
          out_file: "../backend/logs/frontend-out.log",
          error_file: "../backend/logs/frontend-error.log",
          log_date_format: "YYYY-MM-DD HH:mm:ss Z",
          env: {
              PORT: 5173,
              NODE_ENV: "development"
          }
      },
  ]
};
