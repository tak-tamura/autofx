import {
  Box,
  Button,
  ButtonGroup,
  Checkbox,
  Flex,
  Input,
  Select,
  Spacer,
  VStack,
  HStack,
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
  const { chartData, rsiChartData, macdChartData, profit, getChartData } = useGetChartData();

  const [chartOptions, setChartOptions] = useState<ChartOptions>({
    legend: "none",
    bar: { groupWidth: "100%" },
    backgroundColor: "#F7FAFC",
    candlestick: {
      fallingColor: { strokeWidth: 0, fill: "#a52714" },
      risingColor: { strokeWidth: 0, fill: "#0f9d58" },
    },
    series: {},
  });

  const [rsiChartOptions, setRsiChartOptions] = useState<RsiChartOptions>({
    hAxis: {
      slantedText: false,
    },
    legend: {
      position: "none",
    },
    backgroundColor: "#F7FAFC",
    series: {
      0: { color: "black", lineWidth: 1 },
      1: { color: "#e2431e" },
      2: { color: "black", lineWidth: 1 },
    },
  });

  const [macdChartOptions, setMacdChartOptions] = useState<MacdChartOptions>({
    legend: {
      position: "none",
    },
    seriesType: "bars",
    backgroundColor: "#F7FAFC",
    series: {
      1: { type: "line", lineWidth: 1 },
      2: { type: "line", lineWidth: 1 },
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
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
    }
    if (enableEma) {
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
    }
    if (enableBBands) {
      chartOptions.series[++indicatorIndex] = { type: "line", color: "blue", lineWidth: 1 };
      chartOptions.series[++indicatorIndex] = { type: "line", color: "blue", lineWidth: 1 };
      chartOptions.series[++indicatorIndex] = { type: "line", color: "blue", lineWidth: 1 };
    }
    if (enableIchimoku) {
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
      chartOptions.series[++indicatorIndex] = { type: "line" };
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
    <Flex w="100%" h="100vh" p={4} bg="gray.100" gap={4}>
      {/* 左パネル: 設定 */}
      <Box w="300px" bg="white" p={4} borderRadius="md" shadow="md" overflowY="auto">
        <VStack align="stretch" spacing={4}>
          {/* 通貨ペア */}
          <Box>
            <Text fontWeight="bold">通貨ペア</Text>
            <Select onChange={(e) => setCurrencyPair(e.target.value)}>
              <option value="USD_JPY">USD JPY</option>
              <option value="EUR_JPY">EUR JPY</option>
            </Select>
          </Box>

          {/* 時間足 */}
          <Box>
            <Text fontWeight="bold">時間足</Text>
            <ButtonGroup size="sm" isAttached variant="outline">
              {["1m", "15m", "1h", "4h", "1d", "1w"].map((frame) => (
                <Button key={frame} onClick={() => setTimeFrame(frame)}>{frame}</Button>
              ))}
            </ButtonGroup>
          </Box>

          {/* ロウソク足数 */}
          <Box>
            <Text fontWeight="bold">ローソク足数</Text>
            <Input type="number" value={limit} onChange={(e) => setLimit(Number(e.target.value))} />
          </Box>

          {/* SMA設定 */}
          <Box>
            <Checkbox onChange={(e) => setEnableSma(e.target.checked)}>SMA</Checkbox>
            <HStack>
              <Input placeholder="期間1" width="80px" value={smaPeriod1} onChange={(e) => setSmaPeriod1(Number(e.target.value))} />
              <Input placeholder="期間2" width="80px" value={smaPeriod2} onChange={(e) => setSmaPeriod2(Number(e.target.value))} />
              <Input placeholder="期間3" width="80px" value={smaPeriod3} onChange={(e) => setSmaPeriod3(Number(e.target.value))} />
            </HStack>
          </Box>

          {/* EMA設定 */}
          <Box>
            <Checkbox onChange={(e) => setEnableEma(e.target.checked)}>EMA</Checkbox>
            <HStack>
              <Input placeholder="期間1" width="80px" value={emaPeriod1} onChange={(e) => setEmaPeriod1(Number(e.target.value))} />
              <Input placeholder="期間2" width="80px" value={emaPeriod2} onChange={(e) => setEmaPeriod2(Number(e.target.value))} />
              <Input placeholder="期間3" width="80px" value={emaPeriod3} onChange={(e) => setEmaPeriod3(Number(e.target.value))} />
            </HStack>
          </Box>

          {/* BBands設定 */}
          <Box>
            <Checkbox onChange={(e) => setEnableBBands(e.target.checked)}>BBands</Checkbox>
            <HStack>
              <Input placeholder="K" width="80px" value={bbandsK} onChange={(e) => setBbandsK(Number(e.target.value))} />
              <Input placeholder="N" width="80px" value={bbandsN} onChange={(e) => setBbandsN(Number(e.target.value))} />
            </HStack>
          </Box>

          {/* Ichimoku */}
          <Checkbox onChange={(e) => setEnableIchimoku(e.target.checked)}>Ichimoku</Checkbox>

          {/* Order */}
          <Checkbox onChange={(e) => setIncludeOrder(e.target.checked)}>Order</Checkbox>
          {includeOrder && <Text fontSize="sm">¥{profit}</Text>}

          {/* RSI */}
          <Checkbox onChange={(e) => setEnableRsi(e.target.checked)}>RSI</Checkbox>

          {/* MACD */}
          <Checkbox onChange={(e) => setEnableMacd(e.target.checked)}>MACD</Checkbox>
          {enableMacd && (
            <HStack>
              <Input placeholder="Fast" width="80px" value={inFastPeriod} onChange={(e) => setInFastPeriod(Number(e.target.value))} />
              <Input placeholder="Slow" width="80px" value={inSlowPeriod} onChange={(e) => setInSlowPeriod(Number(e.target.value))} />
              <Input placeholder="Signal" width="80px" value={inSignalPeriod} onChange={(e) => setInSignalPeriod(Number(e.target.value))} />
            </HStack>
          )}
        </VStack>
      </Box>

      {/* 右パネル: チャート */}
      <Box flex="1" bg="white" p={4} borderRadius="md" shadow="md">
        <VStack spacing={4}>
          {/* メインチャート */}
          <Chart
            chartType="CandlestickChart"
            width="100%"
            height="600px"
            data={chartData}
            options={chartOptions}
          />

          {/* RSI */}
          {enableRsi && rsiChartData.length > 0 && (
            <Chart
              chartType="LineChart"
              width="100%"
              height="200px"
              data={rsiChartData}
              options={rsiChartOptions}
            />
          )}

          {/* MACD */}
          {enableMacd && macdChartData.length > 0 && (
            <Chart
              chartType="ComboChart"
              width="100%"
              height="200px"
              data={macdChartData}
              options={macdChartOptions}
            />
          )}
        </VStack>
      </Box>
    </Flex>
  );
};