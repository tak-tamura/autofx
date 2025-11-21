import {
  Box,
  Container,
  Select,
  Spinner,
  VStack,
  Switch,
  FormControl,
  FormLabel,
  FormErrorMessage,
  NumberInput,
  NumberInputField,
  useToast,
  Button
} from "@chakra-ui/react";
import { FC, useEffect } from "react";
import { useGetTradeConfig } from "../../hooks/useGetTradeConfig";
import { Controller, useForm } from "react-hook-form";
import { TradeConfigUpdateRequest } from "../../types/requests";
import { useUpdateTradeConfig } from "../../hooks/useUpdateTradeConfig";

// --- ヘルパー: Chakra の NumberInput を RHF Controller で扱う
const RHFNumberInput: React.FC<{
  name: string;
  control: any;
  rules?: any;
  isInvalid?: boolean;
  error?: string;
  label: string; 
  step?: number;
  min?: number;
  max?: number;
  precision?: number;
  placeholder?: string;
}> = ({ name, control, isInvalid, error, label, step = 1, min, max, precision, placeholder }) => {
  return (
    <FormControl isInvalid={isInvalid}>
      <FormLabel fontWeight="bold">{label}</FormLabel>
      <Controller
        name={name as any}
        control={control}
        render={({ field: { onChange, value } }) => (
          <NumberInput
            value={value ?? ""}
            onChange={(valueStr) => onChange(valueStr)}
            onBlur={() => {
              const n = parseFloat(value);
              if (!Number.isNaN(n)) onChange(n);
            }}
            step={step}
            min={min}
            max={max}
            precision={precision}
          >
            <NumberInputField placeholder={placeholder} />
          </NumberInput>
        )}
      />
      <FormErrorMessage>{error}</FormErrorMessage>
    </FormControl>
  );
};

