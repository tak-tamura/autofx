# AutoFX

Java 17 / Spring Boot と React / TypeScript で構成されたFXトレーディングアプリケーションです。ローソク足の収集、インジケーター表示、バックテスト、取引設定、注文履歴、外部FX APIを使った注文処理を実装しています。

## 安全上の注意

このアプリケーションには実際のブローカーAPIへ注文を送信するコードが含まれています。

- 現在の実装には、ローカル環境を自動的にペーパートレードやno-opブローカーへ切り替えるプロファイルがありません。
- 取引有効状態がRedisに存在しない場合、取引は有効として扱われます。
- アプリケーション起動時に価格取得ループとプライベートWebSocket接続が開始されます。
- ローカル確認やテストに本番用のAPIキーを使用しないでください。
- APIキーを設定して起動する前に、DBの取引設定、Redisの取引状態、対象口座を必ず確認してください。

DB統合テストではTestcontainers上のMySQLを使用します。テスト実行時にも本番用のAPIキーを環境へ設定しないでください。

## 技術構成

### バックエンド

- Java 17
- Spring Boot 3.3.5
- Gradle
- MyBatis
- MySQL
- Redis
- Spring Security
- WebSocket
- Testcontainers

### フロントエンド

- React 18
- TypeScript
- Chakra UI
- `lightweight-charts`
- Create React App

フロントエンドは `front/` ディレクトリにあります。

## ディレクトリ構成

```text
.
├── build.gradle
├── front/                         # Reactフロントエンド
├── src/main/java/                 # Spring Bootアプリケーション
├── src/main/resources/
│   ├── application.yml            # DB、Redisなどのアプリ設定
│   ├── fxapi.yml                   # 外部FX APIのエンドポイントと認証情報参照
│   └── docker-compose/             # MySQL、Redis、DDL、seed
└── src/test/java/                 # Unit / Integration tests
```

## 設定

### アプリケーション設定

接続先は次のファイルで定義されています。

- `src/main/resources/application.yml`: MySQL、Redis、Spring設定
- `src/main/resources/fxapi.yml`: パブリックAPI、プライベートAPI、WebSocket API

プライベートAPIは次の環境変数を参照します。

```bash
export API_KEY="your-api-key"
export API_SECRET="your-api-secret"
```

実際の認証情報をソースコード、README、`.env` ファイルへコミットしないでください。

### 取引設定

取引設定はYAMLファイルではなく、MySQLの `config_parameter` テーブルに保存されます。新規DBの初期値はFlywayマイグレーション `src/main/resources/db/migration/V2__initial_config_parameters.sql` にあります。

ここに記載する基準戦略と、新規DBのseedまたは設定画面で変更された実際の設定値は異なる場合があります。稼働中の取引条件を確認するときは、対象環境の `config_parameter` を参照してください。

設定はフロントエンドの取引設定画面、または次のAPIから取得・更新されます。

```text
GET  /api/v1/trade/config
POST /api/v1/trade/config
```

主な設定キーは次のとおりです。

| キー | 用途 |
|---|---|
| `TRADE.TARGET_PAIR` | 取引対象通貨ペア。例: `USD_JPY` |
| `TRADE.BACK_TEST` | バックテスト表示の切り替え |
| `TRADE.TARGET_TIME_FRAME` | 対象時間足。例: `1m`, `15m`, `1h`, `4h`, `1d`, `1w` |
| `TRADE.MAX_CANDLE_NUM` | 評価対象の最大ローソク足数 |
| `TRADE.BUY_POINT_THRESHOLD` | 買いシグナルの閾値 |
| `TRADE.SELL_POINT_THRESHOLD` | 売りシグナルの閾値 |
| `TRADE.AVAILABLE_BALANCE_RATE` | 注文数量計算に使用する資産割合 |
| `TRADE.LEVERAGE` | 注文数量計算に使用するレバレッジ |
| `TRADE.API_COST` | 旧数量計算との互換用設定。リスク基準の数量計算では使用しない |
| `TRADE.RISK_PER_TRADE_RATE` | 口座資産に対する1取引の最大許容損失率 |
| `TRADE.MAX_ORDER_QUANTITY` | 1回の新規注文の数量上限 |
| `TRADE.MAX_SPREAD` | 新規注文を許可する最大スプレッド |
| `TRADE.STOP_LIMIT` | ストップ価格計算に使用するATR倍率 |
| `TRADE.PROFIT_LIMIT` | 利益確定価格計算に使用するATR倍率 |
| `TRADE.ATR_PERIOD` | ATR期間 |
| `TRADE.EMA_PERIOD1` | 短期EMA期間 |
| `TRADE.EMA_PERIOD2` | 長期EMA期間 |
| `TRADE.BBANDS_N` | Bollinger Bands期間 |
| `TRADE.BBANDS_K` | Bollinger Bands標準偏差倍率 |
| `TRADE.RSI_PERIOD` | RSI期間 |
| `TRADE.MACD_FAST_PERIOD` | MACD短期EMA期間 |
| `TRADE.MACD_SLOW_PERIOD` | MACD長期EMA期間 |
| `TRADE.MACD_SIGNAL_PERIOD` | MACDシグナル期間 |
| `TRADE.ADX_PERIOD` | ADX期間 |
| `TRADE.ADX_THRESHOLD` | ADX閾値 |

