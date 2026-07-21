package com.takuro_tamura.autofx.parametersearch.selection;

/**
 * 候補がOut-of-sample評価へ進めない理由。
 * 1候補が複数条件に違反した場合は、該当する理由をすべて記録する。
 */
public enum CandidateRejectionReason {
    /** 取引数が少なく、評価サンプルとして不十分。 */
    INSUFFICIENT_TRADES,
    /** ネット利益が設定された下限を下回る。 */
    NET_PROFIT_BELOW_MINIMUM,
    /** プロフィットファクターが設定された下限を下回る。 */
    PROFIT_FACTOR_BELOW_MINIMUM,
    /** 1取引あたりの平均Rが設定された下限を下回る、または算出不能。 */
    AVERAGE_R_BELOW_MINIMUM
}
