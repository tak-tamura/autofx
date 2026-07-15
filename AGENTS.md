ライブラリ
/
FXトレーディングアプリ開発
/
AGENTS.md


# AGENTS.md

## 1. Purpose

This repository contains an FX trading application for developing, backtesting, evaluating, and operating automated trading strategies.

Codex must prioritize, in this order:

1. Preventing unintended live orders and financial loss
2. Preserving the correctness and reproducibility of trading logic
3. Maintaining clear separation between strategy, execution, market data, and UI concerns
4. Making the smallest safe change that satisfies the request
5. Keeping the codebase testable, observable, and maintainable

Do not treat this application as a generic CRUD system. Small changes to timestamps, indicator calculations, position sizing, order state transitions, or configuration defaults can materially change trading results.

---

## 2. Repository discovery

Before editing code:

1. Inspect the repository tree.
2. Read the root `README.md`, build files, environment examples, and any nested `AGENTS.md`.
3. Identify the actual backend and frontend directories from the repository rather than guessing them.
4. Inspect existing package names, class names, scripts, conventions, and tests before proposing changes.
5. Search for all call sites of any class, method, API, event, database field, or configuration key being changed.
6. Report when the repository differs from the architecture described in this document.

Never invent files, packages, classes, commands, endpoints, or environment variables that have not been verified in the repository.

When a task is ambiguous, prefer a conservative implementation based on existing patterns. State any assumption that can affect trading behavior.

---

## 3. Current system context

The application is expected to include the following technologies and concepts. Verify them against the repository before relying on them.

### Backend

- Java and/or Kotlin
- Spring Boot
- Gradle
- MyBatis and/or JPA/Hibernate
- Redis where appropriate
- REST APIs
- Scheduled jobs
- WebSocket or another push mechanism for real-time updates
- MySQL in production or primary environments
- SQLite may be used for local development or backtesting

### Frontend

- React
- TypeScript or JavaScript
- Trading charts based on `lightweight-charts`
- A management or monitoring UI for prices, signals, positions, orders, executions, and strategy results

### Operations

- Docker
- Deployment to a Sakura VPS running Rocky Linux
- No Kubernetes deployment is currently assumed
- Resource constraints may be approximately 3 vCPU, 2 GB RAM, and 100 GB storage

Do not add Kubernetes manifests, Helm charts, operators, or Kubernetes-specific abstractions unless explicitly requested.

---

## 4. Domain model and boundaries

Keep the following concerns separated.

### Market data

Responsibilities include:

- Receiving or loading price data
- Validating symbol, timeframe, timestamps, and OHLC values
- Detecting missing, duplicated, delayed, or out-of-order candles
- Persisting normalized market data where required

Market-data code must not place orders directly.

### Indicators

Responsibilities include pure or near-pure calculations such as:

- EMA
- RSI
- MACD and MACD histogram
- Bollinger Bands
- ATR
- Other derived values added later

Indicator calculations should be deterministic and reusable by both live trading and backtesting.

Do not duplicate indicator formulas independently in live-trading and backtest code.

### Strategy

Responsibilities include:

- Evaluating completed candles
- Producing explicit trading decisions or signals
- Explaining why a signal was accepted or rejected
- Remaining independent of broker-specific APIs where practical

Strategy code should not perform network calls or mutate broker state directly.

### Risk management

Responsibilities include:

- Position sizing
- Stop-loss and take-profit distances
- Maximum open positions
- Exposure and loss limits
- Weekend and economic-event restrictions
- Validation of order quantities and prices

Risk checks must run before any order is submitted.

### Execution

Responsibilities include:

- Translating approved intents into broker orders
- Handling acknowledgements, fills, partial fills, rejection, cancellation, timeout, and reconciliation
- Creating protective orders such as stop loss and take profit
- Maintaining idempotency and order state

Execution code must not silently reinterpret strategy rules.

### Backtesting

Responsibilities include:

- Replaying historical market data
- Reusing production indicator, strategy, and risk logic whenever feasible
- Explicitly modelling spread, slippage, fees, candle timing, fill assumptions, and data gaps
- Producing reproducible metrics and trade logs

### Presentation

The frontend may display and operate the system, but it must not become the source of truth for:

- Positions
- Order state
- Risk checks
- Indicator values used for trading decisions
- Strategy configuration currently active on the server

---

## 5. Baseline trading strategy

The current baseline strategy for USD/JPY on the one-hour timeframe is understood as follows. Treat these values as configurable strategy parameters, not arbitrary constants scattered through the code.

### Entry basis

- Instrument: USD/JPY
- Timeframe: 1 hour
- Maximum simultaneous positions: 1
- Evaluate entries using a completed candle only
- Primary trigger: EMA 8 / EMA 21 crossover

### Long filter

