package com.takuro_tamura.autofx.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpLoggingFilterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final HttpLoggingFilter filter = new HttpLoggingFilter();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsRequestAndResponseBodies() throws Exception {
        var request = jsonRequest("/api/v1/trade/config", "{\"enabled\":true}");
        var response = new MockHttpServletResponse();
        var chain = responseWritingChain(HttpServletResponse.SC_OK, "{\"result\":\"ok\"}");

        filter.doFilter(request, response, chain);

        assertThat(formattedMessages())
            .contains("HTTP request: method=POST, uri=/api/v1/trade/config, body={\"enabled\":true}")
            .contains("HTTP response: method=POST, uri=/api/v1/trade/config, status=200, body={\"result\":\"ok\"}");
        assertThat(response.getContentAsString()).isEqualTo("{\"result\":\"ok\"}");
    }

    @Test
    void doesNotLogChartRequestOrResponse() throws Exception {
        var request = jsonRequest("/api/chart", "{\"symbol\":\"USD_JPY\"}");
        request.setQueryString("refresh=true");
        var response = new MockHttpServletResponse();
        var chain = responseWritingChain(HttpServletResponse.SC_OK, "{\"candles\":[1,2,3]}");

        filter.doFilter(request, response, chain);

        assertThat(appender.list).isEmpty();
        assertThat(response.getContentAsString()).isEqualTo("{\"candles\":[1,2,3]}");
    }

    @Test
    void masksSensitiveJsonValues() throws Exception {
        var request = jsonRequest("/api/auth/register", "{\"username\":\"alice\",\"password\":\"secret\"}");
        var response = new MockHttpServletResponse();
        var chain = responseWritingChain(
            HttpServletResponse.SC_OK,
            "{\"token\":\"response-token\",\"result\":\"ok\"}"
        );

        filter.doFilter(request, response, chain);

        assertThat(formattedMessages())
            .contains("body={\"username\":\"alice\",\"password\":\"***\"}")
            .contains("body={\"token\":\"***\",\"result\":\"ok\"}")
            .doesNotContain("secret")
            .doesNotContain("response-token");
    }

    private MockHttpServletRequest jsonRequest(String uri, String body) {
        var request = new MockHttpServletRequest("POST", uri);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private FilterChain responseWritingChain(int status, String body) {
        return (request, response) -> {
            request.getInputStream().readAllBytes();
            var httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(status);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            httpResponse.getWriter().write(body);
        };
    }

    private String formattedMessages() {
        return appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (left, right) -> left + "\n" + right);
    }
}
