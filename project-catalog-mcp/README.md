# Project Catalog MCP Server

MCP (Model Context Protocol) сервер для анализа структуры Kotlin Multiplatform проекта.

## Tools

| Tool | Описание |
|------|----------|
| `project_info` | Базовая информация о проекте: имя, тип, директории |
| `list_modules` | Список модулей проекта из settings.gradle.kts |
| `list_screens` | Поиск Compose screens (@Composable *Screen) |

## Сборка

```bash
cd project-catalog-mcp
./gradlew build
```

## Запуск

### Проверка списка tools (клиент)

```bash
./gradlew runListTools
```

Ожидаемый вывод:
```
=== MCP Client: List Tools ===

Starting MCP server process...

Available tools:

  - project_info
    Get basic information about the project: name, type, main directories

  - list_modules
    List all modules/directories in the project, excluding build artifacts

  - list_screens
    Find Compose screens (@Composable functions ending with 'Screen') in the project

Done.
```

### Запуск сервера напрямую (stdio)

```bash
./gradlew runServer
```

Сервер общается через stdin/stdout по протоколу MCP.
Логи идут в stderr.

## Использование с Claude Desktop

Добавить в `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "project-catalog": {
      "command": "/path/to/project-catalog-mcp/gradlew",
      "args": ["--quiet", "runServer"],
      "cwd": "/path/to/project-catalog-mcp"
    }
  }
}
```

## Структура

```
project-catalog-mcp/
├── build.gradle.kts      # Gradle config
├── settings.gradle.kts   # Project settings
├── README.md             # This file
└── src/main/kotlin/mcp/
    ├── Server.kt         # MCP server entry point
    ├── Client.kt         # Test client
    └── ProjectScanner.kt # Project analysis logic
```
