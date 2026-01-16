import WebSocket from "ws";
import { request } from "http";

const BACKEND_HTTP = process.env.BACKEND_HTTP ?? "http://localhost:3000";
const BACKEND_WS = BACKEND_HTTP.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";
const FRONTEND_URL = process.env.FRONTEND_URL ?? "http://localhost:5173";

async function waitForHttp(url, timeoutMs, label) {
  const start = Date.now();
  const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

  while (Date.now() - start < timeoutMs) {
    const ok = await new Promise((resolve) => {
      const req = request(url, (res) => {
        res.resume();
        resolve(res.statusCode === 200);
      });
      req.on("error", () => resolve(false));
      req.end();
    });

    if (ok) {
      return;
    }

    await delay(500);
  }

  throw new Error(`Timed out waiting for ${label} at ${url}`);
}

async function checkFrontendAssets(url) {
  await waitForHttp(url, 60_000, "frontend dev server");
}

async function driveWebSocketFlow() {
  await waitForHttp(`${BACKEND_HTTP}/healthz`, 60_000, "backend healthz");

  const ws = new WebSocket(BACKEND_WS);

  const once = (event) =>
    new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        ws.removeAllListeners(event);
        reject(new Error(`Timeout waiting for ${event}`));
      }, 10_000);
      ws.once(event, (data) => {
        clearTimeout(timer);
        resolve(data);
      });
    });

  await new Promise((resolve, reject) => {
    ws.once("open", resolve);
    ws.once("error", (err) => reject(err));
  });

  const helloRaw = await once("message");
  const hello = JSON.parse(helloRaw.toString());
  if (hello.op !== "hello") {
    throw new Error(`Expected hello, got ${hello.op}`);
  }

  ws.send(JSON.stringify({ op: "tick", n: 1 }));
  const tickRaw = await once("message");
  const tick = JSON.parse(tickRaw.toString());
  if (tick.op !== "tick") {
    throw new Error(`Expected tick, got ${tick.op}`);
  }

  ws.close();
}

async function main() {
  await Promise.all([driveWebSocketFlow(), checkFrontendAssets(FRONTEND_URL)]);
  console.info("E2E compose check succeeded");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
