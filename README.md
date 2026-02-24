# День 7. Сохранение контекста

## Что сделано

Реализовано сохранение истории диалога:
- История хранится в JSON файле (`~/.aichallenge/chat_history.json`)
- При запуске приложения история загружается автоматически
- Диалог продолжается так, как будто агент не выключался

## Интерфейс

Desktop приложение с 3 панелями:
- **Log** — JSON запросов/ответов
- **Chat** — сообщения со streaming, выделение текста
- **Settings** — model, temperature, system prompt, лимит истории, кнопка Clear

## Запуск

```bash
export ANTHROPIC_API_KEY="sk-..."
./gradlew composeApp:run
```

## Стек

Kotlin Multiplatform, Compose Desktop, Ktor, Koin, kotlinx.serialization
