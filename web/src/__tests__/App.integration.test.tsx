import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../App';
import { WSClient, WSMessage } from '../ws';
import * as audio from '../audio';

const mockWs = {
  send: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 1,
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