package com.takuro_tamura.autofx.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.takuro_tamura.autofx.infrastructure.websocket.response.ExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class WebSocketMessageHandler extends TextWebSocketHandler {
    private final Logger log = LoggerFactory.getLogger(TextWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final Consumer<ExecutionResponse> executionConsumer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established. Sending subscribe message");
        final String subscribeMessage = "{\"command\" : \"subscribe\", \"channel\" : \"executionEvents\"}";
        session.sendMessage(new TextMessage(subscribeMessage));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.info("message: {}", message.getPayload());
            final ExecutionResponse execution = objectMapper.readValue(message.getPayload(), ExecutionResponse.class);
            log.info("Received execution info: {}", execution);
            executionConsumer.accept(execution);
        } catch (Exception e) {
            log.error("Failed to handle execution info", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable th) {
        log.error("Transport error occurred", th);
    }
}
