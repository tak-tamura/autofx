# パラメータ探索テスト実行マニュアル

## 1. 目的

このマニュアルでは、GMOコインの公開ローソク足を入力として、パラメータ候補の生成から
In-sample探索、Out-of-sample評価、ウォークフォワード評価、paper運用候補のレビュー資料作成までを
実行する方法を説明します。

この処理はバックテストおよびレビュー資料の生成専用です。GMOコインのPrivate API、
実注文、取引設定DBの更新、取引開始・停止処理は使用しません。

リアルタイムデータを利用した仮想注文・仮想約定を行うPAPERモードは、現時点では未実装です。

## 2. 実行環境

以下が必要です。

- Java 17
- リポジトリに含まれるGradle Wrapper
- GMOコイン公開APIへ接続できるネットワーク環境
- 探索結果とローソク足キャッシュを保存できるディスク容量

GMOコインのAPIキーやSecretは不要です。

コマンドはリポジトリのルートディレクトリで実行してください。

```bash
cd /path/to/autofx
```

## 3. 探索条件の設定

探索条件は次のファイルで管理します。

```text
src/test/resources/parameter-search.properties
```

主な設定グループは次のとおりです。

| グループ | 内容 |
|---|---|
| `market.*` | 通貨ペア、時間足、価格種別、タイムゾーン |
| `period.*` | データセット、In-sample、Out-of-sampleの期間 |
| `execution.*` | 約定タイミング、足内決済、コストの仮定 |
| `risk.*` | ATR期間、ストップ・利益確定倍率 |
| `search.*` | 探索方式、基準値、候補値、候補数上限 |
| `selection.*` | In-sample候補の適格条件と選択上限 |
| `walk-forward.*` | ウォークフォワード期間と合格条件 |

各設定値の詳細は、propertiesファイル内の日本語コメントを参照してください。

### 期間を変更するときの注意

- `dataset-from/to`は、In-sampleとOut-of-sampleの両期間を包含する必要があります。
- In-sampleとOut-of-sampleを重複させないでください。
- GMOコインのAPI日付はJST 6:00を境界として扱われるため、最終足が指定日の翌日早朝になることがあります。
- 形成中の足を探索へ含めないため、`market.exclude-incomplete-candle=true`を維持してください。

### 候補を変更するときの注意

- 基準値は対応する候補一覧にも含めてください。
- `ema-short`は`ema-long`より小さくしてください。
- `macd-fast`は`macd-slow`より小さくしてください。
- 候補数が`search.max-candidates`を超えると、探索開始前に停止します。
- 候補一覧を変更した場合は、候補数や生成順を検証する単体テストの期待値も確認してください。

## 4. 通常の自動テスト

パラメータ探索関連の単体テストは、通常のGradleテストから実行できます。

```bash
./gradlew test \
  -x buildReact \
  -x buildReactApp \
  -x copyReactBuild
```

パラメータ探索関連だけを実行する場合は、次のようにクラスを絞り込みます。

```bash
./gradlew test \
  --tests 'com.takuro_tamura.autofx.parametersearch.*' \
  -x buildReact \
  -x buildReactApp \
  -x copyReactBuild
```

通常の`test`タスクからは、以下の手動実行テストが除外されています。

- GMOコイン公開APIへ接続するテスト
- 長時間のパラメータ探索
- paper候補レビュー計画の生成

したがって、通常テストが外部APIへ接続したり、実注文を送信したりすることはありません。

## 5. パラメータ探索の実行

全探索工程は専用の`parameterSearch`タスクで実行します。

```bash
./gradlew parameterSearch \
  -x buildReact \
  -x buildReactApp \
  -x copyReactBuild
```

このタスクでは、以下を順番に実行します。

1. GMOコイン公開APIまたは検証済みキャッシュからローソク足を読み込む
2. データ期間、重複、欠損、OHLC、形成中の足を検証する
3. In-sample期間で全候補をバックテストする
4. 最低取引数、利益、Profit Factor、平均Rで候補を選別する
5. 固定した候補をOut-of-sample期間で評価する
6. 固定した候補を複数期間に分割してウォークフォワード評価する
7. 最終レビューCSVとpaper候補manifestを生成する

探索中の注文処理にはmockが使用されるため、ブローカーへ注文は送信されません。

### ローソク足キャッシュ

取得したローソク足は次のディレクトリへ保存されます。

```text
build/reports/parameter-search/datasets/
```

市場条件と期間が同じ場合は、APIへ再接続せずキャッシュを使用します。読み込み時にはmetadataと
SHA-256を検証します。ファイルが欠損・破損している場合は、不完全なデータで探索を続行せずエラーになります。

期間、通貨ペア、時間足、価格種別を変更すると、別のデータセットとしてAPIから取得します。

## 6. 探索結果

探索結果は`build/reports/parameter-search/`以下へ出力されます。

