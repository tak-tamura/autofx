package com.takuro_tamura.autofx.parametersearch.config;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestRiskParameters;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
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
            ),
            new BacktestRiskParameters(
                positiveInt(properties, "risk.atr-period"),
                positiveDecimal(properties, "risk.stop-multiplier"),
                positiveDecimal(properties, "risk.profit-multiplier")
            ),
            new StrategySearchSpace(
                SearchMode.valueOf(required(properties, "search.mode")),
                positiveInt(properties, "search.max-candidates"),
                new StrategyParameterSet(
                    positiveInt(properties, "search.baseline.ema-short"),
                    positiveInt(properties, "search.baseline.ema-long"),
                    positiveInt(properties, "search.baseline.rsi-period"),
                    positiveInt(properties, "search.baseline.macd-fast"),
                    positiveInt(properties, "search.baseline.macd-slow"),
                    positiveInt(properties, "search.baseline.macd-signal"),
                    positiveInt(properties, "search.baseline.bbands-period"),
                    positiveDecimal(properties, "search.baseline.bbands-multiplier"),
                    positiveInt(properties, "search.baseline.adx-period"),
                    positiveDecimal(properties, "search.baseline.adx-threshold")
                ),
                intList(properties, "search.candidates.ema-short"),
                intList(properties, "search.candidates.ema-long"),
                intList(properties, "search.candidates.rsi-period"),
                intList(properties, "search.candidates.macd-fast"),
                intList(properties, "search.candidates.macd-slow"),
                intList(properties, "search.candidates.macd-signal"),
                intList(properties, "search.candidates.bbands-period"),
                decimalList(properties, "search.candidates.bbands-multiplier"),
                intList(properties, "search.candidates.adx-period"),
                decimalList(properties, "search.candidates.adx-threshold")
            ),
            new CandidateSelectionCriteria(
                positiveInt(properties, "selection.minimum-trades"),
                decimal(properties, "selection.minimum-net-profit"),
                decimal(properties, "selection.minimum-profit-factor"),
                decimal(properties, "selection.minimum-average-r"),
                positiveInt(properties, "selection.maximum-selected-candidates")
            ),
            new WalkForwardCriteria(
                positiveInt(properties, "walk-forward.window-months"),
                positiveInt(properties, "walk-forward.minimum-trades-per-window"),
                decimal(properties, "walk-forward.minimum-profitable-window-rate"),
                decimal(properties, "walk-forward.minimum-positive-average-r-window-rate")
            )
        );
    }

    private static int positiveInt(Properties properties, String key) {
        final int value = Integer.parseInt(required(properties, key));
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be greater than zero");
        }
        return value;
    }

    private static BigDecimal positiveDecimal(Properties properties, String key) {
        final BigDecimal value = decimal(properties, key);
        if (value.signum() == 0) {
            throw new IllegalArgumentException(key + " must be greater than zero");
        }
        return value.stripTrailingZeros();
    }

    private static List<Integer> intList(Properties properties, String key) {
        return values(properties, key).stream()
            .map(value -> {
                final int parsed = Integer.parseInt(value);
                if (parsed <= 0) {
                    throw new IllegalArgumentException(key + " values must be greater than zero");
                }
                return parsed;
            })
            .toList();
    }

    private static List<BigDecimal> decimalList(Properties properties, String key) {
        return values(properties, key).stream()
            .map(value -> {
                final BigDecimal parsed = new BigDecimal(value);
                if (parsed.signum() <= 0) {
                    throw new IllegalArgumentException(key + " values must be greater than zero");
                }
                return parsed.stripTrailingZeros();
            })
            .toList();
    }

    private static List<String> values(Properties properties, String key) {
        return Arrays.stream(required(properties, key).split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
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
