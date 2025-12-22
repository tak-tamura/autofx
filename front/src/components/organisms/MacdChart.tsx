// components/MacdChart.tsx
import { FC, useEffect, useRef, useState } from "react";
import { 
  createChart, 
  HistogramData, 
  HistogramSeries, 
  LineData, 
  LineSeries, 
  UTCTimestamp, 
  Time, 
  TickMarkType, 
  IChartApi,
  ISeriesApi
} from "lightweight-charts";
import { fmtJst } from "../../util/util";

type MacdPoint = {
  time: UTCTimestamp;
  macd: number;      // ヒストグラム
  signal: number;    // ライン
  hist: number;      // 必要なら分けて
};

type MacdChartProps = {
  data: MacdPoint[];
};

export const MacdChart: FC<MacdChartProps> = ({ data }) => {
  const [autoScroll, setAutoScroll] = useState(true);

  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const histSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);
  const macdSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
  const signalSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);

  // 画面Open時にチャートを初期化
  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 200,
      layout: { background: { color: "#F7FAFC" }, textColor: "#333" },
      rightPriceScale: { borderColor: "#ccc" },
      timeScale: { 
        borderColor: "#ccc",
        timeVisible: true,
        secondsVisible: false,
        tickMarkFormatter: (time: Time, _type: TickMarkType) => {
          if (typeof time !== "number" && typeof time !== "string") {
            const { year, month, day } = time;
            return `${year}-${String(month).padStart(2,"0")}-${String(day).padStart(2,"0")}`;
          }
          if (typeof time === "number") {
            const d = new Date(time * 1000);
            return new Intl.DateTimeFormat("ja-JP", {
              timeZone: "Asia/Tokyo",
              month: "2-digit",
              day: "2-digit",
              hour: "2-digit",
              minute: "2-digit",
              hour12: false,
            }).format(d);
          }
          return time;
        },
      },
      localization: {
        timeFormatter: (time: Time) => {
          if (typeof time === "number") return fmtJst(time);
          if (typeof time === "string") return time;
          
          const { year, month, day } = time;
          return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
        },
        locale: "ja-JP",
      },
    });
    chartRef.current = chart;

    const histSeries = chart.addSeries(HistogramSeries, {});
    const macdSeries = chart.addSeries(LineSeries,{ lineWidth: 1 });
    const signalSeries = chart.addSeries(LineSeries, { lineWidth: 1 });
    histSeriesRef.current = histSeries;
    macdSeriesRef.current = macdSeries;
    signalSeriesRef.current = signalSeries;

    const histData: HistogramData[] = data.map((p) => ({
      time: p.time as any,
      value: p.hist,
    }));
    const macdData: LineData[] = data.map((p) => ({
      time: p.time as any,
      value: p.macd,
    }));
    const signalData: LineData[] = data.map((p) => ({
      time: p.time as any,
      value: p.signal,
    }));

    histSeries.setData(histData);
    macdSeries.setData(macdData);
    signalSeries.setData(signalData);

    chart.timeScale().fitContent();

    // 今右端を見ているかの判定
    chart.timeScale().subscribeVisibleLogicalRangeChange((range) => {
      if (!range || !histSeriesRef.current || !macdSeriesRef.current || !signalSeriesRef.current) return;
      const barsInfo = macdSeriesRef.current.barsInLogicalRange(range);
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

  // data 更新時にチャートを更新
  useEffect(() => {
    if (!chartRef.current || !histSeriesRef.current || !macdSeriesRef.current || !signalSeriesRef.current) return;
    const histSeries = histSeriesRef.current;
    const macdSeries = macdSeriesRef.current;
    const signalSeries = signalSeriesRef.current;

    const histData: HistogramData[] = data.map((p) => ({
      time: p.time as any,
      value: p.hist,
    }));
    const macdData: LineData[] = data.map((p) => ({
      time: p.time as any,
      value: p.macd,
    }));
    const signalData: LineData[] = data.map((p) => ({
      time: p.time as any,
      value: p.signal,
    }));

    histSeries?.setData(histData);
    macdSeries.setData(macdData);
    signalSeries?.setData(signalData);

    if (autoScroll) {
      chartRef.current.timeScale().scrollToRealTime();
    }
  }, [data, autoScroll]);

  return <div ref={containerRef} style={{ width: "100%", height: "200px" }} />;
};
