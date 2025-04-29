import { Candle } from "./candle";

export type ChartApiResponse = {
    productCode: string;
    candles: Array<Candle>;
    indicator: Indicator;
    orders?: Array<Order>;
    profit: number;
};

export type Indicator = {
    smas?: Array<MA>;
    emas?: Array<MA>;
    bbands?: BBands;
    ichimoku?: Ichimoku;
    rsi?: Rsi;
    macd?: Macd;
};

export type MA = {
    period: number;
    values: Array<number>;
};

export type BBands = {
    n: number;
    k: number;
    up: Array<number>;
    mid: Array<number>;
    down: Array<number>;
};

export type Ichimoku = {
    tenkan: Array<number>;
    kijun: Array<number>;
    senkouA: Array<number>;
    senkouB: Array<number>;
    chikou: Array<number>;
};

export type Rsi = {
    period: number;
    values: Array<number>;
};

export type Macd = {
    fastPeriod: number;
    slowPeriod: number;
    signalPeriod: number;
    macd: Array<number>;
    macdSignal: Array<number>;
    macdHist: Array<number>;
};

export type Order = {
    orderId: number;
    currencyPair: string;
    orderSide: "BUY" | "SELL";
    quantity: number;
    orderPrice: number;
    status: "WAITING" |  "FILLED" | "CLOSED" | "CANCELED";
    fillDatetime: string;
    fillPrice: number;
    closeDatetime?: string;
    closePrice?: number;
    //notes?: string;
};