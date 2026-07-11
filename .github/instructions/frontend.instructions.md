---

## applyTo: "front/**,src/**/*.tsx,src/**/*.ts,src/**/*.jsx,src/**/*.js,**/*.tsx,**/*.ts"

# Frontend Instructions

## Frontend tech stack

Prefer the following frontend stack unless the existing code clearly says otherwise:

* React
* TypeScript if already used in the project
* lightweight-charts for price charts
* Chakra UI if already used in the project
* Fetch API, Axios, or the existing API client pattern

## Frontend development principles

When generating or modifying frontend code:

* Prefer simple, readable components.
* Keep trading screens clear and operationally useful.
* Avoid hiding important trading state behind overly compact UI.
* Follow the existing component structure, naming conventions, and styling approach.
* Do not introduce a new state management library unless clearly needed.
* Keep API access in dedicated client modules or hooks.
* Keep chart rendering logic separate from data fetching logic.
* Add loading, empty, and error states.
* Avoid large UI refactors unless specifically requested.

## React coding style

Use:

* Function components
* Hooks
* Small, focused components
* Explicit props
* Derived values instead of duplicated state where possible
* Memoization only when it improves clarity or prevents real performance issues

Avoid:

* Deeply nested component state
* Unnecessary global state
* Large components that combine API calls, chart rendering, and business formatting
* Hidden side effects inside render logic
* Magic numbers for trading-related display values

## Trading UI priorities

Trading UI should make the system state easy to understand.

Important values to display clearly where relevant:

* trading enabled/disabled state
* current position
* side: buy/sell
* entry price
* current price
* stop loss
* take profit
* unrealized P/L
* realized P/L
* latest signal
* latest candle timestamp
* strategy name
* strategy parameters
* backtest summary
* skipped trade reason
* broker/API connection status
* last updated time

For live or semi-live trading screens:

* Make dangerous actions visually and structurally explicit.
* Do not make it easy to accidentally enable live trading.
* Clearly show whether the system is in live mode, paper mode, or backtest mode if such modes exist.
* Prefer confirmation flows for dangerous operations if the existing app supports them.

## Charting

When working with lightweight-charts:

* Keep chart setup and cleanup correct.
* Avoid recreating chart instances unnecessarily.
* Separate candle data transformation from chart component rendering.
* Use consistent timestamp handling.
* Be careful with timezone display.
* Make signal markers clear and not overly noisy.
* Do not overload charts with too many indicators by default.
* Prefer readable defaults and allow additional overlays only when useful.

Useful chart overlays may include:

* candlesticks
* EMA fast / slow
* Bollinger Bands
* entry markers
* exit markers
* stop-loss line
* take-profit line

## API integration

When calling backend APIs:

* Keep API clients typed if TypeScript is used.
* Do not duplicate endpoint strings across many components.
* Handle loading, empty, and error states.
* Avoid assuming every backend response field is non-null.
* Be explicit with date/time parsing.
* Do not silently ignore API errors.
* Keep frontend DTOs aligned with backend response DTOs.
* If an API change is needed, update both the API client and all callers.

## State management

Prefer local state when possible.

Use broader state only for:

* authenticated user/session state if applicable
* trading mode/status shared across screens
* current selected currency pair/timeframe
* data that many unrelated components need

Avoid storing data globally just because it is convenient.

## Forms and configuration screens

For strategy or trading configuration forms:

* Validate numeric inputs.
* Make units explicit.
* Show defaults clearly.
* Prevent invalid values such as:

    * negative ATR multiplier
    * zero stop-loss distance
    * negative spread
    * invalid risk percentage
    * empty currency pair
    * invalid timeframe
* Avoid silently rounding financial values unless the UI makes it clear.
* Prefer explicit save/apply actions for trading configuration changes.

## Error handling

Display errors in a way that helps operation.

Good error messages should indicate:

* what failed
* whether trading may be affected
* whether the user can retry
* whether the backend or broker API is unavailable

Avoid exposing:

* raw stack traces
* credentials
* tokens
* sensitive account details

## Frontend testing

When modifying frontend code:

* Add or update component tests if the project already has a test setup.
* Test important rendering states:

    * loading
    * empty data
    * API error
    * current position exists
    * no current position
    * trading disabled
    * latest signal exists
* Keep data transformation logic testable outside UI components.
* For chart-related code, test transformation functions even if the chart library itself is not directly tested.

## UX tone

The frontend is an operational tool, not a decorative dashboard.

Prefer:

* clarity
* accurate numbers
* explicit state
* fast recognition of risk
* easy debugging

Avoid:

* overly flashy UI
* hiding important details
* ambiguous labels
* unclear colors without text labels
* dashboard-only views that cannot explain why a trade happened

## Frontend validation before completion

Before completing frontend changes:

* Code builds.
* Type errors are resolved.
* Existing tests pass if present.
* API response assumptions are valid.
* Loading, empty, and error states are handled.
* Trading status is displayed clearly.
* Dangerous actions are not made easier to trigger accidentally.
* Date/time display is consistent.
