import React, { useEffect, useMemo, useRef } from "react";
import {
  createChart,
  IChartApi,
  ISeriesApi,
  UTCTimestamp,
  LineStyle,
  CrosshairMode,
  LineSeries,
  AreaSeries,
  Time, 
  TickMarkType,
} from "lightweight-charts";
import { fmtJst } from "../../util/util";

export type LinePoint = { time: UTCTimestamp; value: number };

type Props = {
  adx: LinePoint[];            // ADX(14)など
  height?: number;             // サブパネル高さ
  threshold1?: number;         // 例: 20
  threshold2?: number;         // 例: 25
  onChartReady?: (chart: IChartApi) => void; // 親が同期したいとき用（任意）
};

export const AdxPanelChart: React.FC<Props> = ({
  adx,
  height = 180,
  threshold1 = 20,
  threshold2 = 25,
  onChartReady,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);

  const chartRef = useRef<IChartApi | null>(null);
  const adxSeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  const th1SeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  const th2SeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  const shadeSeriesRef = useRef<ISeriesApi<"Area"> | null>(null);

  // ADX>=threshold1 の区間だけ薄塗りするためのデータ（それ以外は null で途切れ）
  const shadeData = useMemo(() => {
    return adx.map((p) => ({
      time: p.time,
      value: p.value >= threshold1 ? p.value : 0,
    }));
  }, [adx, threshold1]);

  // 閾値ライン用データ（adxのtimeに合わせて水平線を引く）
  const th1Data = useMemo(
    () => adx.map((p) => ({ time: p.time, value: threshold1 })),
    [adx, threshold1]
  );
  const th2Data = useMemo(
    () => adx.map((p) => ({ time: p.time, value: threshold2 })),
    [adx, threshold2]
  );

  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      height,
      //layout: { background: { color: "transparent" }, textColor: "#999" },
      //grid: { vertLines: { visible: false }, horzLines: { visible: false } },
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
      crosshair: { mode: CrosshairMode.Normal },
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
    onChartReady?.(chart);

    // 薄塗り（擬似背景）
    const shade = chart.addSeries(AreaSeries, {
      lineWidth: 1,
      lineColor: "rgba(255,255,255,0)", // 線は見せない
      topColor: "rgba(0, 150, 136, 0.10)",
      bottomColor: "rgba(0, 150, 136, 0.00)",
      priceLineVisible: false,
      lastValueVisible: false,
    });
    shadeSeriesRef.current = shade;

    // ADXライン
    const adxLine = chart.addSeries(LineSeries, {
      lineWidth: 2,
      color: "#7e57c2",
      priceLineVisible: false,
      lastValueVisible: true,
    });
    adxSeriesRef.current = adxLine;

    // 閾値ライン（20/25）
    const th1 = chart.addSeries(LineSeries, {
      color: "rgba(255,255,255,0.35)",
      lineWidth: 1,
      lineStyle: LineStyle.Dotted,
      priceLineVisible: false,
      lastValueVisible: false,
    });
    th1SeriesRef.current = th1;

    const th2 = chart.addSeries(LineSeries, {
      color: "rgba(255,255,255,0.35)",
      lineWidth: 1,
      lineStyle: LineStyle.Dotted,
      priceLineVisible: false,
      lastValueVisible: false,
    });
    th2SeriesRef.current = th2;

    // 初期サイズ合わせ
    chart.timeScale().fitContent();

    // リサイズ追従
    const ro = new ResizeObserver((entries) => {
      for (const e of entries) {
        chart.applyOptions({ width: e.contentRect.width });
      }
    });
    ro.observe(containerRef.current);

    return () => {
      ro.disconnect();
      chart.remove();
      chartRef.current = null;
    };
  }, [height, onChartReady]);

  // データ更新（chart生成後に反映）
  useEffect(() => {
    if (!chartRef.current) return;
    if (!adxSeriesRef.current || !th1SeriesRef.current || !th2SeriesRef.current || !shadeSeriesRef.current) return;

    shadeSeriesRef.current.setData(shadeData as any);
    adxSeriesRef.current.setData(adx as any);
    th1SeriesRef.current.setData(th1Data as any);
    th2SeriesRef.current.setData(th2Data as any);
  }, [adx, shadeData, th1Data, th2Data]);

  return <div ref={containerRef} style={{ width: "100%", height: "200px" }}  />;
};
