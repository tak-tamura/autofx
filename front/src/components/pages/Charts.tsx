import { 
    Box, 
    Button, 
    ButtonGroup, 
    Checkbox, 
    Flex, 
    Input, 
    Select, 
    Spacer, 
    Stack, 
    Text,
} from "@chakra-ui/react";
import { FC, useEffect, useState } from "react";
import { Chart } from "react-google-charts";
import { useGetChartData } from "../../hooks/useGetChartData";
import { GetChartDateRequest } from "../../types/requests";

interface ChartOptions {
    legend: string;
    bar: { groupWidth: string };
    backgroundColor: string;
    candlestick: {
        fallingColor: { strokeWidth: number; fill: string };
        risingColor: { strokeWidth: number; fill: string };
    };
    series: {
        [key: number]: {
            type: string;
            color?: string;
            lineWidth?: number;
            tooltip?: string;
            enableInteractivity?: boolean;
        };
    };
};

interface RsiChartOptions {
    hAxis: {
        slantedText: boolean;
    }
    legend: {
        position: string;
    };
    backgroundColor: string;
    series: {
        [key: number]: {
            type?: string;
            color?: string;
            lineWidth?: number;
        };
    };
};

interface MacdChartOptions {
    legend: {
        position: string;
    };
    backgroundColor: string;
    seriesType: string;
    series: {
        [key: number]: {
            type?: string;
            color?: string;
            lineWidth?: number;
        };
    };
};

