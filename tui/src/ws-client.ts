import WebSocket from "ws";
import { WSMessage, ConnectionState } from "./types";

export class WSClient {
  private ws: WebSocket | null = null;
  private url: string;
  private onMessage: (msg: WSMessage) => void;
  private onStatus: (status: ConnectionState) => void;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;

  constructor(
    url: string,
    onMessage: (msg: WSMessage) => void,
    onStatus: (status: ConnectionState) => void
  ) {
    this.url = url;
    this.onMessage = onMessage;
    this.onStatus = onStatus;
  }

  connect(): void {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return;
    }

    try {
      this.ws = new WebSocket(this.url);

      this.ws.on("open", () => {
        this.reconnectAttempts = 0;
        this.onStatus("open");
      });

      this.ws.on("close", () => {
        this.onStatus("closed");
        this.attemptReconnect();
      });

      this.ws.on("error", (error) => {
        this.onStatus("error");
        console.error("WebSocket error:", error);
      });

      this.ws.on("message", (data: WebSocket.Data) => {
        try {
          const msg = JSON.parse(data.toString()) as WSMessage;
          this.onMessage(msg);
        } catch (e) {
          console.error("Failed to parse message:", e);
        }
      });
    } catch (e) {
      this.onStatus("error");
      console.error("Failed to create WebSocket connection:", e);
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error("Max reconnection attempts reached");
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts;
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    setTimeout(() => {
      this.connect();
    }, delay);
  }

  send(msg: any): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn("Cannot send message: WebSocket not connected");
      return;
    }
    try {
      this.ws.send(JSON.stringify(msg));
    } catch (e) {
      console.error("Failed to send message:", e);
    }
  }

  close(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }
}
