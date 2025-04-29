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
import com.takuro_tamura.autofx.infrastructure.config.TradeProperties;
import com.takuro_tamura.autofx.presentation.controller.response.CandleDto;
import com.takuro_tamura.autofx.presentation.controller.response.ChartResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final TradeProperties tradeProperties;

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
            if (tradeProperties.isBackTest()) {
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

            builder.orders(orders.stream().map(order -> order.toRecord(command.getTimeFrame())).collect(Collectors.toList()));
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
            builder.smas(new Sma(command.getSma().getPeriods(), closePrices).toRecords());
        }

        if (command.isEmaEnabled()) {
            builder.emas(new Ema(command.getEma().getPeriods(), closePrices).toRecords());
        }

        if (command.isBBandsEnabled()) {
            builder.bbands(new BBands(
                command.getBbands().getN(),
                command.getBbands().getK(),
                closePrices
            ).toRecord());
        }

        if (command.isIchimokuEnabled()) {
            builder.ichimoku(new IchimokuCloud(closePrices).toRecord());
        }

        if (command.isRsiEnabled()) {
            builder.rsi(new Rsi(command.getRsi().getPeriod(), closePrices).toRecord());
        }

        if (command.isMacdEnabled()) {
            final MacdParam param = command.getMacd();
            builder.macd(new Macd(
                param.getInFastPeriod(),
                param.getInSlowPeriod(),
                param.getInSignalPeriod(),
                closePrices
            ).toRecord());
        }
    }
}
