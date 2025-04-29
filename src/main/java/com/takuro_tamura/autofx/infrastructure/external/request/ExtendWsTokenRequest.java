package com.takuro_tamura.autofx.infrastructure.external.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtendWsTokenRequest {
    private String token;
}
