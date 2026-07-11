package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.chart.GetChartCommand;
import com.takuro_tamura.autofx.application.command.chart.MacdParam;
import com.takuro_tamura.autofx.domain.indicator.*;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.indicator.AdxCalculator;
import com.takuro_tamura.autofx.presentation.controller.response.CandleDto;
import com.takuro_tamura.autofx.presentation.controller.response.ChartResponse;
import com.takuro_tamura.autofx.presentation.controller.response.factory.IndicatorRecordFactory;
import com.takuro_tamura.autofx.presentation.controller.response.factory.OrderRecordFactory;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartApplicationService {
    private final Logger log = LoggerFactory.getLogger(ChartApplicationService.class);
    private final BackTestService backTestService;
    private final CandleService candleService;
    private final CandleRepository candleRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final TradeConfigParameterService tradeConfigParameterService;
    private final IndicatorRecordFactory indicatorRecordFactory;
    private final OrderRecordFactory orderRecordFactory;

    public ChartResponse getChart(GetChartCommand command) {
        //log.info("getChart called, param: {}", command);
        final List<Candle> candles = candleRepository.findAllWithLimit(
            command.getCurrencyPair(),
            command.getTimeFrame(),
            command.getLimit()
        );

        final var builder = ChartResponse.builder()
            .currencyPair(command.getCurrencyPair())
            .candles(candles.stream().map(CandleDto::new).collect(Collectors.toList()));

        addIndicators(builder, command, candles);

        if (command.isIncludeOrder()) {
            final List<Order> orders;
            if (tradeConfigParameterService.isBackTest()) {
                orders = backTestService.backTest(command.getCurrencyPair(), command.getTimeFrame(), command.getLimit());
            } else {
                if (CollectionUtils.isNotEmpty(candles)) {
                    orders = orderRepository.findByCurrencyPairAfterTime(
                        command.getCurrencyPair(),
                        candles.get(0).getTime()
                    );
                } else {
                    orders = Collections.emptyList();
                }
            }

            builder.orders(orders.stream()
                .map(order -> orderRecordFactory.createOrderRecord(order, command.getTimeFrame()))
                .collect(Collectors.toList()));
            builder.profit(orderService.accumulateProfit(orders).doubleValue());
        }

        return builder.build();
    }

    private void addIndicators(
        ChartResponse.ChartResponseBuilder builder,
        GetChartCommand command,
        List<Candle> candles
    ) {
        final double[] closePrices = candleService.extractClosePrices(candles);

        if (command.isSmaEnabled()) {
            builder.smas(indicatorRecordFactory.createMaRecords(
                new Sma(command.getSma().getPeriods(), closePrices)
            ));
        }

        if (command.isEmaEnabled()) {
            builder.emas(indicatorRecordFactory.createMaRecords(
                new Ema(command.getEma().getPeriods(), closePrices)
            ));
        }

        if (command.isBBandsEnabled()) {
            builder.bbands(indicatorRecordFactory.createBBandsRecord(
                new BBands(
                    command.getBbands().getN(),
                    command.getBbands().getK(),
                    closePrices
                )
            ));
        }

        if (command.isIchimokuEnabled()) {
            builder.ichimoku(indicatorRecordFactory.createIchimokuRecord(
                new IchimokuCloud(closePrices)
            ));
        }

        if (command.isRsiEnabled()) {
            builder.rsi(indicatorRecordFactory.createRsiRecord(
                new Rsi(command.getRsi().getPeriod(), closePrices)
            ));
        }

        if (command.isMacdEnabled()) {
            final MacdParam param = command.getMacd();
            builder.macd(indicatorRecordFactory.createMacdRecord(
                new Macd(
                    param.getInFastPeriod(),
                    param.getInSlowPeriod(),
                    param.getInSignalPeriod(),
                    closePrices
                )
            ));
        }

        if (command.isAdxEnabled()) {
            final List<Double> adx = Arrays.stream(AdxCalculator.calculateAdx(candles, command.getAdx().getPeriod()))
                .boxed()
                .collect(Collectors.toList());
            builder.adx(adx);
        }
    }
}
