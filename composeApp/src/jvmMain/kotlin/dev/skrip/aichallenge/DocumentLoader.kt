package dev.skrip.aichallenge

import dev.skrip.aichallenge.model.Document
import dev.skrip.aichallenge.model.DocumentType
import java.io.File

object DocumentLoader {

    private val supportedExtensions = DocumentType.entries.flatMap { it.extensions }

    fun loadDocuments(docsPath: String = "docs"): List<Document> {
        // Try multiple locations: direct path, parent dir, or absolute
        val possiblePaths = listOf(
            File(docsPath),
            File("../$docsPath"),
            File(System.getProperty("user.dir"), docsPath),
            File(System.getProperty("user.dir")).parentFile?.let { File(it, docsPath) }
        ).filterNotNull()

        val docsDir = possiblePaths.find { it.exists() && it.isDirectory }

        if (docsDir == null) {
            println("Warning: docs directory not found. Searched in:")
            possiblePaths.forEach { println("  - ${it.absolutePath}") }
            return emptyList()
        }

        println("Found docs directory at: ${docsDir.absolutePath}")

        return docsDir.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val ext = file.extension.lowercase()
                ext in supportedExtensions
            }
            .mapNotNull { file ->
                try {
                    val type = DocumentType.fromExtension(file.extension)
                    if (type != null) {
                        Document(
                            name = file.name,
                            path = file.relativeTo(docsDir).path,
                            content = file.readText(),
                            type = type
                        )
                    } else null
                } catch (e: Exception) {
                    println("Error reading file ${file.name}: ${e.message}")
                    null
                }
            }
            .toList()
    }
}
