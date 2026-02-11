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
  private bufferedMessages: WSMessage[] = [];
  private connectionPromise: Promise<void> | null = null;
  private isConnected = false;
  private disconnectTimer: NodeJS.Timeout | null = null;
  private isShuttingDown = false;
  private lastTick: number | null = null;

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

    this.bufferedMessages = [];
    this.isShuttingDown = false;

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
        }
      };

      this.client.connect();
    });

    return this.connectionPromise;
  }

  async disconnect(): Promise<void> {
    this.isShuttingDown = true;
    this.client.close();
    this.isConnected = false;
    this.connectionPromise = null;
    if (this.disconnectTimer) {
      clearTimeout(this.disconnectTimer);
      this.disconnectTimer = null;
    }
    
    this.messageQueue.forEach(promise => {
      if (promise.timeout) clearTimeout(promise.timeout);
      promise.resolve({ op: 'error', message: 'Connection closed' });
    });
    this.messageQueue = [];
    this.bufferedMessages = [];
  }

  async waitForMessage(op: string, timeoutMs: number = this.config.timeout): Promise<WSMessage> {
    const bufferedMessage = this.consumeBufferedMessage(op);
    if (bufferedMessage) {
      return bufferedMessage;
    }

    return new Promise((resolve, reject) => {
      const messagePromise: MessagePromise = {
        resolve,
        reject,
        op
      };

      const timeout = setTimeout(() => {
        const index = this.messageQueue.indexOf(messagePromise);
        if (index !== -1) {
          this.messageQueue.splice(index, 1);
        }
        reject(new Error(`Timeout waiting for '${op}' message after ${timeoutMs}ms`));
      }, timeoutMs);

      messagePromise.timeout = timeout;

      this.messageQueue.push(messagePromise);
    });
  }

  async waitForHello(): Promise<{ state: Snapshot }> {
    const message = await this.waitForMessage('hello');
    if (message.op !== 'hello' || !message.state) {
      throw new Error('Invalid hello message received');
    }
    const hello = message as { state: Snapshot };
    if (typeof hello.state?.tick === 'number') {
      this.lastTick = hello.state.tick;
    }
    return hello;
  }

  async waitForTick(): Promise<{ data: { tick: number; snapshot?: Snapshot } }> {
    const message = await this.waitForMessage('tick');
    if (message.op !== 'tick' || !message.data) {
      throw new Error('Invalid tick message received');
    }
    const tickMessage = message as { data: { tick: number; snapshot?: Snapshot } };
    if (typeof tickMessage.data?.tick === 'number') {
      this.lastTick = tickMessage.data.tick;
    }
    return tickMessage;
  }

  async waitForReset(): Promise<{ state: Snapshot }> {
    const message = await this.waitForMessage('reset');
    if (message.op !== 'reset' || !message.state) {
      throw new Error('Invalid reset message received');
    }
    const resetMessage = message as { state: Snapshot };
    if (typeof resetMessage.state?.tick === 'number') {
      this.lastTick = resetMessage.state.tick;
    }
    return resetMessage;
  }

  sendMessage(message: unknown): void {
    if (!this.isConnected) {
      throw new Error('Cannot send message - not connected');
    }
    this.client.send(message);
  }

  async tick(count: number = 1): Promise<{ data: { tick: number; snapshot?: Snapshot } }> {
    const baselineTick = this.lastTick;
    const tickPromise = this.waitForTick();
    this.sendMessage({ op: 'tick', n: count });
    let tickResult = await tickPromise;

    if (typeof baselineTick === 'number') {
      let attempts = 0;
      while (typeof tickResult.data?.tick === 'number' && tickResult.data.tick <= baselineTick && attempts < 3) {
        tickResult = await this.waitForTick();
        attempts += 1;
      }
    }

    return tickResult;
  }

  async reset(options: { seed?: number; tree_density?: number; bounds?: unknown } = {}): Promise<{ state: Snapshot }> {
    const resetPromise = this.waitForReset();
    this.sendMessage({ op: 'reset', ...options });
    return resetPromise;
  }

  setLevers(levers: Record<string, unknown>): void {
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

  getAgentPath(agentId: string | number): void {
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
      return;
    }

    this.bufferMessage(message);
  }

  private handleStatus(status: 'open' | 'closed' | 'error'): void {
    if (status === 'open') {
      this.isConnected = true;
      if (this.disconnectTimer) {
        clearTimeout(this.disconnectTimer);
        this.disconnectTimer = null;
      }
      return;
    }

    if (status === 'closed') {
      this.isConnected = false;

      if (this.isShuttingDown) {
        return;
      }

      if (this.disconnectTimer) {
        clearTimeout(this.disconnectTimer);
      }
      this.disconnectTimer = setTimeout(() => {
        if (this.isConnected) {
          return;
        }
        this.messageQueue.forEach(promise => {
          if (promise.timeout) clearTimeout(promise.timeout);
          promise.reject(new Error('Connection closed'));
        });
        this.messageQueue = [];
        this.bufferedMessages = [];
        this.disconnectTimer = null;
      }, 350);
      return;
    }

    if (status === 'error') {
      this.isConnected = false;

      if (this.disconnectTimer) {
        clearTimeout(this.disconnectTimer);
        this.disconnectTimer = null;
      }

      this.messageQueue.forEach(promise => {
        if (promise.timeout) clearTimeout(promise.timeout);
        promise.reject(new Error('Connection error'));
      });
      this.messageQueue = [];
      this.bufferedMessages = [];
    }
  }

  private bufferMessage(message: WSMessage): void {
    this.bufferedMessages.push(message);
    if (this.bufferedMessages.length > 256) {
      this.bufferedMessages.shift();
    }
  }

  private consumeBufferedMessage(op: string): WSMessage | null {
    const messageIndex = this.bufferedMessages.findIndex(message => message.op === op);
    if (messageIndex < 0) {
      return null;
    }

    const [message] = this.bufferedMessages.splice(messageIndex, 1);
    return message ?? null;
  }

  getConnectionState(): boolean {
    return this.isConnected;
  }

  async waitForHealthCheck(timeoutMs: number = 5000): Promise<boolean> {
    try {
      const protocolUrl = this.config.url
        .replace(/^ws:/, 'http:')
        .replace(/^wss:/, 'https:');
      const healthUrl = `${new URL(protocolUrl).origin}/healthz`;
      const response = await fetch(healthUrl, {
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
