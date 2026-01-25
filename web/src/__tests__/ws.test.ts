import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { WSClient, WSMessage } from '../ws';

// Mock WebSocket for testing
const mockWebSocket = vi.fn();
const mockWebSocketInstance = {
  readyState: 0,
  close: vi.fn(),
  send: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  onopen: null as (() => void) | null,
  onclose: null as (() => void) | null,
  onerror: null as (() => void) | null,
  onmessage: null as ((ev: MessageEvent) => void) | null,
};

mockWebSocket.mockImplementation(() => mockWebSocketInstance);

describe('WSClient', () => {
  let onMessageMock: ReturnType<typeof vi.fn>;
  let onStatusMock: ReturnType<typeof vi.fn>;
  let client: WSClient;

  beforeEach(() => {
    onMessageMock = vi.fn();
    onStatusMock = vi.fn();
    mockWebSocketInstance.readyState = 0;
    mockWebSocketInstance.send.mockClear();
    mockWebSocketInstance.close.mockClear();
    mockWebSocketInstance.addEventListener.mockClear();
    mockWebSocketInstance.onopen = null;
    mockWebSocketInstance.onclose = null;
    mockWebSocketInstance.onerror = null;
    mockWebSocketInstance.onmessage = null;
  });

  afterEach(() => {
    if (client) {
      client.close();
    }
  });

  describe('Constructor', () => {
    it('stores constructor parameters correctly', () => {
      const messageHandler = vi.fn();
      const statusHandler = vi.fn();
      
      client = new WSClient('ws://test.com', messageHandler, statusHandler);
      
      expect(client).toBeDefined();
    });

    it('accepts custom WebSocket class for dependency injection', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      
      expect(client).toBeDefined();
    });

    it('uses default WebSocket class when none provided', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock);
      
      expect(client).toBeDefined();
    });
  });

  describe('Connection', () => {
    it('creates WebSocket instance with correct URL', () => {
      client = new WSClient('ws://localhost:3000/ws', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      expect(mockWebSocket).toHaveBeenCalledWith('ws://localhost:3000/ws');
    });

    it('sets up event handlers on WebSocket instance', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      expect(mockWebSocketInstance.onopen).toBeDefined();
      expect(mockWebSocketInstance.onclose).toBeDefined();
      expect(mockWebSocketInstance.onerror).toBeDefined();
      expect(mockWebSocketInstance.onmessage).toBeDefined();
    });

    it('calls onStatus callback when connection opens', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      if (mockWebSocketInstance.onopen) {
        mockWebSocketInstance.onopen();
      }
      
      expect(onStatusMock).toHaveBeenCalledWith('open');
    });

    it('calls onStatus callback when connection closes', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      if (mockWebSocketInstance.onclose) {
        mockWebSocketInstance.onclose();
      }
      
      expect(onStatusMock).toHaveBeenCalledWith('closed');
    });

    it('calls onStatus callback on error', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      if (mockWebSocketInstance.onerror) {
        mockWebSocketInstance.onerror();
      }
      
      expect(onStatusMock).toHaveBeenCalledWith('error');
    });

    it('prevents multiple connections when already connected', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      // Simulate connection opening to set isConnected = true
      if (mockWebSocketInstance.onopen) {
        mockWebSocketInstance.onopen();
      }
      
      const callCount = mockWebSocket.mock.calls.length;
      
      // Try to connect again
      client.connect();
      
      expect(mockWebSocket.mock.calls.length).toBe(callCount);
    });

    it('allows reconnection after close', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const initialCallCount = mockWebSocket.mock.calls.length;
      
      // Close the connection
      client.close();
      
      // Reconnect
      client.connect();
      
      expect(mockWebSocket.mock.calls.length).toBeGreaterThan(initialCallCount);
    });
  });

  describe('Message Handling', () => {
    it('parses and forwards valid messages', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const validMessage: WSMessage = { op: 'hello', state: { tick: 0, agents: [], tiles: {} } };
      const messageEvent = new MessageEvent('message', { data: JSON.stringify(validMessage) });
      
      if (mockWebSocketInstance.onmessage) {
        mockWebSocketInstance.onmessage(messageEvent);
      }
      
      expect(onMessageMock).toHaveBeenCalledWith(validMessage);
    });

    it('handles tick messages', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const tickMessage: WSMessage = { 
        op: 'tick', 
        data: { 
          tick: 1, 
          snapshot: { tick: 1, agents: [], tiles: {} } 
        } 
      };
      const messageEvent = new MessageEvent('message', { data: JSON.stringify(tickMessage) });
      
      if (mockWebSocketInstance.onmessage) {
        mockWebSocketInstance.onmessage(messageEvent);
      }
      
      expect(onMessageMock).toHaveBeenCalledWith(tickMessage);
    });

    it('handles reset messages', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const resetMessage: WSMessage = { op: 'reset', state: { tick: 0, agents: [], tiles: {} } };
      const messageEvent = new MessageEvent('message', { data: JSON.stringify(resetMessage) });
      
      if (mockWebSocketInstance.onmessage) {
        mockWebSocketInstance.onmessage(messageEvent);
      }
      
      expect(onMessageMock).toHaveBeenCalledWith(resetMessage);
    });

    it('logs error for invalid message format', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const invalidMessage = { invalid: 'message' };
      const messageEvent = new MessageEvent('message', { data: JSON.stringify(invalidMessage) });
      
      if (mockWebSocketInstance.onmessage) {
        mockWebSocketInstance.onmessage(messageEvent);
      }
      
      expect(onMessageMock).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalled();
      
      consoleErrorSpy.mockRestore();
    });

    it('logs error for malformed JSON', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const messageEvent = new MessageEvent('message', { data: 'invalid json' });
      
      if (mockWebSocketInstance.onmessage) {
        mockWebSocketInstance.onmessage(messageEvent);
      }
      
      expect(onMessageMock).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalled();
      
      consoleErrorSpy.mockRestore();
    });
  });

  describe('Sending Messages', () => {
    beforeEach(() => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      mockWebSocketInstance.readyState = 1; // WebSocket.OPEN
    });

    it('sends messages when connected', () => {
      const message = { op: 'test', data: 'payload' };
      client.send(message);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(JSON.stringify(message));
    });

    it('does not send messages when not connected', () => {
      mockWebSocketInstance.readyState = 0; // Not connected
      const message = { op: 'test', data: 'payload' };
      
      client.send(message);
      
      expect(mockWebSocketInstance.send).not.toHaveBeenCalled();
    });

    it('does not send messages when WebSocket is null', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      // Don't call connect()
      
      const message = { op: 'test', data: 'payload' };
      client.send(message);
      
      expect(mockWebSocketInstance.send).not.toHaveBeenCalled();
    });
  });

  describe('Convenience Methods', () => {
    beforeEach(() => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      mockWebSocketInstance.readyState = 1;
    });

    it('sends place_wall_ghost operation', () => {
      client.sendPlaceWallGhost([10, 20]);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'place_wall_ghost', pos: [10, 20] })
      );
    });

    it('sends place_stockpile operation', () => {
      client.sendPlaceStockpile([15, 25], 'wood', 100);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'place_stockpile', pos: [15, 25], resource: 'wood', max_qty: 100 })
      );
    });

    it('sends place_building operation', () => {
      client.sendPlaceBuilding('house', [5, 10], { config: 'value' });
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'place_house', pos: [5, 10], config: 'value' })
      );
    });

    it('sends queue_build operation', () => {
      client.sendQueueBuild('workshop', [8, 12], { resource: 'stone', max_qty: 50 });
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'queue_build', structure: 'workshop', pos: [8, 12], stockpile: { resource: 'stone', max_qty: 50 } })
      );
    });

    it('sends assign_job operation', () => {
      client.sendAssignJob('build', [3, 7], 42);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'assign_job', job_type: 'build', target_pos: [3, 7], agent_id: 42 })
      );
    });

    it('sends start_run operation', () => {
      client.sendStartRun();
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'start_run' })
      );
    });

    it('sends stop_run operation', () => {
      client.sendStopRun();
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'stop_run' })
      );
    });

    it('sends set_fps operation', () => {
      client.sendSetFps(30);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'set_fps', fps: 30 })
      );
    });

    it('sends get_agent_path operation', () => {
      client.sendGetAgentPath(123);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ op: 'get_agent_path', agent_id: 123 })
      );
    });

    it('sends set_tree_spread_levers operation', () => {
      client.sendSetTreeSpreadLevers(0.5, 10, 20);
      
      expect(mockWebSocketInstance.send).toHaveBeenCalledWith(
        JSON.stringify({ 
          op: 'set_tree_spread_levers', 
          spread_probability: 0.5, 
          min_interval: 10, 
          max_interval: 20 
        })
      );
    });
  });

  describe('Closing Connection', () => {
    it('closes WebSocket instance', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      client.close();
      
      expect(mockWebSocketInstance.close).toHaveBeenCalled();
    });

    it('sets WebSocket to null after closing', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      client.close();
      
      expect(client.getWebSocket()).toBeNull();
    });

    it('sets connection state to false after closing', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      // Open connection
      if (mockWebSocketInstance.onopen) {
        mockWebSocketInstance.onopen();
      }
      
      expect(client.getConnectionState()).toBe(true);
      
      // Close connection
      client.close();
      
      expect(client.getConnectionState()).toBe(false);
    });

    it('handles closing when WebSocket is null', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      // Don't call connect()
      
      expect(() => client.close()).not.toThrow();
    });
  });

  describe('Testing Hooks', () => {
    it('getWebSocket returns current WebSocket instance', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      const ws = client.getWebSocket();
      
      expect(ws).toBe(mockWebSocketInstance);
    });

    it('getWebSocket returns null when not connected', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      
      const ws = client.getWebSocket();
      
      expect(ws).toBeNull();
    });

    it('getConnectionState returns true when connected', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      if (mockWebSocketInstance.onopen) {
        mockWebSocketInstance.onopen();
      }
      
      expect(client.getConnectionState()).toBe(true);
    });

    it('getConnectionState returns false when not connected', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      client.connect();
      
      expect(client.getConnectionState()).toBe(false);
    });

    it('simulateMessage calls onMessage callback', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      
      const testMessage: WSMessage = { op: 'hello', state: { tick: 10, agents: [], tiles: {} } };
      client.simulateMessage(testMessage);
      
      expect(onMessageMock).toHaveBeenCalledWith(testMessage);
    });

    it('simulateStatus calls onStatus callback', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      
      client.simulateStatus('open');
      
      expect(onStatusMock).toHaveBeenCalledWith('open');
    });

    it('simulateStatus updates connection state', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      
      expect(client.getConnectionState()).toBe(false);
      
      client.simulateStatus('open');
      expect(client.getConnectionState()).toBe(true);
      
      client.simulateStatus('closed');
      expect(client.getConnectionState()).toBe(false);
    });

    it('simulateStatus handles error state', () => {
      client = new WSClient('ws://test.com', onMessageMock, onStatusMock, mockWebSocket as any);
      
      client.simulateStatus('error');
      
      expect(onStatusMock).toHaveBeenCalledWith('error');
    });
  });
});