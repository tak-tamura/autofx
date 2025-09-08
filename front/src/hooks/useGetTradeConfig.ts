import { useCallback, useState } from "react";
import { TradeConfigResponse } from "../types/response.type";
import axios, { AxiosError, AxiosResponse } from "axios";
import { useToast } from "@chakra-ui/react";

export const useGetTradeConfig = () => {
    const [tradeConfig, setTradeConfig] = useState<TradeConfigResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const toast = useToast();

    const getTradeConfig = useCallback(() => {
        setLoading(true);
        axios.get<TradeConfigResponse>(
            "/api/v1/trade/config",
            { withCredentials: true }
        ).then((res: AxiosResponse<TradeConfigResponse>) => {
            setTradeConfig(res.data);
        }).catch((e: AxiosError) => {
            toast({
                title: 'エラー',
                description: e.message,
                status: 'error',
                duration: 5000,
                isClosable: true
            });
        }).finally(() => {
            setLoading(false);
        });
    }, []);

    return { getTradeConfig, tradeConfig, loading };
};