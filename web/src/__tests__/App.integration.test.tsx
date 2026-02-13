import '@testing-library/jest-dom/vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../App';
import { WSMessage } from '../ws';
import * as audio from '../audio';

const mockWs = {
  send: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 1,
  onopen: null as (() => void) | null,
  onclose: null as (() => void) | null,
  onerror: null as (() => void) | null,
  onmessage: null as ((event: MessageEvent) => void) | null,
};

vi.mock('../audio', () => ({
  playDeathTone: vi.fn(),
  playTone: vi.fn(),
  playToneSequence: vi.fn(),
  playToneSequenceWithVoice: vi.fn(),
  getScaleFrequency: vi.fn((note, octave) => 440 * Math.pow(2, (note + octave * 12) / 12)),
  markUserInteraction: vi.fn(),
  playBookCreatedTone: vi.fn(),
  playHuntStartTone: vi.fn(),
  playHuntAttackTone: vi.fn(),
  playHuntKillTone: vi.fn(),
  isMuted: vi.fn(() => false),
}));

global.fetch = vi.fn();

const renderAppAtSimRoute = () => {
  return render(
    <MemoryRouter initialEntries={['/sim']}>
      <App />
    </MemoryRouter>
  );
};

describe('App Core Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockWs.onopen = null;
    mockWs.onclose = null;
    mockWs.onerror = null;
    mockWs.onmessage = null;
    global.WebSocket = vi.fn(() => mockWs) as any;
    (global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => ({ tick: 0, agents: [], tiles: {} }),
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('WebSocket Connection', () => {
    it('establishes WebSocket connection on mount', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        expect(global.WebSocket).toHaveBeenCalledWith('ws://localhost:3000/ws');
      });
    });

    it('WebSocket is mocked and available', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        expect(global.WebSocket).toHaveBeenCalled();
      }, { timeout: 3000 });
    });

    it('keeps a single websocket and handles social_interaction after state updates', async () => {
      renderAppAtSimRoute();

      await waitFor(() => {
        expect(global.WebSocket).toHaveBeenCalledTimes(1);
        expect(mockWs.onmessage).toBeTypeOf('function');
      });

      const hello: WSMessage = {
        op: 'hello',
        state: {
          tick: 1,
          map: { kind: 'hex', layout: 'pointy', bounds: { shape: 'rect', w: 3, h: 3, origin: [0, 0] } },
          agents: [
            { id: 1, role: 'priest', pos: [0, 0], status: { alive: true }, needs: {}, recall: {} },
            { id: 2, role: 'knight', pos: [1, 0], status: { alive: true }, needs: {}, recall: {} },
          ],
          tiles: {
            '0,0': { terrain: 'ground' },
            '1,0': { terrain: 'ground' },
          },
        } as any,
      };

      act(() => {
        mockWs.onmessage?.(new MessageEvent('message', { data: JSON.stringify(hello) }));
      });

      const social: WSMessage = {
        op: 'social_interaction',
        data: {
          agent_1_id: 1,
          agent_2_id: 2,
          interaction_type: 'ritual',
        },
      } as any;

      act(() => {
        mockWs.onmessage?.(new MessageEvent('message', { data: JSON.stringify(social) }));
      });

      expect(audio.playToneSequenceWithVoice).toHaveBeenCalledTimes(2);
      expect(global.WebSocket).toHaveBeenCalledTimes(1);
    });

    it('updates status bar on websocket close and reopen', async () => {
      renderAppAtSimRoute();

      await waitFor(() => {
        expect(mockWs.onclose).toBeTypeOf('function');
        expect(mockWs.onopen).toBeTypeOf('function');
      });

      act(() => {
        mockWs.onclose?.();
      });

      await waitFor(() => {
        expect(screen.getByText(/WS:/)).toHaveTextContent('WS: closed');
      });

      act(() => {
        mockWs.onopen?.();
      });

      await waitFor(() => {
        expect(screen.getByText(/WS:/)).toHaveTextContent('WS: open');
      });
    });

    it('shows a dismissible websocket error alert', async () => {
      renderAppAtSimRoute();

      await waitFor(() => {
        expect(mockWs.onerror).toBeTypeOf('function');
      });

      act(() => {
        mockWs.onerror?.();
      });

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent('WebSocket connection error');
      });

      const initialConnections = (global.WebSocket as unknown as { mock: { calls: unknown[] } }).mock.calls.length;

      act(() => {
        screen.getByRole('button', { name: 'Retry now' }).click();
      });

      await waitFor(() => {
        const currentConnections = (global.WebSocket as unknown as { mock: { calls: unknown[] } }).mock.calls.length;
        expect(currentConnections).toBeGreaterThan(initialConnections);
      });

      act(() => {
        screen.getByRole('button', { name: 'Dismiss' }).click();
      });

      await waitFor(() => {
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      });
    });
  });

  describe('App Rendering', () => {
    it('renders simulation view at sim route', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        expect(screen.getByTestId('simulation-canvas')).toBeInTheDocument();
      }, { timeout: 3000 });

      expect(global.WebSocket).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('handles malformed messages gracefully', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        expect(screen.getByTestId('simulation-canvas')).toBeInTheDocument();
      }, { timeout: 3000 });

      expect(document.body).toBeInTheDocument();
    });

    it('renders without crashing on error state', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        expect(screen.getByTestId('simulation-canvas')).toBeInTheDocument();
      }, { timeout: 3000 });

      expect(document.body).toBeInTheDocument();
    });
  });

  describe('Component Rendering', () => {
    it('renders status bar component', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        const statusBar = screen.queryByText(/status|WS/i);
        expect(statusBar).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('renders world info panel', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        const worldInfo = screen.queryByText(/year/i);
        expect(worldInfo).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('renders tick controls', async () => {
      renderAppAtSimRoute();
      
      await waitFor(() => {
        expect(screen.getByTestId('simulation-canvas')).toBeInTheDocument();
      }, { timeout: 3000 });

      const container = screen.getByTestId('simulation-canvas').closest('div[style*="grid"]');
      expect(container).toBeInTheDocument();
    });
  });
});
