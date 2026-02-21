package dev.skrip.aichallenge.ui.modelbench

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.api.AiClient
import dev.skrip.aichallenge.copyToClipboard
import dev.skrip.aichallenge.model.ModelProfiles
import dev.skrip.aichallenge.model.ModelTier
import dev.skrip.aichallenge.util.CostCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun ModelBenchScreen(apiKey: String) {
    var state by remember { mutableStateOf(ModelBenchScreenState()) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Day 5: Сравнение моделей",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле ввода запроса
        OutlinedTextField(
            value = state.prompt,
            onValueChange = { state = state.copy(prompt = it) },
            label = { Text("Запрос") },
            placeholder = { Text("Введите запрос, который хотите прогнать через слабую / среднюю / сильную модель") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            enabled = !state.isRunning,
            maxLines = 10
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка запуска
        Button(
            onClick = {
                if (state.prompt.isBlank()) return@Button

                state = state.copy(isRunning = true)

                // Сброс состояний карточек
                state = state.copy(
                    cards = ModelTier.entries.associateWith { tier ->
                        ModelCardState(tier = tier, status = ModelCardStatus.LOADING)
                    }
                )

                // Запуск параллельных запросов
                scope.launch(Dispatchers.Default) {
                    val client = AiClient(apiKey)
                    try {
                        val jobs = ModelTier.entries.map { tier ->
                            async {
                                runModelBench(client, tier, state.prompt) { updatedCard ->
                                    state = state.copy(
                                        cards = state.cards + (tier to updatedCard)
                                    )
                                }
                            }
                        }
                        jobs.awaitAll()
                    } finally {
                        client.close()
                        state = state.copy(isRunning = false)
                    }
                }
            },
            enabled = !state.isRunning && state.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (state.isRunning) "Выполняется…" else "Запустить на трёх моделях")
        }

        if (state.prompt.isBlank() && !state.isRunning) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Введите запрос для запуска",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Карточки моделей
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModelTier.entries.forEach { tier ->
                val cardState = state.cards[tier] ?: ModelCardState(tier)
                ModelCard(
                    cardState = cardState,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Блок сравнения
        val allDone = state.cards.values.all { it.status == ModelCardStatus.DONE }
        if (allDone && state.cards.values.any { it.responseText.isNotEmpty() }) {
            ComparisonBlock(state.cards)
        }
    }
}

private suspend fun runModelBench(
    client: AiClient,
    tier: ModelTier,
    prompt: String,
    onUpdate: (ModelCardState) -> Unit
) {
    val profile = ModelProfiles.byTier(tier)

    try {
        val startTime = System.nanoTime()
        val result = client.complete(
            modelId = profile.modelId,
            prompt = prompt,
            temperature = 0.2
        )
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000

        val inputTokens = result.inputTokens ?: CostCalculator.estimateTokens(prompt)
        val outputTokens = result.outputTokens ?: CostCalculator.estimateTokens(result.text)
        val cost = CostCalculator.estimateCost(profile, inputTokens, outputTokens)

        onUpdate(
            ModelCardState(
                tier = tier,
                status = ModelCardStatus.DONE,
                responseText = result.text,
                durationMs = durationMs,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                cost = cost
            )
        )
    } catch (e: Exception) {
        onUpdate(
            ModelCardState(
                tier = tier,
                status = ModelCardStatus.ERROR,
                errorMessage = e.message ?: "Неизвестная ошибка"
            )
        )
    }
}

@Composable
private fun ModelCard(
    cardState: ModelCardState,
    modifier: Modifier = Modifier
) {
    val profile = ModelProfiles.byTier(cardState.tier)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (cardState.status) {
                ModelCardStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                ModelCardStatus.DONE -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Заголовок
            Text(
                text = cardState.tier.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = profile.modelId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Статус
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Статус: ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = cardState.status.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (cardState.status) {
                        ModelCardStatus.ERROR -> MaterialTheme.colorScheme.error
                        ModelCardStatus.DONE -> MaterialTheme.colorScheme.primary
                        ModelCardStatus.LOADING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (cardState.status == ModelCardStatus.LOADING) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.dp
                    )
                }
            }

            // Метрики (показываем только если есть данные)
            if (cardState.status == ModelCardStatus.DONE || cardState.durationMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                MetricRow("Время:", "${cardState.durationMs} мс")
                MetricRow("Вх. токены:", "${cardState.inputTokens}")
                MetricRow("Вых. токены:", "${cardState.outputTokens}")
                MetricRow("Стоимость:", "$${String.format("%.4f", cardState.cost)}")
            }

            // Ошибка
            if (cardState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cardState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { copyToClipboard(cardState.errorMessage) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Копировать ошибку", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Ответ
            if (cardState.responseText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ответ:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val responseScrollState = rememberScrollState()
                    Text(
                        text = cardState.responseText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(responseScrollState)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        copyToClipboard(cardState.responseText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Копировать ответ", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ComparisonBlock(cards: Map<ModelTier, ModelCardState>) {
    val doneCards = cards.values.filter { it.status == ModelCardStatus.DONE && it.responseText.isNotEmpty() }

    if (doneCards.isEmpty()) return

    val fastest = doneCards.minByOrNull { it.durationMs }
    val cheapest = doneCards.minByOrNull { it.cost }
    val mostVerbose = doneCards.maxByOrNull { it.outputTokens }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Сравнение результатов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Таблица
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Модель", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Время", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Токены", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Стоимость", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            doneCards.forEach { card ->
                val profile = ModelProfiles.byTier(card.tier)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(profile.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text("${card.durationMs} мс", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text("${card.inputTokens + card.outputTokens}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text("$${String.format("%.4f", card.cost)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Выводы
            fastest?.let {
                val profile = ModelProfiles.byTier(it.tier)
                Text(
                    text = "🚀 Самая быстрая: ${profile.displayName} (${it.durationMs} мс)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            cheapest?.let {
                val profile = ModelProfiles.byTier(it.tier)
                Text(
                    text = "💰 Самая дешёвая: ${profile.displayName} ($${String.format("%.4f", it.cost)})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            mostVerbose?.let {
                val profile = ModelProfiles.byTier(it.tier)
                Text(
                    text = "📝 Самая многословная: ${profile.displayName} (${it.outputTokens} токенов)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Качество ответов оцените самостоятельно, сравнив тексты выше",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
