package com.takuro_tamura.autofx.domain.service.port;

import java.util.Optional;
import java.math.BigDecimal;

/**
 * ビジネスロジック層が決済注文ID のマッピング保存に期待するインターフェース（ポート）
 */
public interface OrderCachePort {
    
    /**
     * 決済注文ID と元注文ID の対応を保存
     * @param closeOrderId ブローカーから返された決済注文ID
     * @param originalOrderId 元の注文ID
     */
    void mapCloseOrderToOriginalOrder(Long closeOrderId, Long originalOrderId);
    
    /**
     * 決済注文IDから元注文IDを取得
     * @param closeOrderId 決済注文ID
     * @return 元注文ID（存在しない場合は空）
     */
    Optional<Long> getOriginalOrderId(Long closeOrderId);
    
    /**
     * マッピングを削除
     * @param closeOrderId 決済注文ID
     */
    void removeMapping(Long closeOrderId);

    void saveEntryAtr(Long orderId, BigDecimal atr);

    Optional<BigDecimal> getEntryAtr(Long orderId);

    void removeEntryAtr(Long orderId);
}
