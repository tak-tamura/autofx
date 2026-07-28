package com.takuro_tamura.autofx.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final String EXCLUDED_PATH = "/api/chart";
    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
        "(?i)(\"(?:password|passwd|secret|token|apiKey|apiSecret|authorization)\"\\s*:\\s*)\"[^\"]*\""
    );
    private static final Pattern SENSITIVE_FORM_VALUE = Pattern.compile(
        "(?i)((?:^|&)(?:password|passwd|secret|token|apiKey|apiSecret|authorization)=)[^&]*"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDED_PATH.equals(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        var wrappedRequest = new ContentCachingRequestWrapper(request);
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                log.info(
                    "HTTP request: method={}, uri={}, body={}",
                    request.getMethod(),
                    requestUri(request),
                    body(wrappedRequest.getContentAsByteArray(), request.getCharacterEncoding(), request.getContentType())
                );
                log.info(
                    "HTTP response: method={}, uri={}, status={}, body={}",
                    request.getMethod(),
                    requestUri(request),
                    wrappedResponse.getStatus(),
                    responseBody(wrappedResponse)
                );
            } finally {
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private String requestUri(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + request.getQueryString();
    }

    private String body(byte[] content, String characterEncoding, String contentType) {
        if (content.length == 0) {
            return "";
        }
        if (!isTextContent(contentType)) {
            return "[binary content omitted]";
        }

        Charset charset = StandardCharsets.UTF_8;
        try {
            if (characterEncoding != null && Charset.isSupported(characterEncoding)) {
                charset = Charset.forName(characterEncoding);
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to UTF-8 when a malformed or unsupported charset is supplied.
        }
        return maskSensitiveValues(new String(content, charset));
    }

    private String responseBody(ContentCachingResponseWrapper response) {
        if (isScreenContent(response.getContentType())) {
            return "[screen content omitted]";
        }
        return body(
            response.getContentAsByteArray(),
            response.getCharacterEncoding(),
            response.getContentType()
        );
    }

    private boolean isScreenContent(String contentType) {
        if (contentType == null) {
            return false;
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.TEXT_HTML.includes(mediaType)
                || "css".equals(mediaType.getSubtype())
                || "javascript".equals(mediaType.getSubtype())
                || "x-javascript".equals(mediaType.getSubtype());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTextContent(String contentType) {
        if (contentType == null) {
            return true;
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return true;
        }
        return "text".equals(mediaType.getType())
            || MediaType.APPLICATION_JSON.includes(mediaType)
            || MediaType.APPLICATION_XML.includes(mediaType)
            || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
    }

    private String maskSensitiveValues(String content) {
        String maskedJson = SENSITIVE_JSON_VALUE.matcher(content).replaceAll("$1\"***\"");
        return SENSITIVE_FORM_VALUE.matcher(maskedJson).replaceAll("$1***");
    }
}
