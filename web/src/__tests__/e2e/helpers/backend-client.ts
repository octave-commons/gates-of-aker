import { WSClient, WSMessage } from '../../../ws';
import type { Snapshot } from '../../../types';

export interface BackendTestConfig {
  url: string;
  timeout: number;
  retryAttempts: number;
}

export interface MessagePromise {
  resolve: (message: WSMessage) => void;
  reject: (error: Error) => void;
  op: string;
  timeout?: NodeJS.Timeout;
}

export class BackendTestClient {
  private client: WSClient;
  private messageQueue: MessagePromise[] = [];
  private connectionPromise: Promise<void> | null = null;
  private isConnected = false;

  constructor(private config: BackendTestConfig) {
    this.client = new WSClient(
      config.url,
      this.handleMessage.bind(this),
      this.handleStatus.bind(this)
    );
  }

  async connect(): Promise<void> {
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error(`Connection timeout after ${this.config.timeout}ms`));
      }, this.config.timeout);

      const originalStatusHandler = this.client['onStatus'];
      this.client['onStatus'] = (status: 'open' | 'closed' | 'error') => {
        originalStatusHandler(status);
        if (status === 'open') {
          clearTimeout(timeout);
          this.isConnected = true;
          resolve();
        } else if (status === 'error') {
          clearTimeout(timeout);
          reject(new Error('Connection failed'));
        }
      };

      this.client.connect();
    });

    return this.connectionPromise;
  }

  async disconnect(): Promise<void> {
    this.client.close();
    this.isConnected = false;
    this.connectionPromise = null;
    
    // Reject any pending messages
    this.messageQueue.forEach(promise => {
      if (promise.timeout) clearTimeout(promise.timeout);
      promise.reject(new Error('Connection closed'));
    });
    this.messageQueue = [];
  }

  async waitForMessage(op: string, timeoutMs: number = this.config.timeout): Promise<WSMessage> {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        const index = this.messageQueue.findIndex(p => p.op === op);
        if (index !== -1) {
          this.messageQueue.splice(index, 1);
        }
        reject(new Error(`Timeout waiting for '${op}' message after ${timeoutMs}ms`));
      }, timeoutMs);

      const messagePromise: MessagePromise = {
        resolve,
        reject,
        op,
        timeout
      };

      this.messageQueue.push(messagePromise);
    });
  }

  async waitForHello(): Promise<{ state: Snapshot }> {
    const message = await this.waitForMessage('hello');
    if (message.op !== 'hello' || !message.state) {
      throw new Error('Invalid hello message received');
    }
    return message as { state: Snapshot };
  }

  async waitForTick(): Promise<{ data: { tick: number; snapshot?: Snapshot } }> {
    const message = await this.waitForMessage('tick');
    if (message.op !== 'tick' || !message.data) {
      throw new Error('Invalid tick message received');
    }
    return message as { data: { tick: number; snapshot?: Snapshot } };
  }

  async waitForReset(): Promise<{ state: Snapshot }> {
    const message = await this.waitForMessage('reset');
    if (message.op !== 'reset' || !message.state) {
      throw new Error('Invalid reset message received');
    }
    return message as { state: Snapshot };
  }

  sendMessage(message: any): void {
    if (!this.isConnected) {
      throw new Error('Cannot send message - not connected');
    }
    this.client.send(message);
  }

  async tick(count: number = 1): Promise<{ data: { tick: number; snapshot?: Snapshot } }> {
    this.sendMessage({ op: 'tick', n: count });
    return this.waitForTick();
  }

  async reset(options: { seed?: number; tree_density?: number; bounds?: any } = {}): Promise<{ state: Snapshot }> {
    this.sendMessage({ op: 'reset', ...options });
    return this.waitForReset();
  }

  setLevers(levers: any): void {
    this.sendMessage({ op: 'set_levers', levers });
  }

  placeWallGhost(pos: [number, number]): void {
    this.sendMessage({ op: 'place_wall_ghost', pos });
  }

  placeStockpile(pos: [number, number], resource: string, maxQty?: number): void {
    this.sendMessage({ op: 'place_stockpile', pos, resource, max_qty: maxQty });
  }

  placeShrine(pos: [number, number]): void {
    this.sendMessage({ op: 'place_shrine', pos });
  }

  getAgentPath(agentId: number): void {
    this.sendMessage({ op: 'get_agent_path', agent_id: agentId });
  }

  startRun(): void {
    this.sendMessage({ op: 'start_run' });
  }

  stopRun(): void {
    this.sendMessage({ op: 'stop_run' });
  }

  setFps(fps: number): void {
    this.sendMessage({ op: 'set_fps', fps });
  }

  private handleMessage(message: WSMessage): void {
    const pendingIndex = this.messageQueue.findIndex(p => p.op === message.op);
    
    if (pendingIndex !== -1) {
      const pending = this.messageQueue[pendingIndex];
      this.messageQueue.splice(pendingIndex, 1);
      
      if (pending.timeout) {
        clearTimeout(pending.timeout);
      }
      
      pending.resolve(message);
    }
  }

  private handleStatus(status: 'open' | 'closed' | 'error'): void {
    if (status === 'closed' || status === 'error') {
      this.isConnected = false;
      
      // Reject all pending messages
      this.messageQueue.forEach(promise => {
        if (promise.timeout) clearTimeout(promise.timeout);
        promise.reject(new Error(`Connection ${status}`));
      });
      this.messageQueue = [];
    }
  }

  getConnectionState(): boolean {
    return this.isConnected;
  }

  async waitForHealthCheck(timeoutMs: number = 5000): Promise<boolean> {
    try {
      const response = await fetch(`${this.config.url.replace('ws://', 'http://').replace('wss://', 'https://')}/healthz`, {
        signal: AbortSignal.timeout(timeoutMs)
      });
      return response.ok;
    } catch (error) {
      return false;
    }
  }
}

export function createBackendTestClient(config?: Partial<BackendTestConfig>): BackendTestClient {
  const defaultConfig: BackendTestConfig = {
    url: process.env.VITE_BACKEND_ORIGIN ? 
      `${process.env.VITE_BACKEND_ORIGIN.replace('http://', 'ws://').replace('https://', 'wss://')}/ws` : 
      'ws://localhost:3000/ws',
    timeout: 10000,
    retryAttempts: 3
  };

  return new BackendTestClient({ ...defaultConfig, ...config });
}