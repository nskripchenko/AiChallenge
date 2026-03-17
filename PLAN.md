# RAG Demo App - Implementation Plan

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/dev/skrip/aichallenge/
│   ├── App.kt                    # Main App composable
│   ├── model/
│   │   ├── Document.kt           # Document model
│   │   ├── Chunk.kt              # Chunk with metadata
│   │   ├── ChunkingStrategy.kt   # Enum: FIXED, STRUCTURED
│   │   ├── IndexEntry.kt         # Chunk + embedding
│   │   └── ChatMessage.kt        # UI message model
│   ├── chunking/
│   │   ├── Chunker.kt            # Interface
│   │   ├── FixedSizeChunker.kt   # Fixed-size implementation
│   │   └── StructuredChunker.kt  # Structure-aware implementation
│   ├── index/
│   │   └── IndexStorage.kt       # JSON save/load
│   ├── search/
│   │   └── SemanticSearch.kt     # Cosine similarity search
│   └── ui/
│       ├── ChatScreen.kt         # Main demo screen
│       └── ChatViewModel.kt      # UI state management
├── jvmMain/kotlin/dev/skrip/aichallenge/
│   ├── ollama/
│   │   └── OllamaClient.kt       # HTTP client for Ollama API
│   ├── DocumentLoader.kt         # Load files from docs/
│   └── main.kt                   # Desktop entry point
└── docs/                         # Test documents folder
```

## Data Models

### Document
```kotlin
data class Document(
    val name: String,
    val path: String,
    val content: String,
    val type: DocumentType  // MD, TXT, KT, JAVA
)
```

### Chunk
```kotlin
data class Chunk(
    val id: String,
    val text: String,
    val metadata: ChunkMetadata
)

data class ChunkMetadata(
    val source: String,
    val file: String,
    val title: String?,
    val section: String?,
    val strategy: ChunkingStrategy,
    val startOffset: Int,
    val endOffset: Int
)
```

### IndexEntry
```kotlin
data class IndexEntry(
    val chunk: Chunk,
    val embedding: List<Float>
)

data class DocumentIndex(
    val strategy: ChunkingStrategy,
    val entries: List<IndexEntry>,
    val createdAt: Long
)
```

## Implementation Steps

### Step 1: Models + Document Loading
- Create data models (Document, Chunk, ChunkMetadata)
- Implement DocumentLoader (jvmMain) - reads .md, .txt, .kt, .java from docs/
- Create docs/ folder with 2-3 test files
- **Check**: Run app, see loaded documents count in console

### Step 2: Fixed-Size Chunking
- Implement FixedSizeChunker with configurable chunk size and overlap
- Test on loaded documents
- **Check**: Print chunks to console, verify metadata

### Step 3: Structure-Aware Chunking
- Implement StructuredChunker
  - Markdown: split by headers (# ## ###)
  - Code (.kt/.java): split by class/object/fun declarations
- **Check**: Compare chunk count and content between strategies

### Step 4: Ollama Client (Embeddings)
- Add Ktor HTTP client dependency
- Implement OllamaClient with embeddings endpoint
- Config: embedding model name (e.g., "nomic-embed-text")
- **Check**: Generate embedding for test string, print vector size

### Step 5: Index Building + Storage
- Build index: chunks -> embeddings -> IndexEntry list
- Save to JSON file (fixed_index.json, structured_index.json)
- Load index from JSON
- **Check**: Reindex, verify JSON files created, reload and compare

### Step 6: Semantic Search
- Implement cosine similarity
- Query -> embedding -> find top-k similar chunks
- **Check**: Test query, print top-3 results with scores

### Step 7: Ollama Chat Generation
- Implement chat completion endpoint
- Build prompt with context chunks
- Config: chat model name (e.g., "llama3.2")
- **Check**: Ask question, get answer in console

### Step 8: Basic UI
- ChatScreen: single screen with all elements
- ChatViewModel: state management
- Elements:
  - Index status (docs count, chunks count, strategy)
  - Strategy toggle (Fixed/Structured)
  - Reindex button
  - Message list (user/assistant)
  - Input field + Ask button
  - Sources display under assistant messages
- **Check**: Full demo flow in UI

## Config

```kotlin
object Config {
    const val OLLAMA_BASE_URL = "http://localhost:11434"
    const val EMBEDDING_MODEL = "nomic-embed-text"
    const val CHAT_MODEL = "llama3.2"
    const val CHUNK_SIZE = 500
    const val CHUNK_OVERLAP = 50
    const val TOP_K = 3
}
```

## Dependencies to Add

```toml
# libs.versions.toml
ktor = "3.1.2"
kotlinx-serialization = "1.8.1"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

## Verification After Each Step

| Step | What to Check |
|------|---------------|
| 1 | Console: "Loaded N documents" |
| 2 | Console: chunks with metadata printed |
| 3 | Console: different chunk counts for Fixed vs Structured |
| 4 | Console: "Embedding size: 768" (or similar) |
| 5 | Files: fixed_index.json, structured_index.json exist |
| 6 | Console: top-3 search results with similarity scores |
| 7 | Console: LLM answer to test question |
| 8 | UI: full interactive demo working |
