package com.takuro_tamura.autofx.application.calculator;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.infrastructure.external.response.Assets;
import com.takuro_tamura.autofx.infrastructure.external.response.Ticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 取引可能金額と取引数量を計算するサービス
 * 現在の価格、資産残高、設定パラメータから最適な取引数量を算出
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAmountCalculationService {

    private final PublicApi publicApi;
    private final PrivateApi privateApi;
    private final TradeConfigParameterService tradeConfigParameterService;

    /**
     * 取引する通貨の数量を計算する
     * 
     * @param side 取引種別（BUY/SELL）
     * @param targetPair 対象通貨ペア
     * @return 取引数量
     */
    public int calculateOrderAmount(OrderSide side, CurrencyPair targetPair) {
        // 現在の価格を取得
        final Ticker ticker = publicApi.getTickers().stream()
            .filter(it -> it.getSymbol() == targetPair)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("cannot find ticker of " + targetPair.name()));

        // 資産残高を取得
        final Assets assets = privateApi.getAssets();
        log.info("Available asset amount: {}", assets.getAvailableAmount());

        // 取引可能金額 = 取引に使用する資産の割合 * 資産残高 * レバレッジ
        final BigDecimal availableAmount = tradeConfigParameterService.getAvailableBalanceRate()
            .multiply(BigDecimal.valueOf(assets.getAvailableAmount()))
            .multiply(tradeConfigParameterService.getLeverage());

        // 1通貨あたりの金額にAPIコストを加算
        final BigDecimal price = (side == OrderSide.BUY) 
            ? new BigDecimal(ticker.getAsk()) 
            : new BigDecimal(ticker.getBid());
        final BigDecimal priceWithApiCost = price.add(tradeConfigParameterService.getApiCost());

        // 取引可能金額 / 1通貨あたりの金額が取引数量
        final int orderAmount = availableAmount.divide(priceWithApiCost, RoundingMode.HALF_DOWN).intValue();
        log.info("Calculated order amount: {}", orderAmount);
        
        return orderAmount;
    }
}
