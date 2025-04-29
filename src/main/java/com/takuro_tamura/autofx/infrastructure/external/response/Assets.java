package com.takuro_tamura.autofx.infrastructure.external.response;

import lombok.Data;

@Data
public class Assets {
    private Double equity;
    private Double availableAmount;
    private Double balance;
    private Double estimatedTradeFee;
    private Integer margin;
    private Double marginRatio;
    private Double positionLossGain;
    private Double totalSwap;
    private Integer transferableAmount;
}
