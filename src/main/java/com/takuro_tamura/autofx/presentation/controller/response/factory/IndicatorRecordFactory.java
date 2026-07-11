package com.takuro_tamura.autofx.presentation.controller.response.factory;

import com.takuro_tamura.autofx.domain.indicator.*;
import com.takuro_tamura.autofx.presentation.controller.response.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndicatorRecordFactory {
    
    public List<MaRecord> createMaRecords(Ema ema) {
        final List<MaRecord> result = new ArrayList<>(ema.getPeriods().length);
        for (int i = 0; i < ema.getPeriods().length; i++) {
            result.add(new MaRecord(ema.getPeriods()[i], ema.getValues()[i]));
        }
        return result;
    }
    
    public List<MaRecord> createMaRecords(Sma sma) {
        final List<MaRecord> result = new ArrayList<>(sma.getPeriods().length);
        for (int i = 0; i < sma.getPeriods().length; i++) {
            result.add(new MaRecord(sma.getPeriods()[i], sma.getValues()[i]));
        }
        return result;
    }
    
    public MacdRecord createMacdRecord(Macd macd) {
        return new MacdRecord(
            macd.getFastPeriod(),
            macd.getSlowPeriod(),
            macd.getSignalPeriod(),
            macd.getMacd(),
            macd.getMacdSignal(),
            macd.getMacdHist()
        );
    }
    
    public RsiRecord createRsiRecord(Rsi rsi) {
        return new RsiRecord(rsi.getPeriod(), rsi.getValues());
    }
    
    public BBandsRecord createBBandsRecord(BBands bBands) {
        return new BBandsRecord(
            bBands.getN(),
            bBands.getK(),
            bBands.getUp(),
            bBands.getMid(),
            bBands.getDown()
        );
    }
    
    public IchimokuRecord createIchimokuRecord(IchimokuCloud ichimoku) {
        return new IchimokuRecord(
            ichimoku.getTenkan(),
            ichimoku.getKijun(),
            ichimoku.getSenkouA(),
            ichimoku.getSenkouB(),
            ichimoku.getChikou()
        );
    }
}
