## Day 19 Demo: Content Pipeline

This demo shows how Claude automatically orchestrates a chain of MCP tools:

```
User Request
    ↓
Claude Agent (Anthropic API)
    ↓
1. search_posts(query)      → fetch posts from JSONPlaceholder
    ↓
2. summarize_posts(json)    → create summary
    ↓
3. save_to_file(filename)   → save to disk
    ↓
Final Response in UI
```

### Quick Start

```bash
# 1. Build MCP server
cd content-pipeline-mcp
./gradlew jar
cd ..

# 2. Set API key
export ANTHROPIC_API_KEY=sk-ant-...

# 3. Run desktop app
./gradlew :composeApp:run
```

### What You'll See

- Single input field with placeholder hint
- "Run Pipeline" button
- Execution trace showing each tool call
- Search results preview
- Generated summary
- Saved file path
- Final Claude response

### Project Structure

```
AiChallenge/
├── composeApp/                    # KMP desktop app
│   └── src/jvmMain/kotlin/
│       └── pipeline/
│           ├── McpClient.kt       # MCP stdio client
│           ├── PipelineAgent.kt   # Claude agent with tool handling
│           └── PipelineScreen.kt  # Compose UI
│
├── content-pipeline-mcp/          # MCP server (separate Gradle project)
│   └── src/main/kotlin/
│       ├── Main.kt                # MCP server with 3 tools
│       └── JsonPlaceholderClient.kt
│
└── pipeline-output/               # Saved files appear here
```

### MCP Tools

| Tool | Description |
|------|-------------|
| `search_posts` | Search posts from JSONPlaceholder by query |
| `summarize_posts` | Create summary from posts JSON |
| `save_to_file` | Save content to pipeline-output/ directory |

### Build Commands

```bash
# Desktop
./gradlew :composeApp:run

# Android
./gradlew :composeApp:assembleDebug

# iOS - open in Xcode
open iosApp/iosApp.xcodeproj
```
