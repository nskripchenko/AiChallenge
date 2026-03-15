# search-mcp

MCP server for searching posts from JSONPlaceholder API.

## Tools

### search_posts

Search posts by query string.

**Input:**
- `query` (string): Search term to filter posts by title or body

**Output:** JSON with query, count, and array of matching posts (id, title, body)

## Build

```bash
./gradlew jar
```

## Run

The server runs via stdio. JAR location: `build/libs/search-mcp-1.0.0.jar`

```bash
java -jar build/libs/search-mcp-1.0.0.jar
```
