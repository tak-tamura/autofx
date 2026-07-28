package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.presentation.controller.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        logger.error("Unhandled exception while processing REST API request", exception);

        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var response = new ErrorResponse(status.value(), status.getReasonPhrase());
        return ResponseEntity.status(status).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        var status = HttpStatus.resolve(statusCode.value());
        var message = status != null ? status.getReasonPhrase() : "Request failed";
        var response = new ErrorResponse(statusCode.value(), message);
        return super.handleExceptionInternal(exception, response, headers, statusCode, request);
    }
}
