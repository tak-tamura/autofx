# Copilot Instructions

## Project overview

This repository is for **AutoFX**, an automated FX trading application.

The application focuses on systematic FX trading, backtesting, strategy evaluation, and operational monitoring.

Primary assumptions:

* Currency pair: primarily `USD/JPY`
* Timeframe: primarily `1H`
* Position rule: generally one open position at a time
* Strategy style: rule-based technical trading
* Goal: stable long-term operation with controlled risk, not aggressive short-term optimization

The system may include:

* Historical backtesting
* Live or semi-live trading execution
* Strategy evaluation
* Risk management
* Operational controls such as weekend stop and economic-indicator stop
* Web UI for monitoring charts, trades, signals, and performance

## General development principles

When generating or modifying code:

* Prefer simple, readable, production-oriented code.
* Keep business rules explicit and easy to test.
* Do not silently change trading logic.
* Do not introduce new dependencies unless they clearly simplify the implementation.
* Follow the existing package structure, naming conventions, and coding style.
* Preserve backward compatibility unless the task explicitly asks for a breaking change.
* Prefer small, focused changes.
* Add or update tests when changing strategy logic, order execution, risk calculation, persistence, or API behavior.
* Do not hardcode secrets, API keys, account IDs, tokens, or credentials.
* Use environment variables or externalized configuration for sensitive values.
* Avoid large refactors unless specifically requested.

## Domain model and terminology

Use precise trading terminology.

Common concepts:

* `Candle` / `PriceBar`: OHLCV data for a fixed timeframe
* `Strategy`: logic that produces buy/sell/no-trade signals
* `Signal`: trading decision generated from indicators
* `Position`: currently open trade
* `Order`: request sent to broker/exchange API
* `Trade`: completed or recorded execution result
* `Backtest`: simulation over historical data
* `Indicator`: EMA, RSI, MACD, Bollinger Bands, ATR, ADX, etc.
* `RiskManagement`: position sizing, stop loss, take profit, max drawdown, exposure limits
* `OCO`: pair of stop-loss and take-profit orders after entry

Prefer domain-specific names over vague names like `Data`, `Info`, `Manager`, or `Util`.

## Current strategy assumptions

Unless the existing code or task says otherwise, assume the current baseline strategy is:

* EMA crossover:

    * Fast EMA: 8
    * Slow EMA: 21
* RSI filter:

    * Buy when RSI is above 50
    * Sell when RSI is below 50
* MACD histogram filter:

    * Buy when MACD histogram is positive
    * Sell when MACD histogram is negative
* Bollinger Band avoidance:

    * Avoid entries when price has already exceeded ±2σ
* ATR-based exit:

    * Stop loss: ATR(14) × 1.5
    * Take profit: ATR(14) × 3.0
* ADX filter:

    * Do not assume ADX is mandatory.
    * ADX was previously tested but may reduce trade count too much.
* Pullback / rebound logic may be added as a rescue pattern, but keep it separate from the baseline crossover logic.

Important:

* Do not optimize parameters casually.
* Do not change strategy thresholds without making the change explicit.
* Any strategy parameter should be configurable.
* Backtest changes should clearly separate:

    * entry condition
    * exit condition
    * risk sizing
    * fees / spread / slippage
    * execution timing

## Risk management rules

Risk management is critical.

When implementing trading logic:

* Stop loss and take profit must be calculated from the actual entry price and current volatility assumptions.
* Lot size calculation should account for stop-loss width.
* Do not calculate position size only from account balance without considering stop-loss distance.
* Avoid unlimited averaging down or martingale-like logic unless explicitly requested.
* Avoid opening multiple positions unless the task explicitly changes the single-position rule.
* Weekend position holding should be avoided unless explicitly enabled.
* Important economic indicators may require trading suspension.
* Fail safely: if market data, account state, or order state is uncertain, prefer no trade over risky execution.

## Backtesting expectations

When working on backtesting:

* Avoid look-ahead bias.
* Use only data available at the decision time.
* Signals should generally be based on confirmed candles.
* Be explicit about whether trades execute at:

    * close price
    * next candle open
    * bid/ask adjusted price
* Account for spread, fees, and slippage where supported.
* Keep performance metrics clear:

    * total return
    * win rate
    * profit factor
    * max drawdown
    * average win/loss
    * risk-reward ratio
    * number of trades
    * consecutive losses
* Do not judge a strategy only by total profit.
* Prefer reproducible test fixtures for strategy behavior.

## Configuration

Prefer externalized configuration for:

* strategy parameters
* currency pair
* timeframe
* lot size limits
* risk percentage
* spread/slippage assumptions
* trading enabled/disabled flag
* broker API endpoints
* economic indicator stop settings
* weekend stop settings

Use sensible defaults for local development, but make production-sensitive values explicit.

## Code review priorities

When reviewing or suggesting changes, prioritize:

1. Trading safety
2. Correctness of financial calculations
3. Avoidance of look-ahead bias
4. Testability
5. Operational recoverability
6. Clear logging and observability
7. Simplicity
8. Performance

Do not prioritize micro-optimizations unless there is evidence of a real bottleneck.

## Response style for Copilot Chat

When answering questions in this repository:

* Be concrete and code-oriented.
* Explain assumptions briefly.
* Point out risky trading or operational behavior.
* Suggest tests when changing important logic.
* If a request may affect live trading, clearly call out safety implications.
* Prefer patches or focused code examples over broad theoretical explanations.
* When unsure about existing behavior, inspect the repository before proposing large changes.

## Things to avoid

Avoid:

* Changing strategy behavior without saying so
* Introducing look-ahead bias in backtests
* Ignoring spread/slippage
* Ignoring stop-loss width in lot calculation
* Assuming live trading is safe by default
* Swallowing exceptions silently
* Retrying order placement without idempotency protection
* Hardcoding credentials
* Mixing controller, persistence, and trading logic
* Overengineering with unnecessary patterns
* Adding dependencies without checking existing alternatives

## Preferred implementation flow

For new trading features, prefer this flow:

1. Define or update domain model
2. Implement pure calculation logic
3. Add unit tests for calculation logic
4. Integrate into service layer
5. Add persistence if needed
6. Expose API if needed
7. Update frontend if needed
8. Add operational logs
9. Document configuration changes

## Validation before completing a task

Before considering a change complete, check the relevant items:

* Code compiles
* Existing tests pass
* New or changed strategy behavior has tests
* Risk calculation is still safe
* No secrets are committed
* Logs are useful but not noisy
* API changes are reflected in frontend callers
* Backtest assumptions are documented
* Live trading behavior fails safely
