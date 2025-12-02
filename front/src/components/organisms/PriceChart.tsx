import { FC, useEffect, useRef } from "react";
import {
  createChart,
  IChartApi,
  CandlestickData,
  LineData,
  CandlestickSeries,
  LineSeries,
  UTCTimestamp,
  SeriesMarker, 
  Time,
  createSeriesMarkers,
} from "lightweight-charts";

interface Candle {
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

interface PriceChartProps {
  candles: Candle[];
  enableSma: boolean;
  enableEma: boolean;
  enableBBands: boolean;
  enableIchimoku: boolean;
  includeOrder: boolean;
};

const smaConfigs = [
  { key: "sma1" as const, color: "#3182CE" },
  { key: "sma2" as const, color: "#38A169" },
  { key: "sma3" as const, color: "#E53E3E" },
];

const emaConfigs = [
  { key: "ema1" as const, color: "#3182CE" },
  { key: "ema2" as const, color: "#38A169" },
  { key: "ema3" as const, color: "#E53E3E" },
];

const bbConfigs = [
  { key: "bbUpper" as const, color: "#4299E1" },
  { key: "bbMiddle" as const, color: "#A0AEC0" },
  { key: "bbLower" as const, color: "#4299E1" },
];

const ichimokuConfigs = [
  { key: "tenkan" as const, color: "#E53E3E" },
  { key: "kijun" as const, color: "#3182CE" },
  { key: "senkouA" as const, color: "#38A169" },
  { key: "senkouB" as const, color: "#DD6B20" },
  { key: "chikou" as const, color: "#718096" },
];

export const PriceChart: FC<PriceChartProps> = ({
  candles,
  enableSma,
  enableEma,
  enableBBands,
  enableIchimoku,
  includeOrder,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const chart: IChartApi = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 600,
      layout: {
        background: { color: "#F7FAFC" },
        textColor: "#333",
      },
      grid: {
        vertLines: { color: "#eee" },
        horzLines: { color: "#eee" },
      },
      timeScale: {
        borderColor: "#ccc",
        timeVisible: true,
        secondsVisible: false,
      },
      rightPriceScale: {
        borderColor: "#ccc",
      },
    });

    // ローソク足シリーズ
    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: "#0f9d58",
      borderUpColor: "#0f9d58",
      wickUpColor: "#0f9d58",
      downColor: "#a52714",
      borderDownColor: "#a52714",
      wickDownColor: "#a52714",
    });

    const candleData: CandlestickData[] = candles.map((c) => ({
      time: c.time as any, // timeパースは後で調整
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close,
    }));
    candleSeries.setData(candleData);

    // 注文マーカー
    if (includeOrder) {
      const markers: SeriesMarker<Time>[] = candles
        .filter((c) => !!c.eventLabel)
        .map((c) => {
          const label = c.eventLabel ?? "";
          const isBuy = label.startsWith("BUY");

          return {
            time: c.time,
            position: isBuy ? "belowBar" : "aboveBar", // BUYは足の下、SELLは足の上
            color: isBuy ? "#0f9d58" : "#a52714",      // BUY=緑, SELL=赤
            shape: isBuy ? "arrowUp" : "arrowDown",    // 矢印マーカー
            text: isBuy ? "B" : "S",                   // 文字（任意）
            size: 1,                                   // 0〜3くらいで調整
          } as SeriesMarker<Time>;
        });

      const orderMarkers = createSeriesMarkers(candleSeries, markers); // eslint-disable-line
    }

    // SMAなどオーバーレイ用ライン
    const addLine = (
      extract: (c: Candle) => number | undefined | null,
      color: string
    ) => {
      const lineSeries = chart.addSeries(LineSeries, { 
        lineWidth: 1,
        color: color,
      });
      const data: LineData[] = candles
        .map((c) => {
          const v = extract(c);
          if (v === undefined || v === null) {
            return null;
          }
          return {
            time: c.time as any,
            value: v,
          };
        }).filter((p): p is LineData => p !== null);
      lineSeries.setData(data);
      return lineSeries;
    };

    if (enableSma) {
      smaConfigs.forEach((config, index) => {
        addLine((c) => c[config.key], config.color);
      });
    }
    
    if (enableEma) {
      emaConfigs.forEach((config, index) => {
        addLine((c) => c[config.key], config.color);
      }); 
    }

    if (enableBBands) {
      bbConfigs.forEach((config) => {
        addLine((c) => {
          if (config.key === "bbUpper") return c.bbUp;
          if (config.key === "bbMiddle") return c.bbMid;
          if (config.key === "bbLower") return c.bbDown;
          return null;
        }, config.color);
      });
    }

    if (enableIchimoku) {
      ichimokuConfigs.forEach((config) => {
        addLine((c) => c[config.key], config.color);
      });
    }

    // 初期フィット
    chart.timeScale().fitContent();

    const handleResize = () => {
      if (!containerRef.current) return;
      chart.applyOptions({ width: containerRef.current.clientWidth });
    };
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      chart.remove();
    };
  }, [candles, enableSma, enableEma, enableBBands, enableIchimoku, includeOrder]);

  return <div ref={containerRef} style={{ width: "100%", height: "600px" }} />;
};
