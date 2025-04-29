package com.takuro_tamura.autofx.infrastructure.datasource.repository;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.datasource.entity.CandleDataModel;
import com.takuro_tamura.autofx.infrastructure.datasource.mapper.CandleMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CandleRepositoryImpl implements CandleRepository {
    private static final String TABLE_NAME_FORMAT = "candle_%s_%s";

    private final CandleMapper mapper;

    private final Logger log = LoggerFactory.getLogger(CandleRepositoryImpl.class);

    @Override
    public Optional<Candle> findByTime(CurrencyPair pair, TimeFrame timeFrame, LocalDateTime time) {
        final CandleDataModel candle = mapper.findByTime(resolveTableName(pair, timeFrame), time);
        if (candle == null) {
            return Optional.empty();
        }
        return Optional.of(candle.toModel(timeFrame));
    }

    @Override
    public Candle save(Candle candle, TimeFrame timeFrame) {
        final var dataModel = new CandleDataModel(candle);
        mapper.insert(resolveTableName(candle.getCurrencyPair(), timeFrame), dataModel);
        candle.setId(dataModel.getId());
        return candle;
    }

    @Override
    public Candle update(Candle candle, TimeFrame duration) {
        final var dataModel = new CandleDataModel(candle);
        mapper.update(resolveTableName(candle.getCurrencyPair(), duration), dataModel);
        return candle;
    }

    @Override
    public List<Candle> findAllWithLimit(CurrencyPair pair, TimeFrame timeFrame, int limit) {
        final List<CandleDataModel> candles = mapper.findAllWithLimit(resolveTableName(pair, timeFrame), limit);
        return candles.stream().map(candle -> candle.toModel(timeFrame)).collect(Collectors.toList());
    }

    @Override
    public void truncate(CurrencyPair pair, TimeFrame timeFrame) {
        mapper.truncate(resolveTableName(pair, timeFrame));
    }

    private String resolveTableName(CurrencyPair pair, TimeFrame timeFrame) {
        return String.format(TABLE_NAME_FORMAT, pair.name().toLowerCase(), timeFrame.getLabel());
    }
}
