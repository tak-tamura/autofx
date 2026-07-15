package com.takuro_tamura.autofx.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AdxEntryFilterTest {

    private final AdxEntryFilter filter = new AdxEntryFilter(20.0);

    @Test
    @DisplayName("評価対象のADXが閾値以上なら、後続のADXが閾値未満でもエントリーを許可する")
    void usesAdxAtEvaluationIndexWhenLaterValueIsBelowThreshold() {
        final double[] adxValues = {0.0, 25.0, 10.0};

        assertThat(filter.canEnter(adxValues, 1)).isTrue();
    }

    @Test
    @DisplayName("評価対象のADXが閾値未満なら、後続のADXが閾値以上でもエントリーを拒否する")
    void doesNotUseLaterAdxValueWhenEvaluationIndexIsBelowThreshold() {
        final double[] adxValues = {0.0, 10.0, 25.0};

        assertThat(filter.canEnter(adxValues, 1)).isFalse();
    }

    @Test
    @DisplayName("評価対象のADXが閾値と等しい場合はエントリーを許可する")
    void allowsEntryWhenAdxEqualsThreshold() {
        assertThat(filter.canEnter(new double[]{20.0}, 0)).isTrue();
    }

    @Test
    @DisplayName("評価対象のADXが非有限値の場合はエントリーを拒否する")
    void rejectsNonFiniteAdx() {
        assertThat(filter.canEnter(new double[]{Double.NaN}, 0)).isFalse();
        assertThat(filter.canEnter(new double[]{Double.POSITIVE_INFINITY}, 0)).isFalse();
        assertThat(filter.canEnter(new double[]{Double.NEGATIVE_INFINITY}, 0)).isFalse();
    }

    @Test
    @DisplayName("ADX配列がnullまたは空の場合は例外を送出する")
    void rejectsMissingAdxValues() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> filter.canEnter(null, 0));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> filter.canEnter(new double[0], 0));
    }

    @Test
    @DisplayName("評価対象indexがADX配列の範囲外の場合は例外を送出する")
    void rejectsIndexOutsideAdxValues() {
        final double[] adxValues = {20.0};

        assertThatIllegalArgumentException()
            .isThrownBy(() -> filter.canEnter(adxValues, -1));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> filter.canEnter(adxValues, adxValues.length));
    }
}
