# День 6. Первый агент

## Что сделано

Реализован агент `ChatAgent` как отдельная сущность с инкапсулированной логикой:
- Принимает запрос пользователя
- Формирует сообщения (system prompt + история + user message)
- Отправляет в Claude API через Ktor HttpClient
- Возвращает ответ в UI

## Интерфейс

Desktop приложение с 3 панелями:
- **Log** — JSON запросов/ответов (разворачиваемые карточки)
- **Chat** — сообщения + статистика (токены, время, цена)
- **Settings** — model, temperature, system prompt, лимит истории

## Запуск

```bash
export ANTHROPIC_API_KEY="sk-..."
./gradlew composeApp:run
```

## Стек

Kotlin Multiplatform, Compose Desktop, Ktor, Koin, kotlinx.serialization
