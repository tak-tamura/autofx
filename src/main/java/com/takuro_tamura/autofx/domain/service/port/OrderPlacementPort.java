package com.takuro_tamura.autofx.domain.service.port;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;

import java.util.List;

/**
 * ビジネスロジック層が注文ブローカーに期待するインターフェース（ポート）
 * 具体的なAPI実装には依存しない
 */
public interface OrderPlacementPort {
    
    /**
     * 新規注文をブローカーに送信
     * @param currencyPair 通貨ペア
     * @param side 売買方向
     * @param size ロットサイズ
     * @return ブローカーが返却した注文ID
     * @throws OrderPlacementException 注文送信に失敗
     */
    Long submitMarketOrder(CurrencyPair currencyPair, OrderSide side, int size);
    
    /**
     * 決済注文（成行）をブローカーに送信
     * @param order 決済対象の注文
     * @return ブローカーが返却した決済注文ID
     * @throws OrderPlacementException 決済注文送信に失敗
     */
    Long submitMarketCloseOrder(Order order);
    
    /**
     * OCO注文（ストップロス + リミット）をブローカーに送信
     * @param order 決済対象の注文
     * @param stopPrice ストップロス価格
     * @param limitPrice リミット（利益確定）価格
     * @return ブローカーが返却した決済注文IDs（通常は2つ）
     * @throws OrderPlacementException 注文送信に失敗
     */
    List<Long> submitOcoOrder(Order order, double stopPrice, double limitPrice);
}
