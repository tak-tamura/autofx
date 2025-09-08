import { useToast } from "@chakra-ui/react";
import axios, { AxiosError } from "axios";
import { useCallback } from "react";
import { TradeConfigUpdateRequest } from "../types/requests";

export const useUpdateTradeConfig = () => {
    const updateTradeConfig = useCallback(async (request: TradeConfigUpdateRequest): Promise<boolean> => {
        try {
            await axios.post<TradeConfigUpdateRequest>(
                "/api/v1/trade/config",
                request,
                { withCredentials: true }
            );
        } catch (e) {
            return false;
        }
        return true;
    }, []);

    return { updateTradeConfig };
};