設定値を変更すると、ライブ取引結果とバックテスト比較可能性に影響する場合があります。

## 必要な環境

- JDK 17
- Node.jsおよびnpm
- Docker DesktopまたはDocker Engine

Gradle Wrapperを使用するため、Gradle本体の事前インストールは不要です。

## ローカル起動

### 1. MySQLとRedisを起動する

```bash
docker compose -f src/main/resources/docker-compose/docker-compose.yml up -d
```

接続先は現在、次のポートに固定されています。

- MySQL: `localhost:3306`
- Redis: `localhost:16379`

MySQLコンテナはDBを起動するだけで、スキーマと初期データはバックエンド起動時にFlywayが適用します。空のDBでは `V1__initial_schema.sql` と `V2__initial_config_parameters.sql` が順番に実行されます。

### DBを初期状態から再構築する

今回のFlyway導入では既存DBをbaselineせず、空のDBから再構築します。必要なデータをバックアップしたうえでDBボリュームを作り直し、MySQL起動後にバックエンドを起動してください。Flywayが`V1`から未適用マイグレーションを順番に実行します。

誤って既存DBを自動baselineすることを防ぐため、`spring.flyway.baseline-on-migrate`は`false`にしています。以後、DDL/DMLを追加するときは適用済みファイルを変更せず、次のversion番号を持つSQLを `src/main/resources/db/migration/` に追加してください。

### 2. バックエンドを起動する

```bash
./gradlew bootRun
```

Spring Bootはデフォルトの `http://localhost:8080` で起動します。

`bootRun` でもGradleのリソース処理を通じてフロントエンドビルドが実行されます。また、アプリケーション起動直後から価格取得処理が開始されるため、前述の安全上の注意を確認してください。

### 3. フロントエンド開発サーバーを起動する

別のターミナルで実行します。

```bash
cd front
npm install
npm start
```

開発サーバーは `http://localhost:3000` で起動し、APIリクエストを `http://localhost:8080` へプロキシします。React Routerのbasenameは `/app` なので、ログイン画面は次のURLです。

```text
http://localhost:3000/app/login
```

## ビルド

```bash
./gradlew build
```

このコマンドはバックエンドだけでなく、次の処理も実行します。

1. `front/` で `npm install`
2. `npm run build`
3. `front/build` を `src/main/resources/static` へコピー
4. Javaのコンパイルとテスト

## テスト

### バックエンド

```bash
./gradlew test
```

MySQL統合テストはTestcontainersを使用するため、Dockerが必要です。また、現在のGradleタスク構成ではバックエンドテスト時にもフロントエンドビルドが実行されます。

特定のテストだけを実行する例:

```bash
./gradlew test --tests com.takuro_tamura.autofx.domain.service.OrderServiceTest
```

### フロントエンド

```bash
cd front
npm test
```

`npm test` はCreate React Appの対話型watchモードで起動します。

## 補足

- Springのアクティブプロファイルは現在 `dev` に固定されています。
- SPAの `/app/**` を `index.html` へ転送するコントローラー処理は現在コメントアウトされています。Spring Bootから配信する場合、ブラウザで深いURLを直接開くと正しく表示されない可能性があります。
- リポジトリには現在、ライセンスファイルがありません。
