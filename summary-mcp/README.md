# summary-mcp

MCP server for text summarization and keyword extraction.

## Tools

### summarize_posts

Create summary from posts JSON.

**Input:**
- `postsJson` (string): JSON string with posts to summarize

**Output:** JSON with summaryText, count, topThemes

### extract_keywords

Extract keywords from text.

**Input:**
- `text` (string): Text to extract keywords from

**Output:** JSON with keywords array and count

## Build

```bash
./gradlew jar
```

## Run

```bash
java -jar build/libs/summary-mcp-1.0.0.jar
```
