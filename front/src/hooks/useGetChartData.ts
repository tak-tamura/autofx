import axios, { AxiosResponse } from "axios";
import { useCallback, useState } from "react";
import { ChartApiResponse } from "../types/response.type";
import { GetChartDateRequest } from "../types/requests";

type Row = Array<Date | number | string | null>;

type Event = {
    type: "BUY" | "SELL";
    time: string;
    price: number;
};

export const useGetChartData = () => {
    const [chartData, setChartData] = useState<Array<Row>>([]);

    const [rsiChartData, setRsiChartData] = useState<Array<Row>>([]);

    const [macdChartData, setMacdChartData] = useState<Array<Row>>([]);

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
            const rows = new Array<Row>();
            rows.push(createHeader(req, res.data));

            const rsiRows = new Array<Row>();
            if (res.data.indicator.rsi) {
                rsiRows.push(createRsiChartHeader(req));
            }

            const macdRows = new Array<Row>();
            if (res.data.indicator.macd) {
                macdRows.push(createMacdChartHeader());
            }

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

            const candles = res.data.candles;
            for (let i = 0; i < candles.length; i++) {
                const c = candles[i];
                const date = new Date(c.time);

                /* ロウソク足チャートのデータを抽出 */
                const row: Row = [date, c.low, c.open, c.close, c.high];
                if (res.data.indicator.smas) {
                    row.push(res.data.indicator.smas[0].values[i] || null);
                    row.push(res.data.indicator.smas[1].values[i] || null);
                    row.push(res.data.indicator.smas[2].values[i] || null);
                }
                if (res.data.indicator.emas) {
                    row.push(res.data.indicator.emas[0].values[i] || null);
                    row.push(res.data.indicator.emas[1].values[i] || null);
                    row.push(res.data.indicator.emas[2].values[i] || null);
                }
                if (res.data.indicator.bbands) {
                    row.push(res.data.indicator.bbands.up[i] || null);
                    row.push(res.data.indicator.bbands.mid[i] || null);
                    row.push(res.data.indicator.bbands.down[i] || null);
                }
                if (res.data.indicator.ichimoku) {
                    row.push(res.data.indicator.ichimoku.tenkan[i] || null);
                    row.push(res.data.indicator.ichimoku.kijun[i] || null);
                    row.push(res.data.indicator.ichimoku.senkouA[i] || null);
                    row.push(res.data.indicator.ichimoku.senkouB[i] || null);
                    row.push(res.data.indicator.ichimoku.chikou[i] || null);
                }
                if (res.data.orders && res.data.orders.length > 0) {
                    if (!currentEvent) {
                        row.push(null);
                        row.push(null);
                    } else if (currentEvent.time === c.time) {
                        row.push(c.high);
                        //row.push(currentEvent.type);
                        row.push(`${currentEvent.type}(${currentEvent.price.toFixed(3)})`);
                        currentEvent = events.shift();
                    } else {
                        //console.log("currentEvent.time:", currentEvent.time, "c.time:", c.time);
                        row.push(null);
                        row.push(null);
                    }
                }
                rows.push(row);

                /* RSIのデータを抽出 */
                if (res.data.indicator.rsi) {
                    const rsiRow: Row = [date];
                    rsiRow.push(70);
                    rsiRow.push(res.data.indicator.rsi.values[i] || null);
                    rsiRow.push(30);
                    rsiRows.push(rsiRow);
                }

                /* MACDのデータを抽出 */
                if (res.data.indicator.macd) {
                    const macdRow: Row = [date];
                    macdRow.push(res.data.indicator.macd.macd[i] || null);
                    macdRow.push(res.data.indicator.macd.macdSignal[i] || null);
                    macdRow.push(res.data.indicator.macd.macdHist[i] || null);
                    macdRows.push(macdRow);
                }
            }
            //console.log(rows);
            setChartData(rows);
            setRsiChartData(rsiRows);
            setMacdChartData(macdRows);
            //console.log(macdChartData);
            setProfit(res.data.profit);
        }).catch(e => {
            console.log(e);
            //alert(e);
        });
    }, []);

    return { chartData, rsiChartData, macdChartData, profit, getChartData, };
};

const createHeader = (req: GetChartDateRequest, res: ChartApiResponse) => {
    const header:any = ["Date", "Low", "Open", "Close", "High"];
    if (req.sma.enable) {
        header.push("SMA1");
        header.push("SMA2");
        header.push("SMA3");
    }
    
    if (req.ema.enable) {
        header.push("EMA1");
        header.push("EMA2");
        header.push("EMA3");
    }
    
    if (req.bbands.enable) {
        header.push(`BBands Up(${req.bbands.n}, ${req.bbands.k})`);
        header.push(`BBands Mid(${req.bbands.n}, ${req.bbands.k})`);
        header.push(`BBands Down(${req.bbands.n}, ${req.bbands.k})`);
    }

    if (req.ichimoku.enable) {
        header.push("Tenkan");
        header.push("Kijun");
        header.push("SenkouA");
        header.push("SenkouB");
        header.push("Chikou");
    }
    if (res.orders && res.orders.length > 0) {
        header.push("Marker");
        header.push({type: "string", role: "annotation"});
    }
    return header;
};

const createRsiChartHeader = (req: GetChartDateRequest) => {
    const header = ["Date"];
    if (req.rsi.enable) {
        header.push("RSI Thread");
        header.push(`RSI(${req.rsi.period})`);
        header.push("RSI Thread");
    }
    return header;
}

const createMacdChartHeader = () => {
    const header = ["Date"];
    header.push("MD");
    header.push("MS");
    header.push("HT");
    return header;
}