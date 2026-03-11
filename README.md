# AI Challenge - Day 18: Market Watcher MCP Demo

Демо-проект MCP-инструмента с периодическим выполнением, SQLite хранилищем и LLM-агентом.

## Что реализовано

- **MCP Server** с background scheduler для сбора market data
- **SQLite** хранилище для watch jobs и snapshots
- **Binance API** для получения OHLC данных
- **Claude API** для анализа и торговых рекомендаций
- **Desktop UI** с real-time визуализацией

## Архитектура

```
Desktop UI (Compose)
    ↓
MCP Client → MCP Server (subprocess)
                ├── Scheduler (coroutines)
                ├── MarketFetcher (Binance)
                └── SQLite Storage
    ↓
Claude API → Trading Recommendations
```

## Быстрый старт

**1. Собрать MCP сервер:**
```bash
cd market-watcher-mcp
../gradlew jar
```

**2. Запустить desktop app:**
```bash
ANTHROPIC_API_KEY="sk-ant-..." ./gradlew :composeApp:run
```

**3. В UI:**
- Нажать **Start All Watchers**
- Смотреть как накапливаются snapshots и появляются рекомендации
- Каждые 5 секунд новая рекомендация для разных криптовалют

## MCP Tools

| Tool | Описание |
|------|----------|
| `start_market_watch` | Запустить watcher для символа |
| `stop_market_watch` | Остановить watcher |
| `get_market_summary` | Получить агрегированный summary |
| `get_watch_status` | Статус watcher'а |
