package com.takuro_tamura.autofx.domain.model.value;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Price implements Comparable<Price> {
    @Getter
    private final BigDecimal value;

    public Price(String value) {
        this.value = new BigDecimal(value);
    }

    public Price(BigDecimal value) {
        this.value = value;
    }

    public Price(double value) {
        this(BigDecimal.valueOf(value));
    }

    public static Price getMidPrice(String bid, String ask) {
        final var bidValue = new BigDecimal(bid);
        final var askValue = new BigDecimal(ask);
        final BigDecimal sum = bidValue.add(askValue);
        final BigDecimal average = sum.divide(new BigDecimal("2"), RoundingMode.HALF_EVEN);
        return new Price(average);
    }

    public Price add(Price other) {
        return new Price(this.value.add(other.value));
    }

    public Price subtract(Price other) {
        return new Price(this.value.subtract(other.value));
    }

    @Override
    public int compareTo(Price other) {
        return this.value.compareTo(other.value);
    }

    public String toString() {
        return this.value.toPlainString();
    }
}
