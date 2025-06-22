package com.takuro_tamura.autofx.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.takuro_tamura.autofx.application.ExecutionApplicationService;
import com.takuro_tamura.autofx.application.command.Execution;
import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.websocket.response.ExecutionResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WebSocketApiClient {

    private final Logger log = LoggerFactory.getLogger(WebSocketApiClient.class);
    private final String webSocketApiUrl;
    private final PrivateApi privateApi;
    private final ExecutionApplicationService executionApplicationService;
    private final ObjectMapper objectMapper;
    private final RedisCacheService redisCacheService;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile WebSocketSession session;

    public WebSocketApiClient(
        @Value("${websocket-api.base-url}") String webSocketApiUrl,
        PrivateApi privateApi,
        ExecutionApplicationService executionApplicationService,
        ObjectMapper objectMapper,
        RedisCacheService redisCacheService
    ) {
        this.webSocketApiUrl = webSocketApiUrl;
        this.privateApi = privateApi;
        this.executionApplicationService = executionApplicationService;
        this.objectMapper = objectMapper;
        this.redisCacheService = redisCacheService;
    }

    @PostConstruct
    public void init() {
        try {
            connectWithRetry();
        } catch (Exception e) {
            log.error("Failed to connect WebSocket", e);
        }
    }

    public void reconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            log.warn("WebSocket disconnected. Attempting to reconnect...");
            connectWithRetry();
        } else {
            log.info("Reconnect already in progress.");
        }
    }

    private void connectWithRetry() {
        new Thread(() -> {
            int retryCount = 0;
            while (true) {
                try {
                    connect(); // 通常接続処理
                    log.info("WebSocket connection established.");
                    reconnecting.set(false);
                    break;
                } catch (Exception e) {
                    retryCount++;
                    log.error("WebSocket connection failed (attempt {}): {}", retryCount, e.getMessage());
                    try {
                        Thread.sleep(Math.min(10000L, retryCount * 2000L)); // 線形バックオフ
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    private void connect() throws Exception {
        final StandardWebSocketClient client = new StandardWebSocketClient();

        final String token = privateApi.issueWsToken();
        redisCacheService.save(CacheKey.WS_AUTH_TOKEN.getKey(), token);

        final String uri = webSocketApiUrl + token;

        final CompletableFuture<WebSocketSession> future = client.execute(
            new WebSocketMessageHandler(objectMapper, this::handleExecutionEvent, this::reconnect),
            uri
        );

        this.session = future.get();
        log.info("WebSocket session established, id: {}", session.getId());
    }

    private void handleExecutionEvent(ExecutionResponse executionResponse) {
        final Execution execution = new Execution(executionResponse);
        executionApplicationService.handleExecution(execution);
    }
}
