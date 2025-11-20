import { useEffect, useState } from "react";

export type Order = {
  orderId: number;
  currencyPair: string;
  side: "BUY" | "SELL";
  size: number;
  status: string;
  fillDatetime: string;
  fillPrice: number;
  closeDatetime: string | null;
  closePrice: number | null;
};

export type OrderHistoryResponse = {
  totalElements: number;
  totalPages: number;
  orders: Order[];
  profit: number;
};

export type OrderHistoryParams = {
  page: number;
  size: number;
  startDate: string; // "YYYY-MM-DD"
  endDate: string;   // "YYYY-MM-DD"
};

export const useOrderHistory = (params: OrderHistoryParams) => {
  const [data, setData] = useState<OrderHistoryResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    const fetchOrders = async () => {
      setLoading(true);
      setError(null);

      try {
        const qs = new URLSearchParams({
          page: params.page.toString(),
          size: params.size.toString(),
          startDate: params.startDate,
          endDate: params.endDate,
        });

        // 開発環境で CRA や Vite の dev server を使う場合は
        // package.json の proxy 設定を使って "/api" を 8080 に向ける想定
        const res = await fetch(`/api/v1/order/history?${qs.toString()}`, {
          signal: controller.signal,
        });

        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }

        const json: OrderHistoryResponse = await res.json();
        setData(json);
      } catch (e: any) {
        if (e.name === "AbortError") return;
        setError(e);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();

    return () => controller.abort();
  }, [params.page, params.size, params.startDate, params.endDate]);

  return { data, loading, error };
};
