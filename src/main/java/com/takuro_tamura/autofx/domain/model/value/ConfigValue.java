package com.takuro_tamura.autofx.domain.model.value;

public record ConfigValue(String value) {
    public Integer valueAsInt() {
        return Integer.parseInt(value);
    }

    public Boolean valueAsBoolean() {
        return Boolean.parseBoolean(value);
    }

    public Double valueAsDouble() {
        return Double.parseDouble(value);
    }
}
