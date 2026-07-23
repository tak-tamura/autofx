package com.takuro_tamura.autofx.parametersearch.walkforward;

import java.time.LocalDateTime;

/** OOS期間内の連続した1評価区間。終了時刻は排他的に扱う。 */
public record WalkForwardWindow(
    int index,
    LocalDateTime start,
    LocalDateTime endExclusive
) {
    public WalkForwardWindow {
        if (index <= 0 || start == null || endExclusive == null || !start.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Positive index and valid walk-forward period are required");
        }
    }
}
