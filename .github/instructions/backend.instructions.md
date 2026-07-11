---

## applyTo: "src/**,src/main/java/**,src/test/java/**,**/*.java,**/*.kt,**/application*.yml,**/application*.yaml,**/application*.properties"

# Backend Instructions

## Backend tech stack

Prefer the following backend stack unless the existing code clearly says otherwise:

* Java 17+
* Spring Boot
* Spring Web / REST APIs
* Spring Scheduler where appropriate
* MyBatis or JPA/Hibernate depending on the existing module
* MySQL for production-like persistence
* SQLite may be used for local backtesting or lightweight development if already present
* Gradle or Maven depending on the repository

## Backend coding style

For Spring Boot code:

* Use constructor injection.
* Prefer immutable DTOs or records where appropriate.
* Keep controllers thin.
* Put business logic in service/domain classes.
* Keep strategy calculation logic testable without Spring context when possible.
* Avoid putting trading logic directly in controllers, schedulers, or repositories.
* Use meaningful exception types for domain errors.
* Validate request DTOs.
* Return clear API responses.
* Use `BigDecimal` for money, price, lot size, and financial calculations when precision matters.
* Be careful with timezone handling.
* Prefer `Instant`, `OffsetDateTime`, or `ZonedDateTime` over ambiguous local times for market data and execution timestamps.
* Avoid static utility-heavy designs when domain services or value objects would be clearer.

## Package and layer responsibilities

Keep responsibilities separated.

Preferred layering:

* Controller:

    * HTTP request/response handling only
    * input validation
    * conversion to application service calls
* Application service:

    * orchestration
    * transaction boundary
    * interaction between repositories, broker clients, and domain logic
* Domain logic:

    * strategy decisions
    * indicator calculations
    * risk calculations
    * position and trade rules
* Repository:

    * persistence only
* External client:

    * broker API
    * market data API
    * economic calendar API if introduced

Avoid mixing these responsibilities in a single class.

## Trading strategy implementation

When implementing strategy logic:

* Keep entry rules and exit rules separate.
* Keep indicator calculation separate from signal generation.
* Keep risk calculation separate from signal generation.
* Strategy logic should be deterministic for the same input candles and configuration.
* Avoid reading current wall-clock time inside pure strategy logic.
* Strategy parameters should be configurable and testable.
* Do not silently change the baseline strategy.

Baseline assumptions:

* EMA(8/21) crossover
* RSI threshold around 50
* MACD histogram direction filter
* Bollinger Band ±2σ entry avoidance
* ATR(14)-based stop loss and take profit
* ADX is optional and should not be reintroduced as mandatory unless requested

## Risk and order execution

When implementing execution-related code:

* Prefer no trade when state is uncertain.
* Check whether a position is already open before creating a new entry order.
* Avoid duplicate orders on retry.
* Make order idempotency explicit where possible.
* Do not retry live order placement blindly.
* Record enough information to reconstruct why an order was placed.
* OCO order placement should happen only after entry execution is confirmed.
* Stop-loss and take-profit prices should be derived from actual or confirmed entry price.
* Lot size calculation must consider stop-loss width.
* Avoid placing orders when ATR, spread, account balance, or price data is invalid.

## Backtesting

When modifying backtest code:

* Avoid look-ahead bias.
* Use confirmed candles for signal generation.
* Make execution timing explicit.
* Avoid using future candle high/low/close values for decisions that would not have been known.
* Account for spread, fees, and slippage where supported.
* Save strategy parameters with each backtest result when persistence exists.
* Keep backtest output reproducible.
* Add tests for edge cases and known scenarios.

Useful metrics:

* total return
* win rate
* profit factor
* max drawdown
* average win
* average loss
* risk-reward ratio
* number of trades
* maximum consecutive losses

## Persistence

When working with persistence:

* Do not expose persistence entities directly from APIs.
* Keep schema changes explicit.
* Add migrations if Flyway or Liquibase is already used.
* Store timestamps consistently.
* Preserve enough historical data to debug trading decisions.
* Do not silently change column meaning or units.
* Be explicit about price precision, lot precision, and currency pair.

Useful persisted records may include:

* candles
* generated signals
* strategy configurations
* orders
* executions
* positions
* trades
* backtest runs
* backtest summaries
* operational events

## API design

When creating or changing backend APIs:

* Use RESTful endpoints unless the existing code uses another style.
* Keep request and response DTOs explicit.
* Do not return database entities directly.
* Use consistent date/time formats.
* Include enough information for the frontend to explain current trading state.
* Dangerous operations such as enabling live trading or placing orders should require explicit intent.
* Prefer idempotent design for trading operations where practical.
* Return clear errors instead of generic failures.

Useful API response fields for trading state:

* trading enabled/disabled
* current position
* latest signal
* latest candle timestamp
* entry price
* stop loss
* take profit
* unrealized P/L
* realized P/L
* strategy name
* strategy parameters
* skip reason when no trade was made

## Scheduling and operations

For scheduled backend jobs:

* Avoid overlapping executions.
* Log start, end, and failure of each scheduled run.
* Make trading enabled/disabled state explicit.
* Fail safely when broker API, market data API, or database is unavailable.
* Do not place duplicate orders after restart or retry.
* Persist enough state to recover safely after restart.
* Use structured logs where possible.

## Logging

Log important events:

* generated signal
* skipped entry and reason
* order placement request
* order placement result
* OCO placement result
* position opened
* position closed
* risk calculation details
* trading suspension reason
* external API failure
* backtest summary

Avoid logging:

* credentials
* tokens
* account secrets
* raw API keys
* excessive market data dumps in normal operation

## Backend testing

When modifying backend code:

* Add unit tests for strategy logic.
* Add unit tests for risk and lot-size calculations.
* Add tests for order execution safety.
* Add tests for backtest behavior when changing simulation logic.
* Prefer fast tests that do not require the full Spring context unless integration behavior is being tested.

Important edge cases:

* no candle data
* insufficient candle history
* null or missing indicator values
* zero or negative ATR
* very small stop-loss width
* invalid spread
* no open position
* already open position
* broker API failure
* database failure
* duplicated scheduler execution
* weekend or trading suspension period

## Backend validation before completion

Before completing backend changes:

* Code compiles.
* Tests pass.
* New strategy behavior has tests.
* Risk calculation remains safe.
* No credentials or secrets are committed.
* API changes are reflected in frontend callers if needed.
* Backtest assumptions are documented.
* Live trading behavior fails safely.
