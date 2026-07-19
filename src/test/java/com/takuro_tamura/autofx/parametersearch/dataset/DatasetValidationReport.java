package com.takuro_tamura.autofx.parametersearch.dataset;

import java.time.LocalDateTime;

/**
 * 検証済みデータから得た集計値。データセット保存時のメタデータ生成に使用する。
 */
public record DatasetValidationReport(
    int candleCount,
    LocalDateTime firstCandleTime,
    LocalDateTime lastCandleTime,
    long dataGapCount
) {
}
