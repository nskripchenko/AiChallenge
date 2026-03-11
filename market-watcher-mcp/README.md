# Market Watcher MCP Server

MCP (Model Context Protocol) server for periodic market data collection with SQLite storage.

## Features

- **Background Scheduler**: Periodically fetches market data based on configured intervals
- **SQLite Storage**: Persists watch jobs and market snapshots locally
- **MCP Tools**: Exposes tools for starting/stopping watchers and getting aggregated summaries
- **Binance API**: Uses public Binance API for OHLC candle data

## MCP Tools

### `start_market_watch`
Starts watching a market symbol with periodic data collection.

**Parameters:**
- `symbol`: Trading pair (e.g., "BTCUSDT")
- `timeframe`: Candle timeframe ("1m", "5m", "15m", "1h", etc.)
- `intervalSeconds`: How often to fetch data

**Returns:** Confirmation with watcher status

### `stop_market_watch`
Stops watching a market symbol.

**Parameters:**
- `symbol`: Trading pair to stop watching

**Returns:** Confirmation with stopped status

### `get_market_summary`
Returns aggregated summary from collected snapshots.

**Parameters:**
- `symbol`: Trading pair

**Returns:**
- `snapshotsCount`: Number of collected snapshots
- `lastPrice`, `minPrice`, `maxPrice`: Price range
- `priceChangePercent`: Change percentage
- `trend`: "bullish" / "bearish" / "sideways"
- `summary`: Human-readable summary text

### `get_watch_status`
Returns current status of a watcher.

**Parameters:**
- `symbol`: Trading pair

**Returns:** Active status, timeframe, interval, last run time, snapshot count

## Storage

Data is stored in `market_watcher.db` (SQLite):

- **watch_jobs**: Active watch configurations
- **market_snapshots**: Collected OHLC data points

## Building

```bash
cd market-watcher-mcp
../gradlew jar
```

Output: `build/libs/market-watcher-mcp-1.0.0.jar`

## Running

The server is designed to be started as a subprocess by the desktop app. It uses stdio for MCP transport.

```bash
java -jar build/libs/market-watcher-mcp-1.0.0.jar
```

Logs go to stderr, MCP messages go to stdout.

## Verifying Data Collection

After running the watcher for a while:

```bash
sqlite3 market_watcher.db "SELECT COUNT(*) FROM market_snapshots WHERE symbol='BTCUSDT'"
```

## Demo Flow

1. Desktop app starts MCP server as subprocess
2. User clicks "Start Watch" - creates watch job, scheduler begins collecting data
3. Snapshots accumulate in SQLite every N seconds
4. User clicks "Get Summary" - aggregates data, sends to Claude API
5. Claude analyzes and returns trading recommendation
6. User clicks "Stop Watch" - stops scheduler for symbol

## Architecture

```
Desktop UI
    ↓
MCP Client (stdio)
    ↓
MCP Server (this project)
    ├── Scheduler (coroutine loop)
    ├── MarketFetcher (Binance API)
    └── Database (SQLite)
```