- EMA 8 crosses above EMA 21 on the completed candle
- RSI is greater than 50
- MACD histogram is greater than 0
- Skip the entry when price is outside or beyond the Bollinger Band `+2σ` condition defined by the implementation

### Short filter

- EMA 8 crosses below EMA 21 on the completed candle
- RSI is less than 50
- MACD histogram is less than 0
- Skip the entry when price is outside or beyond the Bollinger Band `-2σ` condition defined by the implementation

### Exit and protection

- ATR period: 14
- Stop loss: ATR × 1.5
- Take profit: ATR × 3.0
- Intended reward-to-risk ratio: approximately 2.0
- Protective stop-loss and take-profit orders are created after the entry fill, using OCO behavior where supported

### Additional context

- ADX was previously tested at thresholds such as 20 and 15, but it reduced trade frequency and may not be part of the active baseline.
- Pullback-long and rally-short rescue entries have been considered but must not be added implicitly.
- Trading may be paused around important economic indicators.
- Weekend position carry is intended to be avoided.
- Capital increases should not be automated from short-term performance.

Do not change this baseline, re-enable ADX, add rescue entries, modify indicator thresholds, or alter ATR multipliers without an explicit request.

The baseline values above describe the reference strategy, but the active strategy parameters are stored in the database and can be changed from the settings screen. Therefore, seed values and values in an existing environment may legitimately differ from this reference. Do not overwrite active database values merely to make them match this section. When investigating or changing trading behavior, verify and report the parameters actually active in the target environment.

A change to strategy behavior must include tests and a description of its expected effect on:

- Signal frequency
- Win rate
- Average profit and loss
- Drawdown
- Consecutive losses
- Exposure
- Backtest comparability

---

## 6. Candle and time handling

Time handling is safety-critical.

- Use an explicit time zone and preferably store timestamps in UTC.
- Convert to the display or broker time zone only at boundaries.
- Do not use the host machine's implicit default time zone in domain logic.
- A strategy decision must state which candle timestamp it evaluates.
- Never evaluate a still-forming candle unless a feature explicitly requires intrabar behavior.
- Deduplicate candle-close events.
- Make scheduled processing idempotent so retries do not produce duplicate signals or orders.
- Account for market closures, daylight-saving transitions in upstream feeds, delayed data, and missing candles.
- Use `Clock` or an equivalent injectable time source for logic that depends on the current time.

Tests involving time must use fixed instants rather than the real system clock.

---

## 7. Order and position safety

Any code capable of creating, changing, or cancelling an order must follow these rules.

### Live-trading protection

- Default local and test environments to paper trading or a no-op broker.
- Never enable live trading merely because credentials are present.
- Require an explicit environment or configuration flag for live order submission.
- Fail closed when critical risk configuration is missing or invalid.
- Never embed API keys, account identifiers, secrets, or production endpoints in source code.
- Redact secrets and sensitive broker payload fields from logs.
- Do not send a real order during tests.

### Validation before submission

Validate at least:

- Symbol
- Side
- Quantity
- Quantity precision and minimum size
- Entry type and price where applicable
- Stop-loss price
- Take-profit price
- Risk amount
- Maximum-position rule
- Existing pending orders and open positions
- Market/session availability
- Strategy and trading-enabled state
- Duplicate client order ID or signal ID

### Idempotency

- Generate stable identifiers for a signal and its order intent.
- Retries must not create duplicate positions.
- Broker timeouts must be treated as unknown outcomes until reconciled.
- Persist enough state to recover after process restart.
- Reconcile local state against broker state before assuming an order failed.

### Protective orders

- Do not consider a newly filled position fully protected until stop-loss and take-profit creation is confirmed.
- Explicitly handle failure to create one or both protective orders.
- Raise a high-severity alert when a position is unprotected.
- Do not silently widen a stop loss.
- Changes to OCO sequencing require focused tests for fill and failure races.

### Position sizing

The lot-size calculation must account for stop-loss distance and configured risk. Do not calculate size solely from account balance or a fixed lot value when risk-based sizing is expected.

Use `BigDecimal` for money, price, quantity, P&L, and risk calculations unless an existing well-tested numeric abstraction is already used. Define scale and rounding explicitly.

---

## 8. Backtest integrity

Backtests must be reproducible and must not overstate performance.

- Use the same indicator and strategy implementations as live trading where possible.
- Prevent look-ahead bias.
- Do not use the completed candle's unavailable future values when simulating an entry.
- Define whether entry occurs at candle close, next candle open, or another explicit price.
- Model spread, slippage, fees, and fill assumptions explicitly.
- Record the dataset identity, symbol, timeframe, date range, strategy version, and parameters.
- Keep raw historical data immutable.
- Detect missing and duplicate candles.
- Make random behavior deterministic through a fixed seed.
- Preserve a trade-by-trade ledger, not only aggregate metrics.

