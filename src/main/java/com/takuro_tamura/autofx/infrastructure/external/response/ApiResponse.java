package com.takuro_tamura.autofx.infrastructure.external.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ApiResponse<T> {
    private Integer status;

    private T data;

    private List<ErrorMessage> messages;

    private Instant responsetime;

    public boolean isTooManyRequests() {
        return messages.stream().anyMatch(message -> message.messageCode.equals("ERR-5003"));
    }

    @Data
    public static class ErrorMessage {
        @JsonProperty(value = "message_code")
        private String messageCode;
        @JsonProperty(value = "message_string")
        private String messageString;
    }
}
