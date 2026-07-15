package com.takuro_tamura.autofx.infrastructure.external.response;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class OpenPositions {
    private List<OpenPosition> list = Collections.emptyList();
}
