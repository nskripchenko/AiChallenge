package dev.skrip.aichallenge.data.repository

import dev.skrip.aichallenge.domain.invariants.Invariant
import dev.skrip.aichallenge.domain.invariants.InvariantState
import dev.skrip.aichallenge.domain.invariants.InvariantStorage
import dev.skrip.aichallenge.domain.invariants.InvariantType
import dev.skrip.aichallenge.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSON file-based storage for invariants.
 * Stored separately from dialog in ~/.aichallenge/invariants.json
 */
class JsonInvariantStorage(
    private val baseDir: String = System.getProperty("user.home") + "/.aichallenge"
) : InvariantStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val file = File(baseDir, "invariants.json")

    private val _state = MutableStateFlow(InvariantState())
    override val state: StateFlow<InvariantState> = _state.asStateFlow()

    override suspend fun initialize() {
        File(baseDir).mkdirs()

        if (file.exists()) {
            try {
                val content = file.readText()
                val loaded = json.decodeFromString<InvariantState>(content)
                _state.value = loaded
            } catch (e: Exception) {
                // If corrupted, reset to defaults
                resetToDefaults()
            }
        } else {
            // First run - create defaults
            resetToDefaults()
        }
    }

    override suspend fun addInvariant(invariant: Invariant) {
        _state.update { state ->
            state.copy(invariants = state.invariants + invariant)
        }
        save()
    }

    override suspend fun updateInvariant(invariant: Invariant) {
        _state.update { state ->
            state.copy(
                invariants = state.invariants.map {
                    if (it.id == invariant.id) invariant else it
                }
            )
        }
        save()
    }

    override suspend fun removeInvariant(id: String) {
        _state.update { state ->
            state.copy(invariants = state.invariants.filter { it.id != id })
        }
        save()
    }

    override suspend fun toggleInvariant(id: String, isActive: Boolean) {
        _state.update { state ->
            state.copy(
                invariants = state.invariants.map {
                    if (it.id == id) it.copy(isActive = isActive) else it
                }
            )
        }
        save()
    }

    override suspend fun resetToDefaults() {
        _state.value = InvariantState(invariants = DEFAULT_INVARIANTS)
        save()
    }

    private fun save() {
        try {
            val content = json.encodeToString(InvariantState.serializer(), _state.value)
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * Default invariants for crypto consultant
         */
        val DEFAULT_INVARIANTS = listOf(
            Invariant(
                id = "max-position-size",
                type = InvariantType.LIMIT,
                rule = "Максимум 30% депозита в одну позицию",
                description = "Не рекомендовать вкладывать более 30% депозита в одну криптовалюту",
                promptInstruction = "Никогда не рекомендуй вкладывать более 30% депозита в одну криптовалюту",
                isActive = true,
                isDefault = true
            ),
            Invariant(
                id = "always-stop-loss",
                type = InvariantType.MUST,
                rule = "Всегда указывать stop-loss",
                description = "Каждая рекомендация должна содержать уровень stop-loss",
                promptInstruction = "Всегда указывай stop-loss для каждой рекомендации покупки",
                isActive = true,
                isDefault = true
            ),
            Invariant(
                id = "no-meme-coins",
                type = InvariantType.MUST_NOT,
                rule = "Не рекомендовать мем-коины",
                description = "Запрещено рекомендовать DOGE, SHIB, PEPE и подобные",
                promptInstruction = "Не рекомендуй мем-коины (DOGE, SHIB, PEPE, FLOKI и подобные)",
                isActive = true,
                isDefault = true
            ),
            Invariant(
                id = "no-leverage",
                type = InvariantType.MUST_NOT,
                rule = "Не использовать кредитное плечо",
                description = "Запрещено рекомендовать маржинальную торговлю и leverage",
                promptInstruction = "Не рекомендуй использовать leverage, маржу или кредитное плечо",
                isActive = true,
                isDefault = true
            ),
            Invariant(
                id = "diversification",
                type = InvariantType.MUST,
                rule = "Диверсификация портфеля",
                description = "Рекомендовать минимум 2-3 актива для снижения рисков",
                promptInstruction = "При составлении портфеля рекомендуй минимум 2-3 разных актива",
                isActive = false,
                isDefault = true
            )
        )
    }
}
