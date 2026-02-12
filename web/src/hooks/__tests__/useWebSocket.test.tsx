import { act, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useWebSocket } from "../useWebSocket";
import type { WSMessage } from "../../ws";

type ErrorMessage = Extract<WSMessage, { op: "error" }>;

type MockSocket = {
  readyState: number;
  send: ReturnType<typeof vi.fn>;
  close: ReturnType<typeof vi.fn>;
  onopen: (() => void) | null;
  onclose: (() => void) | null;
  onerror: (() => void) | null;
  onmessage: ((event: MessageEvent) => void) | null;
};

class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;
  static instances: MockSocket[] = [];

  readyState = MockWebSocket.CONNECTING;
  send = vi.fn();
  close = vi.fn(() => {
    this.readyState = MockWebSocket.CLOSED;
  });
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;

  constructor(_url: string) {
    MockWebSocket.instances.push(this);
  }

  static reset() {
    MockWebSocket.instances = [];
  }
}

function HookProbe(props: {
  onMessage: (message: WSMessage) => void;
  backendOrigin?: string;
  WebSocketClass?: typeof WebSocket;
}) {
  const { status } = useWebSocket(props);
  return <div data-testid="status">{status}</div>;
}

function ErrorHookProbe(props: {
  onMessage: (message: ErrorMessage) => void;
  backendOrigin?: string;
  WebSocketClass?: typeof WebSocket;
}) {
  const { status } = useWebSocket<ErrorMessage>(props);
  return <div data-testid="error-status">{status}</div>;
}

describe("useWebSocket", () => {
  it("connects, updates status, and closes on unmount", () => {
    MockWebSocket.reset();
    const onMessage = vi.fn();
    const { unmount } = render(
      <HookProbe
        onMessage={onMessage}
        backendOrigin="http://localhost:3000"
        WebSocketClass={MockWebSocket as unknown as typeof WebSocket}
      />
    );

    expect(MockWebSocket.instances).toHaveLength(1);
    const socket = MockWebSocket.instances[0];
    expect(screen.getByTestId("status").textContent).toBe("closed");

    act(() => {
      socket.readyState = MockWebSocket.OPEN;
      socket.onopen?.();
    });

    expect(screen.getByTestId("status").textContent).toBe("open");

    unmount();

    expect(socket.close).toHaveBeenCalledTimes(1);
  });

  it("uses latest onMessage callback without recreating socket", () => {
    MockWebSocket.reset();
    const onMessageA = vi.fn();
    const onMessageB = vi.fn();

    const { rerender } = render(
      <HookProbe
        onMessage={onMessageA}
        backendOrigin="http://localhost:3000"
        WebSocketClass={MockWebSocket as unknown as typeof WebSocket}
      />
    );

    expect(MockWebSocket.instances).toHaveLength(1);
    const socket = MockWebSocket.instances[0];

    rerender(
      <HookProbe
        onMessage={onMessageB}
        backendOrigin="http://localhost:3000"
        WebSocketClass={MockWebSocket as unknown as typeof WebSocket}
      />
    );

    expect(MockWebSocket.instances).toHaveLength(1);

    act(() => {
      socket.onmessage?.({ data: JSON.stringify({ op: "error", message: "boom" }) } as MessageEvent);
    });

    expect(onMessageA).not.toHaveBeenCalled();
    expect(onMessageB).toHaveBeenCalledWith({ op: "error", message: "boom" });
  });

  it("supports generic message narrowing for onMessage", () => {
    MockWebSocket.reset();
    const onErrorMessage = vi.fn<(message: ErrorMessage) => void>();

    render(
      <ErrorHookProbe
        onMessage={onErrorMessage}
        backendOrigin="http://localhost:3000"
        WebSocketClass={MockWebSocket as unknown as typeof WebSocket}
      />
    );

    expect(MockWebSocket.instances).toHaveLength(1);
    const socket = MockWebSocket.instances[0];

    act(() => {
      socket.onmessage?.({ data: JSON.stringify({ op: "error", message: "generic-boom" }) } as MessageEvent);
    });

    expect(onErrorMessage).toHaveBeenCalledWith({ op: "error", message: "generic-boom" });
  });
});
