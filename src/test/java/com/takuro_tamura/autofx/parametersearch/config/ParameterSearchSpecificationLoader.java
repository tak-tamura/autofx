package com.takuro_tamura.autofx.parametersearch.config;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Properties;

public final class ParameterSearchSpecificationLoader {
    private ParameterSearchSpecificationLoader() {
    }

    public static ParameterSearchSpecification load(String classpathResource) {
        final Properties properties = new Properties();
        try (InputStream input = ParameterSearchSpecificationLoader.class
            .getClassLoader()
            .getResourceAsStream(classpathResource)) {
            if (input == null) {
                throw new IllegalArgumentException("Parameter-search resource was not found: " + classpathResource);
            }
            // Properties.load(InputStream)のISO-8859-1既定値を使わず、日本語コメントを含む設定をUTF-8で読む。
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load parameter-search resource: " + classpathResource, e);
        }

        return new ParameterSearchSpecification(
            new ParameterSearchSpecification.MarketDataConditions(
                CurrencyPair.valueOf(required(properties, "market.currency-pair")),
                TimeFrame.fromLabel(required(properties, "market.time-frame")),
                MarketPriceType.valueOf(required(properties, "market.price-type")),
                ZoneId.of(required(properties, "market.time-zone")),
                Boolean.parseBoolean(required(properties, "market.exclude-incomplete-candle"))
            ),
            new ParameterSearchSpecification.EvaluationPeriods(
                date(properties, "period.dataset-from"),
                date(properties, "period.dataset-to"),
                date(properties, "period.in-sample-from"),
                date(properties, "period.in-sample-to"),
                date(properties, "period.out-of-sample-from"),
                date(properties, "period.out-of-sample-to")
            ),
            new BacktestAssumptions(
                required(properties, "execution.entry-timing"),
                required(properties, "execution.intrabar-exit-model"),
                required(properties, "execution.both-touched-policy"),
                required(properties, "execution.final-position-policy"),
                decimal(properties, "execution.spread"),
                decimal(properties, "execution.slippage"),
                decimal(properties, "execution.commission")
            )
        );
    }

    private static LocalDate date(Properties properties, String key) {
        return LocalDate.parse(required(properties, key));
    }

    private static BigDecimal decimal(Properties properties, String key) {
        final BigDecimal value = new BigDecimal(required(properties, key));
        if (value.signum() < 0) {
            throw new IllegalArgumentException(key + " must not be negative");
        }
        return value;
    }

    private static String required(Properties properties, String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter-search property is missing: " + key);
        }
        return value.trim();
    }
}
