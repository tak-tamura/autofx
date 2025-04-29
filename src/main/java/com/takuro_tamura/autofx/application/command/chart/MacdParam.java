package com.takuro_tamura.autofx.application.command.chart;

import lombok.Data;

@Data
public class MacdParam {
    private boolean enable;
    private int inFastPeriod;
    private int inSlowPeriod;
    private int inSignalPeriod;
}
