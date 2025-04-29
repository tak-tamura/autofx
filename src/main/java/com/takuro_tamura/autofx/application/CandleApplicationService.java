package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.CandleImportCommand;
import com.takuro_tamura.autofx.application.command.CandleUpsertCommand;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.infrastructure.external.response.FxStatus;
import com.takuro_tamura.autofx.infrastructure.external.response.Kline;
import org.apache.commons.collections4.queue.PredicatedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class CandleApplicationService {
    private final Logger log = LoggerFactory.getLogger(CandleApplicationService.class);
    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final List<CurrencyPair> collectTargetPairs;

    private final CandleRepository candleRepository;

    private final PublicApi publicApi;

    public CandleApplicationService(
        @Value("${chart.collect-target-pairs}") List<CurrencyPair> collectTargetPairs,
        CandleRepository candleRepository,
        PublicApi publicApi
    ) {
        this.collectTargetPairs = collectTargetPairs;
        this.candleRepository = candleRepository;
        this.publicApi = publicApi;
    }

    public boolean upsertCandle(CandleUpsertCommand command) {
        if (command.getStatus() != FxStatus.OPEN) {
            return false;
        }

        if (!collectTargetPairs.contains(command.getCurrencyPair())) {
            return false;
        }

        final Optional<Candle> current = candleRepository.findByTime(
            command.getCurrencyPair(),
            command.getTimeFrame(),
            command.getTime()
        );

        if (current.isPresent()) {
            updateCandlePrice(current.get(), command);
            return false;
        } else {
            createNewCandle(command);
            return true;
        }
    }

    @Transactional
    public int importCandlesFromKlines(CandleImportCommand command) {
        if (command.isTruncate()) {
            candleRepository.truncate(command.getCurrencyPair(), command.getTimeFrame());
        }

        final LocalDate fromDate = LocalDate.parse(command.getFromDate(), FORMATTER);
        final LocalDate toDate = LocalDate.parse(command.getToDate(), FORMATTER);

        int nImported = 0;
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            final List<Kline> klines = publicApi.getKlines(
                command.getCurrencyPair(),
                command.getTimeFrame(),
                date.format(FORMATTER)
            );

            for (final Kline kline : klines) {
                final LocalDateTime time = kline.getOpenTimeAsLocalDateTime();
                final Candle candle = Candle.builder()
                    .time(time)
                    .currencyPair(command.getCurrencyPair())
                    .timeFrame(command.getTimeFrame())
                    .open(new Price(kline.getOpen()))
                    .close(new Price(kline.getClose()))
                    .high(new Price(kline.getHigh()))
                    .low(new Price(kline.getLow()))
                    .build();
                try {
                    candleRepository.save(candle, command.getTimeFrame());
                } catch (DuplicateKeyException e) {
                    log.warn("Duplicate entry, skip.");
                }
            }
            nImported += klines.size();
        }

        return nImported;
    }

    private void createNewCandle(CandleUpsertCommand command) {
        final var candle = new Candle(command);
        candleRepository.save(candle, command.getTimeFrame());
    }

    private void updateCandlePrice(Candle candle, CandleUpsertCommand command) {
        if (candle.shouldUpdateHighPrice(command.getPrice())) {
            candle.setHigh(command.getPrice());
        } else if (candle.shouldUpdateLowPrice(command.getPrice())) {
            candle.setLow(command.getPrice());
        }
        candle.setClose(command.getPrice());
        candleRepository.update(candle, command.getTimeFrame());
    }
}
