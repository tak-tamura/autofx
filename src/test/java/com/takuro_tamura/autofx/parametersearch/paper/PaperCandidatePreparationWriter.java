package com.takuro_tamura.autofx.parametersearch.paper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 手動レビュー計画を上書き不可のJSONとして保存する。 */
public class PaperCandidatePreparationWriter {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        // レビュー時に指数表記を読み替えずに済むよう、BigDecimalは通常表記で保存する。
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    public Path write(Path outputDirectory, PaperCandidatePreparationPlan plan) {
        if (outputDirectory == null || plan == null) {
            throw new IllegalArgumentException("Output directory and preparation plan are required");
        }
        if (!plan.datasetId().matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Dataset ID contains unsafe file-name characters");
        }
        final Path output = outputDirectory.resolve(
            plan.datasetId() + "_rank-" + plan.candidateRank() + "_paper_preparation.json"
        );
        try {
            Files.createDirectories(outputDirectory);
            // CREATE_NEWで存在確認と作成を一操作にし、同時実行でも既存計画を上書きさせない。
            try (var stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(stream, plan);
            }
            return output;
        } catch (IOException e) {
            if (Files.exists(output)) {
                throw new IllegalStateException("Paper preparation plan is immutable and already exists: " + output, e);
            }
            throw new IllegalStateException("Failed to write paper preparation plan: " + output, e);
        }
    }
}
