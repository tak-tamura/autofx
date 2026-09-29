import { useState } from "react";

export type OrderHistoryExportFormat = "csv" | "json";

type OrderHistoryExportParams = {
  startDate: string;
  endDate: string;
};

const getFilename = (
  contentDisposition: string | null,
  params: OrderHistoryExportParams,
  format: OrderHistoryExportFormat
): string => {
  const matched = contentDisposition?.match(/filename="([^"\\/]+)"/i);
  return (
    matched?.[1] ??
    `order-history_${params.startDate}_to_${params.endDate}.${format}`
  );
};

export const useOrderHistoryExport = () => {
  const [exportingFormat, setExportingFormat] =
    useState<OrderHistoryExportFormat | null>(null);
  const [exportError, setExportError] = useState<Error | null>(null);

  const download = async (
    params: OrderHistoryExportParams,
    format: OrderHistoryExportFormat
  ) => {
    setExportingFormat(format);
    setExportError(null);

    try {
      const query = new URLSearchParams({
        startDate: params.startDate,
        endDate: params.endDate,
        format,
      });
      const response = await fetch(
        `/api/v1/order/history/export?${query.toString()}`
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const blobUrl = URL.createObjectURL(await response.blob());
      const anchor = document.createElement("a");
      anchor.href = blobUrl;
      anchor.download = getFilename(
        response.headers.get("Content-Disposition"),
        params,
        format
      );
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(blobUrl);
    } catch (error) {
      setExportError(
        error instanceof Error ? error : new Error("ダウンロードに失敗しました")
      );
    } finally {
      setExportingFormat(null);
    }
  };

  return { download, exportingFormat, exportError };
};
