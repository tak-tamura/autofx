package com.takuro_tamura.autofx.infrastructure.external.response;

import lombok.Data;

import java.util.List;

@Data
public class CancelBulkOrderResponse {
    private List<Success> success;

    @Data
    public static class Success {
        private String rootOrderId;
    }
}
