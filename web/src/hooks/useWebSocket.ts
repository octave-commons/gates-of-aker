import { useEffect, useMemo, useRef, useState } from "react";
import { WSClient, WSMessage } from "../ws";

type WSStatus = "open" | "closed" | "error";

type UseWebSocketOptions<TMessage extends WSMessage> = {
  onMessage: (message: TMessage) => void;
  backendOrigin?: string;
  WebSocketClass?: typeof WebSocket;
};

export function useWebSocket<TMessage extends WSMessage = WSMessage>({
  onMessage,
  backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000",
  WebSocketClass = WebSocket,
}: UseWebSocketOptions<TMessage>) {
  const [status, setStatus] = useState<WSStatus>("closed");
  const onMessageRef = useRef(onMessage);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  const wsUrl = useMemo(
    () => backendOrigin.replace(/^http/, "ws").replace(/\/$/, "") + "/ws",
    [backendOrigin]
  );

  const client = useMemo(
    () => new WSClient<TMessage>(wsUrl, (message) => onMessageRef.current(message), setStatus, WebSocketClass),
    [wsUrl, WebSocketClass]
  );

  useEffect(() => {
    client.connect();
    return () => client.close();
  }, [client]);

  const reconnect = () => {
    client.connect();
  };

  return { client, status, reconnect };
}
