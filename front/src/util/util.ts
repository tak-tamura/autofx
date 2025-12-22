export const fmtJst = (tSec: number) => {
  const d = new Date(tSec * 1000);
  // ここでAsia/Tokyo指定で確実にJST表示（PCのTZに依存しない）
  const parts = new Intl.DateTimeFormat("ja-JP", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).formatToParts(d);

  const get = (type: string) => parts.find(p => p.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")} ${get("hour")}:${get("minute")}`;
};