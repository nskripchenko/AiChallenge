package dev.skrip.aichallenge.chunking

import dev.skrip.aichallenge.model.Chunk
import dev.skrip.aichallenge.model.ChunkMetadata
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.model.Document
import dev.skrip.aichallenge.model.DocumentType

class StructuredChunker : Chunker {

    override fun chunk(document: Document): List<Chunk> {
        return when (document.type) {
            DocumentType.MARKDOWN -> chunkMarkdown(document)
            DocumentType.KOTLIN, DocumentType.JAVA -> chunkCode(document)
            DocumentType.TEXT -> chunkText(document)
        }
    }

    private fun chunkMarkdown(document: Document): List<Chunk> {
        val content = document.content
        val chunks = mutableListOf<Chunk>()

        // Split by headers (# ## ### etc.)
        val headerPattern = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
        val matches = headerPattern.findAll(content).toList()

        if (matches.isEmpty()) {
            // No headers, treat as single chunk
            return listOf(createChunk(document, content, 0, content.length, "content", 0))
        }

        var chunkIndex = 0

        // Content before first header (if any)
        val firstMatch = matches.first()
        if (firstMatch.range.first > 0) {
            val preContent = content.substring(0, firstMatch.range.first).trim()
            if (preContent.isNotEmpty()) {
                chunks.add(createChunk(document, preContent, 0, firstMatch.range.first, "intro", chunkIndex++))
            }
        }

        // Process each section
        matches.forEachIndexed { index, match ->
            val sectionTitle = match.groupValues[2].trim()
            val startOffset = match.range.first
            val endOffset = if (index < matches.size - 1) {
                matches[index + 1].range.first
            } else {
                content.length
            }

            val sectionContent = content.substring(startOffset, endOffset).trim()
            if (sectionContent.isNotEmpty()) {
                chunks.add(createChunk(document, sectionContent, startOffset, endOffset, sectionTitle, chunkIndex++))
            }
        }

        return chunks
    }

    private fun chunkCode(document: Document): List<Chunk> {
        val content = document.content
        val chunks = mutableListOf<Chunk>()

        // Pattern for top-level declarations: class, object, interface, fun (not inside class)
        // This is a simplified approach - we look for declarations at the start of lines
        val declarationPattern = Regex(
            "^(class|object|interface|fun|data class|sealed class|enum class|abstract class)\\s+\\w+",
            RegexOption.MULTILINE
        )

        val matches = declarationPattern.findAll(content).toList()

        if (matches.isEmpty()) {
            // No declarations found, return whole file as one chunk
            return listOf(createChunk(document, content, 0, content.length, "code", 0))
        }

        var chunkIndex = 0

        // Package/imports before first declaration
        val firstMatch = matches.first()
        if (firstMatch.range.first > 0) {
            val header = content.substring(0, firstMatch.range.first).trim()
            if (header.isNotEmpty() && (header.contains("package") || header.contains("import"))) {
                chunks.add(createChunk(document, header, 0, firstMatch.range.first, "imports", chunkIndex++))
            }
        }

        // Process each declaration
        matches.forEachIndexed { index, match ->
            val declType = match.groupValues[1]
            val startOffset = match.range.first
            val endOffset = if (index < matches.size - 1) {
                matches[index + 1].range.first
            } else {
                content.length
            }

            val declContent = content.substring(startOffset, endOffset).trim()
            if (declContent.isNotEmpty()) {
                // Extract name from declaration
                val nameMatch = Regex("(class|object|interface|fun)\\s+(\\w+)").find(declContent)
                val name = nameMatch?.groupValues?.getOrNull(2) ?: declType

                chunks.add(createChunk(document, declContent, startOffset, endOffset, "$declType:$name", chunkIndex++))
            }
        }

        return chunks
    }

    private fun chunkText(document: Document): List<Chunk> {
        val content = document.content
        val chunks = mutableListOf<Chunk>()

        // Split by double newlines (paragraphs) or significant sections
        val paragraphs = content.split(Regex("\n\\s*\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) {
            return listOf(createChunk(document, content, 0, content.length, "content", 0))
        }

        // Group small paragraphs together, split large ones
        var currentChunk = StringBuilder()
        var currentStart = 0
        var chunkIndex = 0
        var searchStart = 0

        paragraphs.forEach { paragraph ->
            val paragraphStart = content.indexOf(paragraph, searchStart)
            searchStart = paragraphStart + paragraph.length

            if (currentChunk.length + paragraph.length > 800) {
                // Save current chunk if not empty
                if (currentChunk.isNotEmpty()) {
                    val chunkText = currentChunk.toString().trim()
                    val endOffset = content.indexOf(chunkText, currentStart) + chunkText.length
                    chunks.add(createChunk(document, chunkText, currentStart, endOffset, "section_$chunkIndex", chunkIndex++))
                    currentChunk = StringBuilder()
                    currentStart = paragraphStart
                }
            }

            if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
            currentChunk.append(paragraph)
        }

        // Don't forget the last chunk
        if (currentChunk.isNotEmpty()) {
            val chunkText = currentChunk.toString().trim()
            chunks.add(createChunk(document, chunkText, currentStart, content.length, "section_$chunkIndex", chunkIndex))
        }

        return chunks
    }

    private fun createChunk(
        document: Document,
        text: String,
        startOffset: Int,
        endOffset: Int,
        section: String,
        index: Int
    ): Chunk {
        return Chunk(
            id = "${document.name}_struct_$index",
            text = text,
            metadata = ChunkMetadata(
                source = document.path,
                file = document.name,
                title = extractTitle(document),
                section = section,
                strategy = ChunkingStrategy.STRUCTURED,
                startOffset = startOffset,
                endOffset = endOffset
            )
        )
    }

    private fun extractTitle(document: Document): String? {
        val firstLine = document.content.lines().firstOrNull()?.trim() ?: return null
        return when {
            firstLine.startsWith("#") -> firstLine.trimStart('#').trim()
            firstLine.startsWith("package") -> firstLine.removePrefix("package").trim()
            else -> null
        }
    }
}