At minimum, report:

- Number of trades
- Win rate
- Gross and net profit
- Average win and loss
- Profit factor
- Maximum drawdown
- Maximum consecutive wins and losses
- Reward-to-risk distribution
- Exposure or time in market
- Assumed transaction costs

When comparing two strategy versions, use the same dataset and execution assumptions.

Do not claim that a strategy improved based solely on total profit. Discuss sample size and drawdown as well.

---

## 9. Backend implementation conventions

Follow existing repository conventions first. Where the repository has no established convention:

- Prefer constructor injection.
- Keep controllers thin.
- Put domain decisions in application or domain services, not controllers, repositories, schedulers, or broker clients.
- Use immutable value objects where practical.
- Use explicit enums for side, order type, order status, position status, timeframe, and trading mode.
- Avoid boolean parameters whose meaning is unclear at the call site.
- Validate configuration at application startup.
- Keep external broker DTOs separate from internal domain models.
- Map persistence entities and API DTOs explicitly.
- Define transaction boundaries deliberately.
- Avoid broad `catch (Exception)` blocks unless rethrowing with context or protecting a process boundary.
- Preserve the root cause when wrapping exceptions.
- Do not suppress order-processing errors.
- Use structured logs with stable keys.

Useful log context includes:

- symbol
- timeframe
- candle timestamp
- signal ID
- strategy version
- client order ID
- broker order ID
- position ID
- trading mode
- rejection reason

Do not log every market tick at `INFO` in production.

---

## 10. Database and persistence

- Inspect existing migration tooling before changing the schema.
- Use migrations for schema changes; do not rely on automatic destructive schema generation.
- Make migrations backward-compatible when deployment may overlap application versions.
- Add indexes based on verified query patterns.
- Preserve numeric precision for prices, quantities, and P&L.
- Use UTC timestamps.
- Enforce uniqueness for natural idempotency keys where appropriate.
- Avoid deleting trading history required for audit or reconciliation.
- Do not rewrite historical executions to make calculated results look cleaner.

Schema changes affecting orders, executions, positions, candles, signals, or strategy versions require migration and rollback consideration.

---

## 11. API conventions

- Preserve existing URL and response conventions.
- Separate request/response DTOs from persistence models.
- Validate inputs at the API boundary.
- Return stable machine-readable error codes for operational failures.
- Do not expose broker credentials, raw secret-bearing payloads, or internal stack traces.
- Make mutation endpoints idempotent where repeated requests are plausible.
- Add pagination for unbounded history endpoints.
- Document timestamp zone and numeric units.
- Avoid changing an existing API contract without updating consumers and tests.

Actions that can enable trading, disable safeguards, close positions, or cancel orders require explicit authorization and audit logging.

---

## 12. Frontend implementation conventions

Follow existing frontend structure and package choices.

- Prefer functional React components and hooks if already used.
- Keep API access in a dedicated client layer.
- Keep server state distinct from local UI state.
- Do not recalculate trade-critical indicators independently in the browser unless the result is explicitly display-only.
- Display data freshness, active trading mode, selected account/environment, and connection status prominently.
- Clearly distinguish paper and live trading.
- Require confirmation for destructive or financially consequential actions.
- Disable repeat submission while a mutation is in progress.
- Handle loading, empty, stale, reconnecting, partial-error, and fatal-error states.
- Clean up chart instances, subscriptions, timers, and WebSocket listeners.
- Avoid rendering unbounded candle or trade history.
- Preserve chart performance by updating incrementally rather than recreating the full chart.

For `lightweight-charts`:

- Verify the installed library version before using APIs.
- Normalize timestamps to the format expected by the installed version.
- Keep candle data sorted and deduplicated.
- Avoid mutating arrays held in React state.
- Dispose chart and series resources on unmount.

Do not redesign unrelated screens while implementing a focused feature.

---

## 13. Configuration and secrets

- Use environment variables or the repository's established secret mechanism.
- Keep an `.env.example` or equivalent free of real credentials.
- Document every required configuration key.
- Use explicit profiles or modes such as local, test, paper, and live.
- Prefer safe defaults.
- Validate mutually dependent settings.
- Do not silently fall back from paper to live or from one broker/account to another.
- Treat strategy parameters as versioned configuration when they affect results.

A configuration change must identify whether it changes runtime behavior, historical comparability, or risk.

---

## 14. Testing requirements

Before modifying code, locate and follow the existing test style and commands.

### Unit tests

Add or update unit tests for:

- Indicator edge cases
- Crossover detection
- Completed-candle behavior
- Long and short filters
- Bollinger Band rejection
- ATR-based stop and take-profit calculation
- Risk-based lot sizing
- Rounding and precision
- Maximum-position logic
- Weekend and event restrictions
- Idempotency
- State transitions