| ディレクトリ | 出力内容 |
|---|---|
| `datasets/` | ローソク足CSVとデータセットmetadata |
| `selections/` | In-sample候補の順位・棄却理由・取引台帳 |
| `out-of-sample/` | OOS評価結果と取引台帳 |
| `walk-forward/` | 候補別・期間別の安定性評価と取引台帳 |
| `final/` | 全候補の最終判定CSVとpaper候補manifest |
| `paper-preparation/` | Phase 10の手動レビュー計画 |

最終的に確認する主なファイルは次の2つです。

```text
build/reports/parameter-search/final/<datasetId>_final_review.csv
build/reports/parameter-search/final/<datasetId>_paper_candidates.json
```

`paper_candidates.json`に含まれる候補は、paper運用のレビュー候補です。ライブ取引の許可や
将来の収益性を意味しません。

## 7. 同じ条件で再実行する場合

ローソク足キャッシュは再利用できますが、選定結果や最終レポートは監査性のため上書きされません。
同じdataset IDの成果物がすでに存在する場合、タスクはエラー終了します。

再実行前に、既存成果物を別のレビュー用ディレクトリへ退避してください。ローソク足キャッシュも
削除すると公開APIから再取得されるため、再探索だけが目的の場合は`datasets/`を残してください。

`./gradlew clean`は`build/`以下のローソク足キャッシュと全レポートを削除するため、必要な成果物を
退避してから実行してください。

## 8. paper候補レビュー計画の生成

Phase 9のmanifestから候補を1つ明示的に選び、現在設定との差分を確認する場合は
`preparePaperCandidate`タスクを使用します。

このタスクはPAPER取引を開始するものではなく、DBや取引モードも変更しません。

### 現在設定スナップショット

次のサンプルをコピーし、対象環境で現在使用している値へ変更します。

```text
src/test/resources/paper-current-configuration.example.json
```

例：

```json
{
  "currencyPair": "USD_JPY",
  "timeFrame": "1h",
  "atrPeriod": 14,
  "strategyParameters": {
    "emaShortPeriod": 8,
    "emaLongPeriod": 21,
    "rsiPeriod": 14,
    "macdFastPeriod": 12,
    "macdSlowPeriod": 26,
    "macdSignalPeriod": 9,
    "bBandsPeriod": 20,
    "bBandsMultiplier": 2.0,
    "adxPeriod": 14,
    "adxThreshold": 20.0
  }
}
```

これは比較用の読み取り専用ファイルです。記載した値がDBへ反映されることはありません。

### 実行コマンド

```bash
./gradlew preparePaperCandidate \
  -Pmanifest=build/reports/parameter-search/final/<datasetId>_paper_candidates.json \
  -PcandidateRank=1 \
  -PcurrentConfiguration=/path/to/paper-current-configuration.json \
  -x buildReact \
  -x buildReactApp \
  -x copyReactBuild
```

出力先を変更する場合は、`-PoutputDirectory`を追加します。

```bash
-PoutputDirectory=build/reports/parameter-search/paper-preparation-review-2
```

以下の場合はfail closedで停止します。

- manifestのschema versionが未対応
- `manualReviewRequired`が`false`
- `liveTradingAllowed`が`true`
- 候補順位が未指定、重複、または存在しない
- manifestと現在設定の通貨ペア・時間足が異なる
- 必須パラメータが欠損または不正
- 同じレビュー計画ファイルがすでに存在する

## 9. 結果レビューの観点

候補を判断するときは、総利益だけでなく次を確認してください。

- In-sampleとOut-of-sampleの取引数
- Profit Factor
- 最大ドローダウン
- 最大連敗数
- 平均R
- ウォークフォワード各期間の利益と平均R
- 取引時間比率
- 取引コストの仮定
- データ欠損数
- 現在設定から変更されるパラメータ

スプレッド、スリッページ、手数料がゼロの場合は、それらを考慮した実運用成績ではありません。

## 10. 主なエラーへの対応

### `Candle timestamp is outside requested dataset period`

GMOコインのAPI日付境界とJSTのローソク足時刻を確認してください。API日付の翌日早朝の足が含まれること自体は
あり得ますが、設定期間から明らかに外れるデータは受け入れないでください。

### キャッシュのSHA-256検証エラー

キャッシュのCSVまたはmetadataが変更・破損しています。既存ファイルを監査用に退避したうえで、
データセットを再取得してください。

### `immutable and already exists`

同じdataset IDまたは同じ候補順位の成果物がすでに存在します。既存結果を上書きせず、退避するか
別の`outputDirectory`を指定してください。

### `Required Gradle property is missing`

`preparePaperCandidate`の`manifest`、`candidateRank`、`currentConfiguration`をすべて指定してください。

## 11. 安全上の制約

- 探索結果を取引設定DBへ自動反映しないでください。
- `paper_candidates.json`をライブ取引許可として扱わないでください。
- パラメータ探索タスクから取引開始APIを呼び出さないでください。
- バックテストのATR倍率と稼働設定の固定率ストップを同じ値として扱わないでください。
- PAPERモードが実装されるまでは、リアルタイム仮想取引が行われるとは解釈しないでください。
- ライブ取引へ進む場合は、PAPERモード、約定モデル、注文状態、再起動復元、重複防止を別途実装・検証してください。
