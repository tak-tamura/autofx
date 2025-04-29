export type GetChartDateRequest = {
    currencyPair: string;
    limit: number;
    timeFrame: string;
    sma: MaParams;
    ema: MaParams;
    bbands: BBandsParams;
    ichimoku: IchimokuParams;
    rsi: RsiParams;
    macd: MacdParams;
    includeOrder: boolean;
};

export type MaParams = {
    enable: boolean;
    periods: Array<number>;
};

export type BBandsParams = {
    enable: boolean;
    n: number;
    k: number;
};

export type IchimokuParams = {
    enable: boolean;
};

export type RsiParams = {
    enable: boolean;
    period: number;
};

export type MacdParams = {
    enable: boolean;
    inFastPeriod: number;
    inSlowPeriod: number;
    inSignalPeriod: number;
};