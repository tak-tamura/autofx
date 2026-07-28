package com.takuro_tamura.autofx.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class RestExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowingController())
            .setControllerAdvice(new RestExceptionHandler())
            .build();
    }

    @Test
    void returnsCommonErrorResponseWhenExceptionOccurs() throws Exception {
        mockMvc.perform(get("/test/error"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value(500))
            .andExpect(jsonPath("$.errorMessage").value("Internal Server Error"))
            .andExpect(jsonPath("$.statusCode").doesNotExist())
            .andExpect(jsonPath("$.statusMessage").doesNotExist());
    }

    @Test
    void returnsBadRequestWhenRequestBodyValidationFails() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value(400))
            .andExpect(jsonPath("$.errorMessage").value("Bad Request"));
    }

    @Test
    void returnsBadRequestWhenRequestBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value(400))
            .andExpect(jsonPath("$.errorMessage").value("Bad Request"));
    }

    @Test
    void returnsBadRequestWhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(get("/test/parameter"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value(400))
            .andExpect(jsonPath("$.errorMessage").value("Bad Request"));
    }

    @Test
    void returnsMethodNotAllowedForUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(post("/test/parameter"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value(405))
            .andExpect(jsonPath("$.errorMessage").value("Method Not Allowed"));
    }

    @RestController
    private static class ExceptionThrowingController {

        @GetMapping("/test/error")
        void throwException() {
            throw new RuntimeException("sensitive error details");
        }

        @PostMapping("/test/validation")
        void validateRequest(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/parameter")
        void requireParameter(@RequestParam String value) {
        }
    }

    private record TestRequest(@NotBlank String name) {
    }
}
