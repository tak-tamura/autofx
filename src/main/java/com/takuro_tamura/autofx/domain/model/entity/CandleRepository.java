package com.takuro_tamura.autofx.domain.model.entity;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleRepository {
    Optional<Candle> findByTime(CurrencyPair pair, TimeFrame timeFrame, LocalDateTime time);

    Candle save(Candle candle, TimeFrame timeFrame);

    Candle update(Candle candle, TimeFrame timeFrame);

    List<Candle> findAllWithLimit(CurrencyPair pair, TimeFrame timeFrame, int limit);

    void truncate(CurrencyPair pair, TimeFrame timeFrame);
}
