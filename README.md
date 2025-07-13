# Trade Application

This is a trading application built using Java, Spring Boot, and React. It supports backtesting and live trading with configurable parameters.

## Features

- Configurable trading parameters via `trade-config.yml`
- Backtesting mode for strategy evaluation
- Leverage and balance management
- Integration with external trading APIs
- React-based frontend for user interaction

## Technologies Used

- **Backend**: Java, Spring Boot, SQL
- **Frontend**: React, TypeScript, JavaScript
- **Build Tools**: Gradle, npm

## Configuration

The application uses a configuration file located at `src/main/resources/trade-config.yml`. Below are the key parameters:

- `target-pair`: The currency pair to trade (e.g., USD/JPY).
- `target-time-frame`: The time frame for trading (e.g., MINUTE).
- `back-test`: Enable or disable backtesting mode.
- `available-balance-rate`: Percentage of balance to use for trading.
- `max-candle-num`: Maximum number of candles to analyze.
- `stop-limit`: Stop-loss limit as a percentage.
- `profit-limit`: Take-profit limit as a percentage.
- `indicator-limit`: Number of indicators to use for decision-making.
- `buy-point-threshold`: Threshold for buy signals.
- `sell-point-threshold`: Threshold for sell signals.
- `api-cost`: Cost per API call.
- `leverage`: Leverage multiplier for trades.

## Prerequisites

- Java 17 or higher
- Node.js and npm
- IntelliJ IDEA (recommended for development)

## Setup and Run

1. Clone the repository:
   ```bash
   git clone https://github.com/tak-tamura/autofx.git
   cd autofx
   ```

2. Build the backend:
   ```bash
   ./gradlew build
   ```

3. Start the backend:
   ```bash
   ./gradlew bootRun
   ```

4. Install frontend dependencies:
   ```bash
   cd frontend
   npm install
   ```

5. Start the frontend:
   ```bash
   npm start
   ```

6. Access the application at `http://localhost:3000`.

## Testing

- Run backend tests:
  ```bash
  ./gradlew test
  ```

- Run frontend tests:
  ```bash
  npm test
  ```

## License

This project is licensed under the MIT License.