package dev.skrip.aichallenge

import kotlin.test.Test
import kotlin.test.assertTrue

class DocumentLoaderTest {

    @Test
    fun testLoadDocuments() {
        val documents = DocumentLoader.loadDocuments()

        println("=" .repeat(50))
        println("Loaded ${documents.size} documents:")
        documents.forEach { doc ->
            println("  - ${doc.name} (${doc.type}, ${doc.content.length} chars)")
        }
        println("=" .repeat(50))

        assertTrue(documents.isNotEmpty(), "Should load at least one document")
    }
}
