package dev.skrip.aichallenge.data.storage

import dev.skrip.aichallenge.domain.model.LongTermMemory
import dev.skrip.aichallenge.domain.model.WorkingMemory
import dev.skrip.aichallenge.domain.repository.MemoryStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSON-based storage for memory persistence
 *
 * Files:
 * - ~/.aichallenge/working_memory.json
 * - ~/.aichallenge/long_term_memory.json
 */
class JsonMemoryStorage : MemoryStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val baseDir = File(System.getProperty("user.home"), ".aichallenge").apply {
        if (!exists()) mkdirs()
    }

    private val workingMemoryFile = File(baseDir, "working_memory.json")
    private val longTermMemoryFile = File(baseDir, "long_term_memory.json")

    override suspend fun saveWorkingMemory(memory: WorkingMemory) = withContext(Dispatchers.IO) {
        try {
            workingMemoryFile.writeText(json.encodeToString(memory))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadWorkingMemory(): WorkingMemory = withContext(Dispatchers.IO) {
        try {
            if (workingMemoryFile.exists()) {
                json.decodeFromString<WorkingMemory>(workingMemoryFile.readText())
            } else {
                WorkingMemory()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            WorkingMemory()
        }
    }

    override suspend fun saveLongTermMemory(memory: LongTermMemory) = withContext(Dispatchers.IO) {
        try {
            longTermMemoryFile.writeText(json.encodeToString(memory))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadLongTermMemory(): LongTermMemory = withContext(Dispatchers.IO) {
        try {
            if (longTermMemoryFile.exists()) {
                json.decodeFromString<LongTermMemory>(longTermMemoryFile.readText())
            } else {
                LongTermMemory()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LongTermMemory()
        }
    }

    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        workingMemoryFile.delete()
        longTermMemoryFile.delete()
        Unit
    }
}