export const TradeSetting: FC = () => {
  const { getTradeConfig, tradeConfig, loading } = useGetTradeConfig();
  const { updateTradeConfig } = useUpdateTradeConfig();
  const toast = useToast();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
    reset
  } = useForm<TradeConfigUpdateRequest>();

  useEffect(() => {
    getTradeConfig();
  }, [getTradeConfig]);

  useEffect(() => {
    if (!tradeConfig) return;

    if (tradeConfig) {
      reset({
        targetCurrencyPair: tradeConfig.targetCurrencyPair,
        backTest: tradeConfig.backTest,
        targetTimeFrame: tradeConfig.targetTimeFrame,
        maxCandleNum: tradeConfig.maxCandleNum,
        buyPointThreshold: tradeConfig.buyPointThreshold,
        sellPointThreshold: tradeConfig.sellPointThreshold,
        availableBalanceRate: tradeConfig.availableBalanceRate,
        leverage: tradeConfig.leverage,
        apiCost: tradeConfig.apiCost,
        stopLimit: tradeConfig.stopLimit,
        profitLimit: tradeConfig.profitLimit,
        atrPeriod: tradeConfig.atrPeriod,
        emaPeriod1: tradeConfig.emaPeriod1,
        emaPeriod2: tradeConfig.emaPeriod2,
        bBandsN: tradeConfig.bBandsN,
        bBandsK: tradeConfig.bBandsK,
        rsiPeriod: tradeConfig.rsiPeriod,
        macdFastPeriod: tradeConfig.macdFastPeriod,
        macdSlowPeriod: tradeConfig.macdSlowPeriod,
        macdSignalPeriod: tradeConfig.macdSignalPeriod
      });
    }
  }, [tradeConfig, reset]);

  const onSubmit = async (request: TradeConfigUpdateRequest) => {
    const success = await updateTradeConfig(request);
    if (success) {
      toast({
        title: '成功',
        description: 'トレード設定の更新に成功しました',
        status: 'success',
        duration: 5000,
        isClosable: true
      });
      getTradeConfig();
    } else {
      toast({
        title: 'エラー',
        description: 'トレード設定の更新に失敗しました',
        status: 'error',
        duration: 5000,
        isClosable: true
      });
    }
  };

  return (
    <Box w="100%" h="100%" p={4} bg="gray.100">
      <Container as="form" onSubmit={handleSubmit(onSubmit)} bg="white" p={4}>
        {loading ? <Spinner size="sm" /> :
          <VStack spacing={4} align="stretch">
            {/* 通貨ペア */}
            <FormControl isInvalid={!!errors.targetCurrencyPair}>
              <FormLabel fontWeight="bold" mb={2}>通貨ペア</FormLabel>
              <Select {...register("targetCurrencyPair")}>
                <option value="USD_JPY">USD JPY</option>
                <option value="EUR_JPY">EUR JPY</option>
              </Select>
              <FormErrorMessage>{errors.targetCurrencyPair?.message}</FormErrorMessage>
            </FormControl>

            {/* 時間足 */}
            <FormControl isInvalid={!!errors.targetTimeFrame}>
              <FormLabel fontWeight="bold" mb={2}>時間足</FormLabel>
              <Select {...register("targetTimeFrame")}>
                {["1m", "15m", "1h", "4h", "1d", "1w"].map(tf => (
                  <option key={tf} value={tf}>{tf}</option>
                ))}
              </Select>
              <FormErrorMessage>{errors.targetCurrencyPair?.message}</FormErrorMessage>
            </FormControl>

            {/* バックテスト */}
            <FormControl isInvalid={!!errors.backTest}>
              <FormLabel fontWeight="bold" mb={2}>バックテスト</FormLabel>
              <Switch {...register("backTest")} />
            </FormControl>

            {/* ローソク足数 */}
            <RHFNumberInput 
              name="maxCandleNum"
              control={control}
              label="ローソク足数"
              min={1}
              max={10000}
              step={1}
              precision={0}
              isInvalid={!!errors.maxCandleNum}
              error={errors.maxCandleNum?.message}
              placeholder="365"
            />

            {/* Buy Point Threshold */}
            <RHFNumberInput 
              name="buyPointThreshold"
              control={control}
              label="Buy Point Threshold"
              min={1}
              max={10}
              step={1}
              precision={0}
              isInvalid={!!errors.buyPointThreshold}
              error={errors.buyPointThreshold?.message}
              placeholder="1"
            />

            {/* Sell Point Threshold */}
            <RHFNumberInput 
              name="sellPointThreshold"
              control={control}
              label="Sell Point Threshold"
              min={1}
              max={10}
              step={1}
              precision={0}
              isInvalid={!!errors.sellPointThreshold}
              error={errors.sellPointThreshold?.message}
              placeholder="1"
            />

            {/* Available Balance Rate */}
            <RHFNumberInput 
              name="availableBalanceRate"
              control={control}
              label="Available Balance Rate"
              min={0}
              max={1}
              step={0.01}
              precision={2}
              isInvalid={!!errors.availableBalanceRate}
              error={errors.availableBalanceRate?.message}
              placeholder="0.8"
            />

            {/* レバレッジ */}
            <RHFNumberInput 
              name="leverage"
              control={control}
              label="レバレッジ"
              min={1}
              max={25}
              step={1}
              precision={0}
              isInvalid={!!errors.leverage}
              error={errors.leverage?.message}
              placeholder="0.8"
            />

            {/* APIコスト */}
            <RHFNumberInput 
              name="apiCost"
              control={control}
              label="APIコスト"
              min={0}
              max={1}
              step={0.001}
              precision={3}
              isInvalid={!!errors.apiCost}
              error={errors.apiCost?.message}
              placeholder="0.002"
            />

            {/* Stop Limit */}
            <RHFNumberInput 
              name="stopLimit"
              control={control}
              label="Stop Limit"
              min={0}
              max={100}
              step={0.01}
              precision={2}
              isInvalid={!!errors.stopLimit}
              error={errors.stopLimit?.message}
              placeholder="0.10"
            />

            {/* Profit Limit */}
            <RHFNumberInput 
              name="profitLimit"
              control={control}
              label="Profit Limit"
              min={0}
              max={100}
              step={0.01}
              precision={2}
              isInvalid={!!errors.profitLimit}
              error={errors.profitLimit?.message}
              placeholder="0.15"
            />

            {/* ATR Period */}
            <RHFNumberInput 
              name="atrPeriod"
              control={control}
              label="ATR Period"
              min={1}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.atrPeriod}
              error={errors.atrPeriod?.message}
              placeholder="14"
            />

            {/* EMA Period 1 */}
            <RHFNumberInput 
              name="emaPeriod1"
              control={control}
              label="EMA Period1"
              min={1}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.emaPeriod1}
              error={errors.emaPeriod1?.message}
              placeholder="7"
            />

            {/* EMA Period 2 */}
            <RHFNumberInput 
              name="emaPeriod2"
              control={control}
              label="EMA Period2"
              min={1}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.emaPeriod2}
              error={errors.emaPeriod2?.message}
              placeholder="14"
            />

            {/* BBands N */}
            <RHFNumberInput 
              name="bBandsN"
              control={control}
              label="BBands N"
              min={0}
              max={100}
              step={0.1}
              precision={1}
              isInvalid={!!errors.bBandsN}
              error={errors.bBandsN?.message}
              placeholder="2.0"
            />

            {/* BBands K */}
            <RHFNumberInput 
              name="bBandsK"
              control={control}
              label="BBands K"
              min={0}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.bBandsK}
              error={errors.bBandsK?.message}
              placeholder="20"
            />

            {/* RSI */}
            <RHFNumberInput 
              name="rsiPeriod"
              control={control}
              label="RSI Period"
              min={0}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.rsiPeriod}
              error={errors.rsiPeriod?.message}
              placeholder="14"
            />

            {/* MACD Fast Period */}
            <RHFNumberInput 
              name="macdFastPeriod"
              control={control}
              label="MACD Fast Period"
              min={0}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.macdFastPeriod}
              error={errors.macdFastPeriod?.message}
              placeholder="12"
            />

            {/* MACD Slow Period */}
            <RHFNumberInput 
              name="macdSlowPeriod"
              control={control}
              label="MACD Slow Period"
              min={0}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.macdSlowPeriod}
              error={errors.macdSlowPeriod?.message}
              placeholder="26"
            />

            {/* MACD Signal Period */}
            <RHFNumberInput 
              name="macdSignalPeriod"
              control={control}
              label="MACD Signal Period"
              min={0}
              max={100}
              step={1}
              precision={0}
              isInvalid={!!errors.macdSignalPeriod}
              error={errors.macdSignalPeriod?.message}
              placeholder="9"
            />

            <Button type="submit" colorScheme="teal" isLoading={isSubmitting}>保存</Button>
          </VStack>
        }
      </Container>
    </Box>
  );
};