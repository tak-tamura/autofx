import { FC, useEffect, useRef } from "react";
import { createChart, LineData, LineSeries, UTCTimestamp } from "lightweight-charts";

type RsiPoint = {
  time: UTCTimestamp;
  value: number;
  upper?: number;  // 例えば70
  lower?: number;  // 例えば30
};

type RsiChartProps = {
  data: RsiPoint[];
};

export const RsiChart: FC<RsiChartProps> = ({ data }) => {
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

    const rsiSeries = chart.addSeries(LineSeries, { lineWidth: 1 });
    const rsiData: LineData[] = data.map((p) => ({
      time: p.time as any,
      value: p.value,
    }));
    rsiSeries.setData(rsiData);

    // 70 / 30 ラインを引きたい場合は追加の lineSeries を使う

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
