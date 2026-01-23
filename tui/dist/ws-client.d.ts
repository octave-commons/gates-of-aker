import { WSMessage, ConnectionState } from "./types";
export declare class WSClient {
    private ws;
    private url;
    private onMessage;
    private onStatus;
    private reconnectAttempts;
    private maxReconnectAttempts;
    private reconnectDelay;
    constructor(url: string, onMessage: (msg: WSMessage) => void, onStatus: (status: ConnectionState) => void);
    connect(): void;
    private attemptReconnect;
    send(msg: any): void;
    close(): void;
    isConnected(): boolean;
}
//# sourceMappingURL=ws-client.d.ts.map