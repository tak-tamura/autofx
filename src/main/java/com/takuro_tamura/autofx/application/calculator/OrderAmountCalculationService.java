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

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAmountCalculationService {
    static final int MINIMUM_ORDER_QUANTITY = 10000;
    static final int ORDER_QUANTITY_STEP = 1;

    private final PublicApi publicApi;
    private final PrivateApi privateApi;
    private final TradeConfigParameterService config;

    /**
     * 1取引の想定損失額が設定された許容額を超えないように、新規注文数量を計算する。
     *
     * <p>リスク基準数量は「口座資産 × 1取引リスク率 ÷ (ATR × 損切り倍率 + スプレッド)」で求める。
     * その後、証拠金から求めた数量と設定上限のうち最も小さい値を採用する。</p>
     *
     * @param side 売買区分
     * @param targetPair 対象通貨ペア
     * @param atr シグナル判定時の確定足から計算したATR
     * @return ブローカーの数量制約を満たす注文数量
     * @throws IllegalStateException 計算に必要な値が不正、または安全条件を満たさない場合
     */
    public int calculateOrderAmount(OrderSide side, CurrencyPair targetPair, BigDecimal atr) {
        validatePositive(atr, "ATR");

        // 発注側の価格を使うため、BUYはask、SELLはbidをエントリー価格とする。
        final Ticker ticker = publicApi.getTickers().stream()
            .filter(it -> it.getSymbol() == targetPair)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("cannot find ticker of " + targetPair.name()));
        final BigDecimal ask = parsePositive(ticker.getAsk(), "ask");
        final BigDecimal bid = parsePositive(ticker.getBid(), "bid");
        if (ask.compareTo(bid) < 0) {
            throw new IllegalStateException("ask must not be lower than bid");
        }

        // スプレッドもストップ到達時の想定損失に含める。過大な場合は数量を減らすだけでなく発注自体を拒否する。
        final BigDecimal spread = ask.subtract(bid);
        final BigDecimal maxSpread = config.getMaxSpread();
        validatePositive(maxSpread, "maximum spread");
        if (spread.compareTo(maxSpread) > 0) {
            throw new IllegalStateException("spread exceeds configured maximum");
        }

        // ローカルDBだけでなくブローカーにも照会し、既存建玉がある場合は二重エントリーを防ぐ。
        final Assets assets = privateApi.getAssets();
        final BigDecimal equity = positiveNumber(assets.getEquity(), "equity");
        final BigDecimal availableAmount = positiveNumber(assets.getAvailableAmount(), "available amount");
        if (!privateApi.getOpenPositions(targetPair).getList().isEmpty()) {
            throw new IllegalStateException("broker reports an existing open position for " + targetPair);
        }

        final BigDecimal riskRate = config.getRiskPerTradeRate();
        final BigDecimal stopMultiplier = config.getStopLimit();
        final BigDecimal leverage = config.getLeverage();
        final BigDecimal availableBalanceRate = config.getAvailableBalanceRate();
        validateRate(riskRate, "risk per trade rate");
        if (riskRate.compareTo(new BigDecimal("0.1")) > 0) {
            throw new IllegalStateException("risk per trade rate must not exceed 0.1");
        }
        validatePositive(stopMultiplier, "stop multiplier");
        validatePositive(leverage, "leverage");
        validateRate(availableBalanceRate, "available balance rate");

        // 許容損失額 = 口座資産 × 1取引リスク率
        final BigDecimal riskBudget = equity.multiply(riskRate);
        // 1通貨あたりの想定損失 = ATRベースの損切り距離 + スプレッド
        final BigDecimal lossPerUnit = atr.multiply(stopMultiplier).add(spread);
        validatePositive(lossPerUnit, "loss per unit");
        // 許容損失額を超えないよう、小数部分は必ず切り捨てる。
        final BigDecimal riskQuantity = riskBudget.divide(lossPerUnit, 0, RoundingMode.DOWN);

        // リスク基準で大きな数量を許容できても、利用可能証拠金の範囲を超えてはならない。
        final BigDecimal entryPrice = side == OrderSide.BUY ? ask : bid;
        final BigDecimal marginQuantity = availableAmount
            .multiply(availableBalanceRate)
            .multiply(leverage)
            .divide(entryPrice, 0, RoundingMode.DOWN);
        final int configuredMaximum = config.getMaxOrderQuantity();
        if (configuredMaximum < MINIMUM_ORDER_QUANTITY) {
            throw new IllegalStateException("maximum order quantity is below broker minimum");
        }

        // 3種類の上限で最も安全側の数量を選び、ブローカーの数量ステップに切り捨てで合わせる。
        final BigDecimal capped = riskQuantity.min(marginQuantity).min(BigDecimal.valueOf(configuredMaximum));
        final int quantity = capped.divide(BigDecimal.valueOf(ORDER_QUANTITY_STEP), 0, RoundingMode.DOWN)
            .multiply(BigDecimal.valueOf(ORDER_QUANTITY_STEP))
            .intValueExact();
        if (quantity < MINIMUM_ORDER_QUANTITY) {
            throw new IllegalStateException("risk-based order quantity is below broker minimum");
        }

        log.info("Calculated risk-based order amount: symbol={}, side={}, equity={}, riskBudget={}, atr={}, " +
                "stopDistance={}, spread={}, riskQuantity={}, marginQuantity={}, finalQuantity={}",
            targetPair, side, equity, riskBudget, atr, atr.multiply(stopMultiplier), spread,
            riskQuantity, marginQuantity, quantity);
        return quantity;
    }

    private static BigDecimal parsePositive(String value, String name) {
        try {
            final BigDecimal number = new BigDecimal(value);
            validatePositive(number, name);
            return number;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalStateException(name + " must be a valid number", e);
        }
    }

    private static BigDecimal positiveNumber(Double value, String name) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalStateException(name + " must be a finite number");
        }
        final BigDecimal number = BigDecimal.valueOf(value);
        validatePositive(number, name);
        return number;
    }

    private static void validatePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalStateException(name + " must be greater than zero");
        }
    }

    private static void validateRate(BigDecimal value, String name) {
        validatePositive(value, name);
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException(name + " must not exceed one");
        }
    }
}
