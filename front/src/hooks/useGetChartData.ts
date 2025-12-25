import axios, { AxiosResponse } from "axios";
import { useCallback, useState } from "react";
import { ChartApiResponse } from "../types/response.type";
import { GetChartDateRequest } from "../types/requests";
import { UTCTimestamp } from "lightweight-charts";

type Event = {
    type: "BUY" | "SELL";
    time: string;
    price: number;
};

type PriceChartData = {
    time: UTCTimestamp;
    open: number;
    high: number;
    low: number;
    close: number;
    sma1?: number | null;
    sma2?: number | null;
    sma3?: number | null;
    ema1?: number | null;
    ema2?: number | null;
    ema3?: number | null;
    bbN?: number | null;
    bbK?: number | null;
    bbUp?: number | null;
    bbMid?: number | null;
    bbDown?: number | null;
    tenkan?: number | null;
    kijun?: number | null;
    senkouA?: number | null;
    senkouB?: number | null;
    chikou?: number | null;
    eventLabel?: string | null;
};

type RsiChartData = {
    time: UTCTimestamp;
    value: number;
};

type MacdChartData = {
    time: UTCTimestamp;
    macd: number;
    signal: number;
    hist: number;
};

type AdxChartData = {
    time: UTCTimestamp;
    value: number;
};

export const useGetChartData = () => {
    const [priceChartData, setPriceChartData] = useState<Array<PriceChartData>>([]);

    const [rsiChartData, setRsiChartData] = useState<Array<RsiChartData>>([]);

    const [macdChartData, setMacdChartData] = useState<Array<MacdChartData>>([]);

    const [adxChartData, setAdxChartData] = useState<Array<AdxChartData>>([]);

    const [profit, setProfit] = useState(0);

    const getChartData = useCallback((req: GetChartDateRequest) => {
        //console.log(req);
        axios.post<ChartApiResponse>(
            "/api/chart",
            req,
            {
                withCredentials: true
            }
        ).then((res: AxiosResponse<ChartApiResponse>) => {
            //console.log(res.data);

            const events = new Array<Event>();
            if (res.data.orders && res.data.orders.length > 0) {
                res.data.orders.forEach((order) => {
                    const type1 = order.orderSide;
                    events.push({type: type1, time: order.fillDatetime, price: order.fillPrice});
                    if (order.closeDatetime && order.closePrice) {
                        const type2 = order.orderSide === "BUY" ? "SELL" : "BUY";
                        events.push({type: type2, time: order.closeDatetime, price: order.closePrice});
                    }
                });
            }
            let currentEvent = events.shift();

            const priceDataRows = new Array<PriceChartData>();
            const rsiDataRows = new Array<RsiChartData>();
            const macdDataRows = new Array<MacdChartData>();
            const adxDataRows = new Array<AdxChartData>();

            const candles = res.data.candles;
            for (let i = 0; i < candles.length; i++) {
                const c = candles[i];
                const date = new Date(c.time);
                const utcSec = Math.floor(date.getTime() / 1000) as UTCTimestamp;
                //const jstSec = (utcSec + 9 * 60 * 60) as UTCTimestamp;

                const priceDateRow: PriceChartData = {
                    time: utcSec,
                    open: c.open,
                    high: c.high,
                    low: c.low,
                    close: c.close
                };

                if (res.data.indicator.smas) {
                    priceDateRow.sma1 = res.data.indicator.smas[0].values[i] || null;
                    priceDateRow.sma2 = res.data.indicator.smas[1].values[i] || null;
                    priceDateRow.sma3 = res.data.indicator.smas[2].values[i] || null;
                }

                if (res.data.indicator.emas) {
                    priceDateRow.ema1 = res.data.indicator.emas[0].values[i] || null;
                    priceDateRow.ema2 = res.data.indicator.emas[1].values[i] || null;
                    priceDateRow.ema3 = res.data.indicator.emas[2].values[i] || null;
                }

                if (res.data.indicator.bbands) {
                    priceDateRow.bbN = res.data.indicator.bbands.n;
                    priceDateRow.bbK = res.data.indicator.bbands.k;
                    priceDateRow.bbUp = res.data.indicator.bbands.up[i] || null;
                    priceDateRow.bbMid = res.data.indicator.bbands.mid[i] || null;
                    priceDateRow.bbDown = res.data.indicator.bbands.down[i] || null;
                }

                if (res.data.indicator.ichimoku) {
                    priceDateRow.tenkan = res.data.indicator.ichimoku.tenkan[i] || null;
                    priceDateRow.kijun = res.data.indicator.ichimoku.kijun[i] || null;
                    priceDateRow.senkouA = res.data.indicator.ichimoku.senkouA[i] || null;
                    priceDateRow.senkouB = res.data.indicator.ichimoku.senkouB[i] || null;
                    priceDateRow.chikou = res.data.indicator.ichimoku.chikou[i] || null;
                }

                while (currentEvent) {
                    const eventTime = new Date(currentEvent.time).getTime();
                    const candleTime = new Date(c.time).getTime();
                    if (eventTime < candleTime) {
                        // イベントの時間が現在のローソク足の時間より前の場合、次のローソク足へ進む
                        currentEvent = events.shift();
                        continue;
                    }

                    if (eventTime > candleTime) {
                        // イベントの時間が現在のローソク足の時間より後の場合、ループを抜ける
                        break;
                    }
                    
                    // イベントの時間が現在のローソク足の時間と等しい場合、ラベルを設定
                    priceDateRow.eventLabel = `${currentEvent.type}(${currentEvent.price.toFixed(3)})`;
                    currentEvent = events.shift();
                }

                priceDataRows.push(priceDateRow);

                if (res.data.indicator.rsi) {
                    rsiDataRows.push({
                        time: utcSec,
                        value: res.data.indicator.rsi.values[i] || 0,
                    });
                }

                if (res.data.indicator.macd) {
                    macdDataRows.push({
                        time: utcSec,
                        macd: res.data.indicator.macd.macd[i] || 0,
                        signal: res.data.indicator.macd.macdSignal[i] || 0,
                        hist: res.data.indicator.macd.macdHist[i] || 0,
                    });
                }

                if (res.data.indicator.adx) {
                    adxDataRows.push({
                        time: utcSec,
                        value: res.data.indicator.adx[i] || 0,
                    });
                }
            }
            //console.log(rows);
            //console.log(priceDataRows);
            setPriceChartData(priceDataRows);
            setRsiChartData(rsiDataRows);
            setMacdChartData(macdDataRows);
            setAdxChartData(adxDataRows);
            setProfit(res.data.profit);
        }).catch(e => {
            console.log(e);
            //alert(e);
        });
    }, []);

    return { 
        priceChartData, 
        rsiChartData, 
        macdChartData, 
        adxChartData,
        profit, 
        getChartData, 
    };
};