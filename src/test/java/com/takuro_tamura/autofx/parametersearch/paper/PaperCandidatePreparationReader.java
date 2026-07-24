package com.takuro_tamura.autofx.parametersearch.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Phase 10の入力JSONを読み取り、欠損や形式不正を文脈付きの例外として報告する。 */
public class PaperCandidatePreparationReader {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public PaperCandidateManifest readManifest(Path path) {
        return read(path, PaperCandidateManifest.class, "paper candidate manifest");
    }

    public PaperCurrentConfiguration readCurrentConfiguration(Path path) {
        return read(path, PaperCurrentConfiguration.class, "current configuration snapshot");
    }

    private <T> T read(Path path, Class<T> type, String description) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Readable " + description + " file is required: " + path);
        }
        try {
            return objectMapper.readValue(path.toFile(), type);
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Failed to read " + description + ": " + path, e);
        }
    }
}
