# Content Pipeline MCP Server

MCP (Model Context Protocol) server for Day 19 demo - demonstrating automatic tool composition with Claude as orchestrator.

## Overview

This MCP server provides 3 tools that form a content processing pipeline:

1. **search_posts** - Search posts from JSONPlaceholder API
2. **summarize_posts** - Create a summary from search results
3. **save_to_file** - Save content to a file

## How It Works

The pipeline flow:

```
User Request
    ↓
Claude Agent (via Anthropic API)
    ↓
1. search_posts(query) → returns matching posts JSON
    ↓
2. summarize_posts(postsJson) → returns summary
    ↓
3. save_to_file(filename, content) → saves to disk
    ↓
Final Response
```

Claude automatically orchestrates this chain - no manual step-by-step execution needed.

## Tools

### search_posts

Searches posts from JSONPlaceholder by query string.

**Input:**
- `query` (string): Search term to filter posts by title or body

**Output:** JSON with count and matching posts (id, title, body)

### summarize_posts

Creates a summary from posts JSON.

**Input:**
- `postsJson` (string): JSON string from search_posts output

**Output:** JSON with summaryText, count, query, topThemes

### save_to_file

Saves content to a file in the `pipeline-output/` directory.

**Input:**
- `filename` (string): Output filename
- `content` (string): Content to save

**Output:** JSON with success status and saved path

## Security

- Files are saved only to `pipeline-output/` directory
- Filenames are sanitized (only alphanumeric, dots, hyphens, underscores allowed)

## Building

```bash
cd content-pipeline-mcp
./gradlew build
```

The fat JAR will be at `build/libs/content-pipeline-mcp-1.0.0.jar`

## Running the Demo

1. Build the MCP server:
   ```bash
   cd content-pipeline-mcp
   ./gradlew jar
   ```

2. Set your Anthropic API key:
   ```bash
   export ANTHROPIC_API_KEY=your-key-here
   ```

3. Run the desktop app:
   ```bash
   cd ..
   ./gradlew :composeApp:run
   ```

4. In the UI, click "Run Pipeline" to execute the demo

## Output Location

Saved files appear in: `content-pipeline-mcp/pipeline-output/`
