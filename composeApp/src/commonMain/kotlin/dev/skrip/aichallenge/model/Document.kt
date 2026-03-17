package dev.skrip.aichallenge.model

import kotlinx.serialization.Serializable

enum class DocumentType(val extensions: List<String>) {
    MARKDOWN(listOf("md")),
    TEXT(listOf("txt")),
    KOTLIN(listOf("kt", "kts")),
    JAVA(listOf("java"));

    companion object {
        fun fromExtension(ext: String): DocumentType? {
            return entries.find { ext.lowercase() in it.extensions }
        }
    }
}

@Serializable
data class Document(
    val name: String,
    val path: String,
    val content: String,
    val type: DocumentType
)
