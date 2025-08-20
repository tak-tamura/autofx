package com.takuro_tamura.autofx.domain.service.config;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ChartConfigParameterService {
    private final ConfigParameterService configParameterService;

    public List<CurrencyPair> getTargetCurrencyPairs() {
        final String pairs = configParameterService.getString("CHART.COLLECT_TARGET_PAIRS", "USD_JPY");
        return Stream.of(pairs.split(","))
                .map(value -> CurrencyPair.valueOf(value.trim().toUpperCase()))
                .toList();
    }

    public List<TimeFrame> getTargetTimeFrames() {
        final String timeFrames = configParameterService.getString("CHART.COLLECT_TARGET_TIME_FRAMES", "1m,15m,1h,4h,1d,1w");
        return Stream.of(timeFrames.split(","))
                .map(value -> TimeFrame.fromLabel(value.trim().toLowerCase()))
                .toList();
    }
}
