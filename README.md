## Day 17: Первый инструмент MCP

**Задание:** Реализовать MCP-сервер вокруг API, подключить к агенту, вызвать инструмент и получить результат.

### Что сделано

#### 1. MCP-сервер (`jsonplaceholder-mcp/`)

Реализован MCP server на Kotlin/JVM вокруг JSONPlaceholder API.

**Регистрация инструментов:**

```kotlin
server.addTool(
    name = "get_post",
    description = "Get a post by ID from JSONPlaceholder API",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            put("id", buildJsonObject {
                put("type", JsonPrimitive("integer"))
                put("description", JsonPrimitive("The post ID to fetch"))
            })
        },
        required = listOf("id")
    )
) { request ->
    val id = request.arguments["id"]?.jsonPrimitive?.intOrNull
    val post = JsonPlaceholderApi.getPost(id)
    CallToolResult(content = listOf(TextContent(text = "Post #${post.id}...")))
}
```

**Зарегистрированные tools:**

| Tool       | Параметры  | Описание                        |
|------------|------------|---------------------------------|
| `get_post` | `id: Int`  | Получить пост по ID             |
| `get_user` | `id: Int`  | Получить пользователя по ID     |

#### 2. Агент с LLM (`composeApp/jvmMain/`)

**JsonPlaceholderAgent** — координирует взаимодействие:
- Отправляет запрос пользователя в Claude (Anthropic API)
- Получает `tool_use` от Claude с именем инструмента и аргументами
- Вызывает MCP tool через JSON-RPC
- Возвращает результат обратно Claude
- Получает финальный ответ

#### 3. Desktop UI

Визуализация всего процесса:
- **Execution Trace** — пошаговое выполнение
- **Tool Call** — какой tool выбрал Claude и с какими аргументами
- **Tool Result** — сырой результат от MCP server
- **Final Response** — ответ Claude после обработки результата

### Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│  Desktop App                                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  User: "Show me post 1"                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  JsonPlaceholderAgent                                     │  │
│  │  → Отправляет запрос в Claude API                         │  │
│  │  ← Получает tool_use: get_post(id=1)                      │  │
│  │  → Вызывает MCP tool                                      │  │
│  │  ← Получает результат                                     │  │
│  │  → Отправляет tool_result в Claude                        │  │
│  │  ← Получает финальный ответ                               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MCP Server (subprocess)                                  │  │
│  │  tools/call: get_post(id=1)                               │  │
│  │  → HTTP GET jsonplaceholder.typicode.com/posts/1          │  │
│  │  ← { userId: 1, title: "...", body: "..." }               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Запуск

```bash
# 1. Установить API ключ Anthropic
export ANTHROPIC_API_KEY=sk-ant-...

# 2. Собрать MCP server
cd jsonplaceholder-mcp && ./gradlew build && cd ..

# 3. Запустить desktop app
./gradlew :composeApp:run
```

### Демо сценарий

1. Нажать **Connect** — запускает MCP server как subprocess
2. Ввести запрос: `Show me post 1`
3. Нажать **Ask Claude**

**Результат на UI:**

```
Execution Trace:
  1. User Request: "Show me post 1"           ✓ Done
  2. Sending to Claude                        ✓ Done
  3. Claude Requested Tool: get_post          ✓ Done
  4. Calling MCP Tool                         ✓ Done
  5. Tool Result Received                     ✓ Done
  6. Sending Result to Claude                 ✓ Done
  7. Final Response Ready                     ✓ Done

Tool Call:
  Tool: get_post
  Arguments: { "id": 1 }

Tool Result (from MCP):
  Post #1
  User ID: 1
  Title: sunt aut facere repellat provident...
  Body: quia et suscipit suscipit recusandae...

Final Response (Claude):
  Here's post 1 from JSONPlaceholder:
  Title: sunt aut facere repellat provident...
  ...
```
