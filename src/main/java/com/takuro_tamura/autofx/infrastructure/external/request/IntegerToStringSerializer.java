package com.takuro_tamura.autofx.infrastructure.external.request;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class IntegerToStringSerializer extends JsonSerializer<Integer> {
    @Override
    public void serialize(Integer value, JsonGenerator gen, SerializerProvider seralizers) throws IOException {
        gen.writeString(value != null ? value.toString() : null);
    }
}
