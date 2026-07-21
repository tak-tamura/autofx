package com.takuro_tamura.autofx.parametersearch.dataset;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 履歴CSVが取得時から変更されていないことを検証するSHA-256計算を共通化する。 */
final class HistoricalDatasetHash {
    private HistoricalDatasetHash() {
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