export const Charts: FC = () => {
    const {chartData, rsiChartData, macdChartData, profit, getChartData} = useGetChartData();

    const [chartOptions, setChartOptions] = useState<ChartOptions>({
        legend: "none",
        bar: { groupWidth: "100%" },
        backgroundColor: "#F7FAFC",
        candlestick : {
            fallingColor: { strokeWidth: 0, fill: "#a52714" },
            risingColor: { strokeWidth: 0, fill: "#0f9d58" },
        },
        series: {},
    });

    const [rsiChartOptions, setRsiChartOptions] = useState<RsiChartOptions>({
        hAxis: {
            slantedText: false,
        },
        legend : {
            position: "none",
        },
        backgroundColor: "#F7FAFC",
        series: {
            0: {color: "black", lineWidth: 1},
            1: {color: "#e2431e"},
            2: {color: "black", lineWidth: 1},
        },
    });

    const [macdChartOptions, setMacdChartOptions] = useState<MacdChartOptions>({
        legend : {
            position: "none",
        },
        seriesType: "bars",
        backgroundColor: "#F7FAFC",
        series: {
            1: {type: "line", lineWidth: 1},
            2: {type: "line", lineWidth: 1},
        },
    });

    /* ロウソク足チャートのパラメータ */
    const [currencyPair, setCurrencyPair] = useState("USD_JPY");
    const [timeFrame, setTimeFrame] = useState("1m");
    const [limit, setLimit] = useState(365);

    /* SMAのパラメータ */
    const [enableSma, setEnableSma] = useState(false);
    const [smaPeriod1, setSmaPeriod1] = useState(7);
    const [smaPeriod2, setSmaPeriod2] = useState(14);
    const [smaPeriod3, setSmaPeriod3] = useState(50);

    /* EMAのパラメータ */
    const [enableEma, setEnableEma] = useState(false);
    const [emaPeriod1, setEmaPeriod1] = useState(7);
    const [emaPeriod2, setEmaPeriod2] = useState(14);
    const [emaPeriod3, setEmaPeriod3] = useState(50);

    /* BBandsのパラメータ */
    const [enableBBands, setEnableBBands] = useState(false);
    const [bbandsN, setBbandsN] = useState(20);
    const [bbandsK, setBbandsK] = useState(20);

    /* Ichimokuのパラメータ */
    const [enableIchimoku, setEnableIchimoku] = useState(false);

    /* Rsiのパラメータ */
    const [enableRsi, setEnableRsi] = useState(false);
    const [rsiPeriod, setRsiPeriod] = useState(14);

    /* MACDのパラメータ */
    const [enableMacd, setEnableMacd] = useState(false);
    const [inFastPeriod, setInFastPeriod] = useState(12);
    const [inSlowPeriod, setInSlowPeriod] = useState(26);
    const [inSignalPeriod, setInSignalPeriod] = useState(9);

    /* Orderのパラメータ */
    const [includeOrder, setIncludeOrder] = useState(false);

    /* 初回読み込み時とプロパティ変更時にチャートデータを取得 */
    useEffect(() => {
        let indicatorIndex = 0;
        if (enableSma) {
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
        }
        if (enableEma) {
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
        }
        if (enableBBands) {
            chartOptions.series[++indicatorIndex] = {type: "line", color: "blue", lineWidth: 1 };
            chartOptions.series[++indicatorIndex] = {type: "line", color: "blue", lineWidth: 1 };
            chartOptions.series[++indicatorIndex] = {type: "line", color: "blue", lineWidth: 1 };
        }
        if (enableIchimoku) {
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
            chartOptions.series[++indicatorIndex] = {type: "line"};
        }
        if (includeOrder) {
            chartOptions.series[++indicatorIndex] = {
                type: "line", 
                tooltip: "none",
                enableInteractivity: false,
                lineWidth: 0,
            };
        }
        //console.log(chartOptions.series);

        const req: GetChartDateRequest = {
            currencyPair,
            limit,
            timeFrame,
            sma: {
                enable: enableSma,
                periods: [smaPeriod1, smaPeriod2, smaPeriod3],
            },
            ema: {
                enable: enableEma,
                periods: [emaPeriod1, emaPeriod2, emaPeriod3],
            },
            bbands: {
                enable: enableBBands,
                k: bbandsK,
                n: bbandsN,
            },
            ichimoku: {
                enable: enableIchimoku,
            },
            rsi: {
                enable: enableRsi,
                period: rsiPeriod,
            },
            macd: {
                enable: enableMacd,
                inFastPeriod: inFastPeriod,
                inSlowPeriod: inSlowPeriod,
                inSignalPeriod: inSignalPeriod,
            },
            includeOrder: includeOrder,
        };
        const call = () => {
            getChartData(req);
        };
        call();

        const intervalId = setInterval(call, 5000);

        return () => clearInterval(intervalId);
    }, [
        currencyPair, 
        limit, 
        timeFrame, 
        enableSma, 
        smaPeriod1, 
        smaPeriod2, 
        smaPeriod3,
        enableEma, 
        emaPeriod1, 
        emaPeriod2, 
        emaPeriod3,
        enableBBands,
        bbandsK,
        bbandsN,
        enableIchimoku,
        enableRsi,
        rsiPeriod,
        enableMacd,
        inFastPeriod,
        inSlowPeriod,
        inSignalPeriod,
        includeOrder,
    ]);

    return (
        <Box justifyContent="center" bg="blackAlpha.50" w="sm" p={0} borderRadius="md" shadow="md" width="100%" mt={1}>
            <Stack p={2} align="center" justify="center" bg="white">
                <Flex gap={2}>
                    {/* 通貨選択ドロップダウン */}
                    <Box>
                        <Select onChange={(e) => setCurrencyPair(e.target.value)}>
                            <option value="USD_JPY">USD JPY</option>
                            <option value="EUR_JPY">EUR JPY</option>
                        </Select>
                    </Box>
                    <Spacer />
                    {/* Duration選択ボタン */}
                    <ButtonGroup gap="1">
                        <Button colorScheme="teal" onClick={() => setTimeFrame("1m")}>1m</Button>
                        <Button colorScheme="teal" onClick={() => setTimeFrame("15m")}>15m</Button>
                        <Button colorScheme="teal" onClick={() => setTimeFrame("1h")}>1h</Button>
                        <Button colorScheme="teal" onClick={() => setTimeFrame("4h")}>4h</Button>
                        <Button colorScheme="teal" onClick={() => setTimeFrame("1d")}>1d</Button>
                        <Button colorScheme="teal" onClick={() => setTimeFrame("1w")}>1w</Button>
                    </ButtonGroup>
                    <Input value={limit} onChange={(e) => setLimit(Number(e.target.value))} width={100}/>
                </Flex>
                <Spacer />
                {/* SMAパラメータ */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setEnableSma(e.target.checked)}>SMA</Checkbox>
                    <Input width={100} value={smaPeriod1} onChange={(e) => setSmaPeriod1(Number(e.target.value))} />
                    <Input width={100} value={smaPeriod2} onChange={(e) => setSmaPeriod2(Number(e.target.value))} />
                    <Input width={100} value={smaPeriod3} onChange={(e) => setSmaPeriod3(Number(e.target.value))} />
                </Flex>
                {/* EMAパラメータ */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setEnableEma(e.target.checked)}>EMA</Checkbox>
                    <Input width={100} value={emaPeriod1} onChange={(e) => setEmaPeriod1(Number(e.target.value))} />
                    <Input width={100} value={emaPeriod2} onChange={(e) => setEmaPeriod2(Number(e.target.value))} />
                    <Input width={100} value={emaPeriod3} onChange={(e) => setEmaPeriod3(Number(e.target.value))} />
                </Flex>
                {/* BBandsパラメータ */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setEnableBBands(e.target.checked)}>BBands</Checkbox>
                    <Input width={100} placeholder="K" value={bbandsK} onChange={(e) => setBbandsK(Number(e.target.value))} />
                    <Input width={100} placeholder="N" value={bbandsN} onChange={(e) => setBbandsN(Number(e.target.value))} />
                </Flex>
                {/* Ichimokuパラメータ */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setEnableIchimoku(e.target.checked)}>Ichimoku</Checkbox>
                </Flex>
                {/* Orderパラメータ */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setIncludeOrder(e.target.checked)}>Order</Checkbox>
                    {includeOrder && <Text>¥{profit}</Text>}
                </Flex>
                {/* ロウソク足チャート */}
                <Chart
                    chartType="CandlestickChart"
                    width="100%"
                    height="600px"
                    data={chartData}
                    options={chartOptions}
                />
                {/* RSIチャート */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setEnableRsi(e.target.checked)}>RSI</Checkbox>
                </Flex>
                {rsiChartData.length && (
                    <Chart
                        chartType="LineChart"
                        width="100%"
                        height="200px"
                        data={rsiChartData}
                        options={rsiChartOptions}
                    />
                )}
                {/* MACDチャート */}
                <Flex gap={2}>
                    <Checkbox onChange={(e) => setEnableMacd(e.target.checked)}>MACD</Checkbox>
                    <Input width={100} placeholder="Fast Period" value={inFastPeriod} onChange={(e) => setInFastPeriod(Number(e.target.value))} />
                    <Input width={100} placeholder="Slow Period" value={inSlowPeriod} onChange={(e) => setInSlowPeriod(Number(e.target.value))} />
                    <Input width={100} placeholder="Signal Period" value={inSignalPeriod} onChange={(e) => setInSignalPeriod(Number(e.target.value))} />
                </Flex>
                {macdChartData.length && (
                    <Chart
                        chartType="ComboChart"
                        width="100%"
                        height="200px"
                        data={macdChartData}
                        options={macdChartOptions}
                    />
                )}
            </Stack>
        </Box>
    );
};