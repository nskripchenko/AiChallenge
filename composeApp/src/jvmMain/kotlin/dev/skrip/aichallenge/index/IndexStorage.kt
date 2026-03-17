package dev.skrip.aichallenge.index

import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.model.DocumentIndex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class IndexStorage(
    private val baseDir: String = "index"
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        File(baseDir).mkdirs()
    }

    fun getIndexPath(strategy: ChunkingStrategy): String {
        val fileName = when (strategy) {
            ChunkingStrategy.FIXED -> "fixed_index.json"
            ChunkingStrategy.STRUCTURED -> "structured_index.json"
        }
        return "$baseDir/$fileName"
    }

    fun saveIndex(index: DocumentIndex): File {
        val file = File(getIndexPath(index.strategy))
        file.parentFile?.mkdirs()
        val jsonString = json.encodeToString(index)
        file.writeText(jsonString)
        return file
    }

    fun loadIndex(strategy: ChunkingStrategy): DocumentIndex? {
        val file = File(getIndexPath(strategy))
        if (!file.exists()) return null

        return try {
            val jsonString = file.readText()
            json.decodeFromString<DocumentIndex>(jsonString)
        } catch (e: Exception) {
            println("Error loading index: ${e.message}")
            null
        }
    }

    fun indexExists(strategy: ChunkingStrategy): Boolean {
        return File(getIndexPath(strategy)).exists()
    }

    fun deleteIndex(strategy: ChunkingStrategy): Boolean {
        return File(getIndexPath(strategy)).delete()
    }

    fun getIndexInfo(strategy: ChunkingStrategy): IndexInfo? {
        val index = loadIndex(strategy) ?: return null
        return IndexInfo(
            strategy = index.strategy,
            documentCount = index.documentCount,
            chunkCount = index.entries.size,
            embeddingSize = index.entries.firstOrNull()?.embedding?.size ?: 0,
            createdAt = index.createdAt
        )
    }
}

data class IndexInfo(
    val strategy: ChunkingStrategy,
    val documentCount: Int,
    val chunkCount: Int,
    val embeddingSize: Int,
    val createdAt: Long
)
