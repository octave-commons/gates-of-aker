import WebSocket from "ws";
export class WSClient {
    ws = null;
    url;
    onMessage;
    onStatus;
    reconnectAttempts = 0;
    maxReconnectAttempts = 5;
    reconnectDelay = 3000;
    constructor(url, onMessage, onStatus) {
        this.url = url;
        this.onMessage = onMessage;
        this.onStatus = onStatus;
    }
    connect() {
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
            this.ws.on("message", (data) => {
                try {
                    const msg = JSON.parse(data.toString());
                    this.onMessage(msg);
                }
                catch (e) {
                    console.error("Failed to parse message:", e);
                }
            });
        }
        catch (e) {
            this.onStatus("error");
            console.error("Failed to create WebSocket connection:", e);
        }
    }
    attemptReconnect() {
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
    send(msg) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.warn("Cannot send message: WebSocket not connected");
            return;
        }
        try {
            this.ws.send(JSON.stringify(msg));
        }
        catch (e) {
            console.error("Failed to send message:", e);
        }
    }
    close() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }
    isConnected() {
        return this.ws?.readyState === WebSocket.OPEN;
    }
}
//# sourceMappingURL=ws-client.js.map