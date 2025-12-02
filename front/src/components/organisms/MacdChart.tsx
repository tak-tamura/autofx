// components/MacdChart.tsx
import { FC, useEffect, useRef } from "react";
import { createChart, HistogramData, HistogramSeries, LineData, LineSeries, UTCTimestamp } from "lightweight-charts";

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
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 200,
      layout: { background: { color: "#F7FAFC" }, textColor: "#333" },
      rightPriceScale: { borderColor: "#ccc" },
      timeScale: { borderColor: "#ccc" },
    });

    const histSeries = chart.addSeries(HistogramSeries, {});
    const macdSeries = chart.addSeries(LineSeries,{ lineWidth: 1 });
    const signalSeries = chart.addSeries(LineSeries, { lineWidth: 1 });

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

    const handleResize = () => {
      if (!containerRef.current) return;
      chart.applyOptions({ width: containerRef.current.clientWidth });
    };
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      chart.remove();
    };
  }, [data]);

  return <div ref={containerRef} style={{ width: "100%", height: "200px" }} />;
};
