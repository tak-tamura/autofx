package com.takuro_tamura.autofx.domain.model.value;

public enum OrderSide {
    BUY, SELL,
    ;

    public static OrderSide getCloseSide(OrderSide side) {
        switch (side) {
            case BUY -> {
                return SELL;
            }
            case SELL -> {
                return BUY;
            }
        }
        return side;
    }
}