### Integration tests

Use integration tests for:

- Database mappings and migrations
- Repository queries
- API validation and error responses
- Scheduler locking or deduplication
- Broker adapter behavior with a fake server
- Order reconciliation
- OCO and protective-order failure paths
- WebSocket or event delivery where relevant

### Regression tests

A trading bug fix should include a regression test reproducing the original failure.

### Safety rule

Tests must not contact a production broker or submit live orders. External APIs must be mocked, stubbed, recorded safely, or run against an explicit sandbox.

---

## 15. Build, test, and verification workflow

Use the commands that actually exist in the repository. Typical commands may include the following, but verify them before execution.

### Backend examples

```bash
./gradlew test
./gradlew check
./gradlew bootRun
```

### Frontend examples

```bash
npm ci
npm test
npm run lint
npm run build
```

Do not add or switch package managers merely for convenience.

For each implementation task:

1. Run the narrowest relevant tests first.
2. Run the affected module's complete test suite.
3. Run lint, static analysis, formatting checks, and builds defined by the repository.
4. Inspect the final diff for unrelated changes.
5. Report commands executed and any command that could not be executed.
6. Do not claim success when tests were skipped or failed.

Do not modify generated files unless the repository requires them to be committed.

---

## 16. Change discipline

- Make focused changes.
- Preserve public behavior unless the task explicitly changes it.
- Do not perform opportunistic large refactors.
- Do not upgrade frameworks, Gradle, Node.js, dependencies, database versions, or chart libraries without explicit need.
- Ask before adding a production dependency when an existing dependency or standard library can solve the problem.
- Avoid formatting unrelated files.
- Do not rename domain concepts casually.
- Update documentation when behavior, configuration, commands, or APIs change.
- Keep commits and diffs reviewable.

When fixing a defect:

1. Identify the root cause.
2. Add a failing regression test where feasible.
3. Make the smallest correct fix.
4. Verify related edge cases.
5. Explain whether historical trades or stored state may be affected.

---

## 17. Observability and operations

Changes to scheduled trading, market-data ingestion, order execution, or reconciliation must include operational visibility.

Prefer metrics or logs for:

- Last successfully processed candle
- Market-data delay
- Duplicate or missing candle count
- Signal count by accepted/rejected reason
- Order submission, rejection, fill, and cancellation
- Unprotected positions
- Local/broker position mismatch
- Scheduler failures
- WebSocket disconnects
- Strategy enabled state
- Paper/live mode
- P&L and drawdown, with clear calculation scope

Alerts should be actionable. Avoid silently retrying forever.

Given the expected VPS resource limits:

- Avoid unbounded caches, queues, and in-memory histories.
- Avoid excessive thread pools.
- Stream or paginate large datasets.
- Measure before adding resource-heavy infrastructure.
- Do not introduce Kafka, Kubernetes, or other major operational systems without an explicit requirement.

---

## 18. Security

- Never commit secrets.
- Do not expose administrative or trading endpoints without authentication and authorization.
- Validate and constrain user-provided symbols, ranges, sort keys, and filenames.
- Use parameterized database access.
- Protect state-changing endpoints against the application's relevant browser and API threats.
- Keep dependencies within versions supported by the repository and investigate known vulnerabilities when changing them.
- Do not weaken TLS validation.
- Do not print full broker requests or responses when they may contain credentials or account data.

Security fixes should minimize behavior changes while preserving auditability.

---

## 19. Review checklist for trading changes

Before considering a trading-related task complete, verify:

- Is the decision based only on data available at that point in time?
- Is the candle complete?
- Is processing idempotent?
- Can a retry create a duplicate order?
- Is the position size based on the actual stop distance?
- Are decimal scale and rounding explicit?
- Can the position become unprotected?
- What happens after timeout, partial fill, restart, or broker rejection?
- Does local state reconcile with broker state?
- Are paper and live modes unmistakably separated?
- Are strategy parameters recorded?
- Are backtest assumptions unchanged or clearly documented?
- Are both long and short paths tested?
- Are logs sufficient to reconstruct the decision?
- Could the change increase financial risk or trade frequency unexpectedly?

If any answer is uncertain, call it out rather than assuming safety.

---

## 20. Expected Codex response format

For implementation tasks, conclude with:

### Summary

A concise description of what changed.

### Files changed

List the important files and their roles.

### Verification

List the commands and tests actually run, with outcomes.

### Trading impact

State one of:

- No trading behavior change
- Trading behavior changed as requested
- Trading impact uncertain and requires review

When behavior changes, describe the affected signals, orders, risk calculations, or backtest results.

### Risks and follow-up

List only material unresolved risks, assumptions, migrations, or operational steps.

Do not claim that profitability is guaranteed or that a backtest predicts future performance.
