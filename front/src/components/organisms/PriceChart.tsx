import { FC, useEffect, useRef, useState } from "react";
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
  ISeriesApi,
  ISeriesMarkersPluginApi,
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
  const [autoScroll, setAutoScroll] = useState(true);

  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candleSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);

  // インジケータ用seriesを保持
  const smaSeriesRefs = useRef<ISeriesApi<'Line'>[]>([]);
  const emaSeriesRefs = useRef<ISeriesApi<'Line'>[]>([]);
  const bbSeriesRefs = useRef<ISeriesApi<'Line'>[]>([]);
  const ichimokuSeriesRefs = useRef<ISeriesApi<'Line'>[]>([]);

  // マーカー管理
  const orderMarkersRef = useRef<ISeriesMarkersPluginApi<Time> | null>(null);

  // 画面Open時にチャートを初期化
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
      localization: {
        timeFormatter: (time: UTCTimestamp) => {
          const date = new Date(time * 1000);
          const y = date.getFullYear();
          const m = ("0" + (date.getMonth() + 1)).slice(-2);
          const d = ("0" + date.getDate()).slice(-2);
          const hh = ("0" + date.getHours()).slice(-2);
          const mm = ("0" + date.getMinutes()).slice(-2);
          return `${y}-${m}-${d} ${hh}:${mm}`;
        },
      },  
    });
    chartRef.current = chart;

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
    candleSeriesRef.current = candleSeries;

    // SMA, EMA, BBands, Ichimokuラインをあらかじめ作成しておく
    smaSeriesRefs.current = smaConfigs.map((config) => {
      return chart.addSeries(LineSeries, { 
        lineWidth: 1,
        color: config.color,
      });
    });

    emaSeriesRefs.current = emaConfigs.map((config) => {
      return chart.addSeries(LineSeries, { 
        lineWidth: 1,
        color: config.color,
      });
    });
    
    bbSeriesRefs.current = bbConfigs.map((config) => {
      return chart.addSeries(LineSeries, { 
        lineWidth: 1,
        color: config.color,
      });
    });

    ichimokuSeriesRefs.current = ichimokuConfigs.map((config) => {
      return chart.addSeries(LineSeries, { 
        lineWidth: 1,
        color: config.color,
      });
    });

    // マーカー管理オブジェクト
    orderMarkersRef.current = createSeriesMarkers(candleSeries);

    // 初期フィット
    chart.timeScale().fitContent();

    // 今右端を見ているかの判定
    chart.timeScale().subscribeVisibleLogicalRangeChange((range) => {
      if (!range || !candleSeries) return;
      const barsInfo = candleSeries.barsInLogicalRange(range);
      if (!barsInfo) return;

      const isAtRightEnd = (barsInfo.barsAfter ?? 0) < 1;
      setAutoScroll(isAtRightEnd);
    });

    const handleResize = () => {
      if (!containerRef.current) return;
      chart.applyOptions({ width: containerRef.current.clientWidth });
    };
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      chart.remove();
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); 

  // candlesデータ更新時の処理
  useEffect(() => {
    if (!chartRef.current || !candleSeriesRef.current) return;

    const candleData: CandlestickData[] = candles.map((c) => ({
      time: c.time as any, // timeパースは後で調整
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close,
    }));
    candleSeriesRef.current.setData(candleData);

    if (autoScroll) {
      chartRef.current.timeScale().scrollToRealTime();
    }

  }, [candles, autoScroll]);

  // SMA表示切替時の処理
  useEffect(() => {
    if (!chartRef.current || smaSeriesRefs.current.length === 0) return;

    if (!enableSma) {
      smaSeriesRefs.current.forEach((series) => series.setData([]));
      return;
    }

    const [sma1Series, sma2Series, sma3Series] = smaSeriesRefs.current;

    const sma1Data: LineData[] = candles
      .map((c) => (c.sma1 ? { time: c.time as any, value: c.sma1 } : null))
      .filter((p): p is LineData => p !== null);
    const sma2Data: LineData[] = candles
      .map((c) => (c.sma2 ? { time: c.time as any, value: c.sma2 } : null))
      .filter((p): p is LineData => p !== null);
    const sma3Data: LineData[] = candles
      .map((c) => (c.sma3 ? { time: c.time as any, value: c.sma3 } : null))
      .filter((p): p is LineData => p !== null);

    sma1Series.setData(sma1Data);
    sma2Series.setData(sma2Data);
    sma3Series.setData(sma3Data);
  }, [enableSma, candles]);

  // EMA表示切替時の処理
  useEffect(() => {
    if (!chartRef.current || emaSeriesRefs.current.length === 0) return;

    if (!enableEma) {
      emaSeriesRefs.current.forEach((series) => series.setData([]));
      return;
    }

    const [ema1Series, ema2Series, ema3Series] = emaSeriesRefs.current;

    const ema1Data: LineData[] = candles
      .map((c) => (c.ema1 ? { time: c.time as any, value: c.ema1 } : null))
      .filter((p): p is LineData => p !== null);
    const ema2Data: LineData[] = candles
      .map((c) => (c.ema2 ? { time: c.time as any, value: c.ema2 } : null))
      .filter((p): p is LineData => p !== null);
    const ema3Data: LineData[] = candles
      .map((c) => (c.ema3 ? { time: c.time as any, value: c.ema3 } : null))
      .filter((p): p is LineData => p !== null);

    ema1Series.setData(ema1Data);
    ema2Series.setData(ema2Data);
    ema3Series.setData(ema3Data);
  }, [enableEma, candles]);

  // BBands表示切替時の処理
  useEffect(() => {
    if (!chartRef.current || bbSeriesRefs.current.length === 0) return;

    if (!enableBBands) {
      bbSeriesRefs.current.forEach((series) => series.setData([]));
      return;
    }

    const [bbUpperSeries, bbMiddleSeries, bbLowerSeries] = bbSeriesRefs.current;

    const bbUpperData: LineData[] = candles
      .map((c) => (c.bbUp ? { time: c.time as any, value: c.bbUp } : null))
      .filter((p): p is LineData => p !== null);
    const bbMiddleData: LineData[] = candles
      .map((c) => (c.bbMid ? { time: c.time as any, value: c.bbMid } : null))
      .filter((p): p is LineData => p !== null);
    const bbLowerData: LineData[] = candles
      .map((c) => (c.bbDown ? { time: c.time as any, value: c.bbDown } : null))
      .filter((p): p is LineData => p !== null);

    bbUpperSeries.setData(bbUpperData);
    bbMiddleSeries.setData(bbMiddleData);
    bbLowerSeries.setData(bbLowerData);
  }, [enableBBands, candles]);

  // Ichimoku表示切替時の処理
  useEffect(() => {
    if (!chartRef.current || ichimokuSeriesRefs.current.length === 0) return;

    if (!enableIchimoku) {
      ichimokuSeriesRefs.current.forEach((series) => series.setData([]));
      return;
    }

    const [
      tenkanSeries,
      kijunSeries,
      senkouASeries,
      senkouBSeries,
      chikouSeries,
    ] = ichimokuSeriesRefs.current;

    const tenkanData: LineData[] = candles
      .map((c) => (c.tenkan ? { time: c.time as any, value: c.tenkan } : null))
      .filter((p): p is LineData => p !== null);
    const kijunData: LineData[] = candles
      .map((c) => (c.kijun ? { time: c.time as any, value: c.kijun } : null))
      .filter((p): p is LineData => p !== null);
    const senkouAData: LineData[] = candles
      .map((c) => (c.senkouA ? { time: c.time as any, value: c.senkouA } : null))
      .filter((p): p is LineData => p !== null);
    const senkouBData: LineData[] = candles
      .map((c) => (c.senkouB ? { time: c.time as any, value: c.senkouB } : null))
      .filter((p): p is LineData => p !== null);
    const chikouData: LineData[] = candles
      .map((c) => (c.chikou ? { time: c.time as any, value: c.chikou } : null))
      .filter((p): p is LineData => p !== null);

    tenkanSeries.setData(tenkanData);
    kijunSeries.setData(kijunData);
    senkouASeries.setData(senkouAData);
    senkouBSeries.setData(senkouBData);
    chikouSeries.setData(chikouData);
  }, [enableIchimoku, candles]);

  // 注文マーカー表示切替時の処理
  useEffect(() => {
    if (!candleSeriesRef.current || !orderMarkersRef.current) return;

    if (!includeOrder) {
      orderMarkersRef.current.setMarkers([]);
      return;
    }

    const markers: SeriesMarker<Time>[] = candles
      .filter((c) => !!c.eventLabel)
      .map((c) => {
        const label = c.eventLabel ?? "";
        const isBuy = label.startsWith("BUY");
        return {
          time: c.time,
          position: isBuy ? "belowBar" : "aboveBar",
          color: isBuy ? "#0f9d58" : "#a52714",
          shape: isBuy ? "arrowUp" : "arrowDown",
          text: isBuy ? "B" : "S",
          size: 1,
        } as SeriesMarker<Time>;
      });

    markers.sort((a, b) => Number(a.time) - Number(b.time));
    orderMarkersRef.current.setMarkers(markers);
  }, [candles, includeOrder]);

  return <div ref={containerRef} style={{ width: "100%", height: "600px" }} />;
};
