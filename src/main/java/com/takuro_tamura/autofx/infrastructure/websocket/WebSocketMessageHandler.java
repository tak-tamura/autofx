package com.takuro_tamura.autofx.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.takuro_tamura.autofx.infrastructure.websocket.response.ExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class WebSocketMessageHandler implements WebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(WebSocketMessageHandler.class);
    private final ObjectMapper objectMapper;
    private final Consumer<ExecutionResponse> executionConsumer;
    private final Runnable onDisconnect;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established. Sending subscribe message");
        final String subscribeMessage = "{\"command\" : \"subscribe\", \"channel\" : \"executionEvents\"}";
        session.sendMessage(new TextMessage(subscribeMessage));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleText(session, textMessage);
        } else if (message instanceof PingMessage pingMessage) {
            log.info("Received ping from server. Sending pong.");
            session.sendMessage(new PongMessage(pingMessage.getPayload()));
        } else {
            log.warn("Unhandled message type: {}", message.getClass().getSimpleName());
        }
    }

    private void handleText(WebSocketSession session, TextMessage message) {
        try {
            log.info("Received execution info");
            final ExecutionResponse execution = objectMapper.readValue(message.getPayload(), ExecutionResponse.class);
            executionConsumer.accept(execution);
        } catch (Exception e) {
            log.error("Failed to handle execution info", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("WebSocket closed. Status: {}", status);
        onDisconnect.run();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

