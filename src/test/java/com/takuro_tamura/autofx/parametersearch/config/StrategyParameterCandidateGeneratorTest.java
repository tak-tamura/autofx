package com.takuro_tamura.autofx.parametersearch.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StrategyParameterCandidateGeneratorTest {
    private final StrategyParameterCandidateGenerator generator = new StrategyParameterCandidateGenerator();

    @Test
    void generatesBaselineAndOneFactorCandidatesInStableOrder() {
        final StrategySearchSpace searchSpace = ParameterSearchSpecificationLoader
            .load("parameter-search.properties")
            .strategySearchSpace();

        final List<StrategyParameterSet> candidates = generator.generate(searchSpace);

        assertThat(searchSpace.candidateCount()).isEqualTo(39);
        assertThat(candidates).hasSize(39).doesNotHaveDuplicates();
        assertThat(candidates.get(0)).isEqualTo(searchSpace.baseline());
        assertThat(candidates).allSatisfy(candidate -> {
            assertThat(candidate.emaShortPeriod()).isLessThan(candidate.emaLongPeriod());
            assertThat(candidate.macdFastPeriod()).isLessThan(candidate.macdSlowPeriod());
        });
        assertThat(candidates).allSatisfy(candidate ->
            assertThat(differenceCount(searchSpace.baseline(), candidate)).isLessThanOrEqualTo(1)
        );
    }

    @Test
    void rejectsCandidateCountAboveConfiguredLimit() {
        final StrategySearchSpace original = ParameterSearchSpecificationLoader
            .load("parameter-search.properties")
            .strategySearchSpace();
        final StrategySearchSpace limited = copyWithLimit(original, 38);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> generator.generate(limited))
            .withMessageContaining("39")
            .withMessageContaining("38");
    }

    @Test
    void rejectsInvalidDependentPeriods() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StrategyParameterSet(
            21, 21, 14, 12, 26, 9, 20, new BigDecimal("2.0"), 14, new BigDecimal("20")
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> new StrategyParameterSet(
            8, 21, 14, 26, 26, 9, 20, new BigDecimal("2.0"), 14, new BigDecimal("20")
        ));
    }

    private int differenceCount(StrategyParameterSet baseline, StrategyParameterSet candidate) {
        int differences = 0;
        differences += baseline.emaShortPeriod() == candidate.emaShortPeriod() ? 0 : 1;
        differences += baseline.emaLongPeriod() == candidate.emaLongPeriod() ? 0 : 1;
        differences += baseline.rsiPeriod() == candidate.rsiPeriod() ? 0 : 1;
        differences += baseline.macdFastPeriod() == candidate.macdFastPeriod() ? 0 : 1;
        differences += baseline.macdSlowPeriod() == candidate.macdSlowPeriod() ? 0 : 1;
        differences += baseline.macdSignalPeriod() == candidate.macdSignalPeriod() ? 0 : 1;
        differences += baseline.bBandsPeriod() == candidate.bBandsPeriod() ? 0 : 1;
        differences += baseline.bBandsMultiplier().equals(candidate.bBandsMultiplier()) ? 0 : 1;
        differences += baseline.adxPeriod() == candidate.adxPeriod() ? 0 : 1;
        differences += baseline.adxThreshold().equals(candidate.adxThreshold()) ? 0 : 1;
        return differences;
    }

    private StrategySearchSpace copyWithLimit(StrategySearchSpace source, int limit) {
        return new StrategySearchSpace(
            source.mode(), limit, source.baseline(), source.emaShortPeriods(), source.emaLongPeriods(),
            source.rsiPeriods(), source.macdFastPeriods(), source.macdSlowPeriods(), source.macdSignalPeriods(),
            source.bBandsPeriods(), source.bBandsMultipliers(), source.adxPeriods(), source.adxThresholds()
        );
    }
}
