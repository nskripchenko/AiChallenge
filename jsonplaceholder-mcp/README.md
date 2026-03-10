# JSONPlaceholder MCP Server

MCP (Model Context Protocol) server that provides tools for fetching data from JSONPlaceholder API.

## Tools

### get_post
Fetches a post by ID from JSONPlaceholder API.

**Input:**
- `id` (integer, required): The post ID to fetch

**Output:**
- userId
- id
- title
- body

### get_user
Fetches a user by ID from JSONPlaceholder API.

**Input:**
- `id` (integer, required): The user ID to fetch

**Output:**
- id
- name
- username
- email
- company name

## Build

```bash
cd jsonplaceholder-mcp
./gradlew build
```

This creates a fat JAR at `build/libs/jsonplaceholder-mcp-1.0.0.jar`.

---

## Day 17: MCP Tool Calling with LLM Agent

This demo shows Claude (Anthropic API) calling MCP tools to fetch real data.

### Architecture

```
User (desktop UI)
       ↓
   User query (e.g., "Show me post 1")
       ↓
JsonPlaceholderAgent
       ↓
ClaudeApiClient → Anthropic API
       ↓
Claude decides to call tool (tool_use)
       ↓
JsonPlaceholderAgent executes tool via MCP
       ↓
McpJsonPlaceholderAgent → MCP Server → JSONPlaceholder API
       ↓
Tool result sent back to Claude
       ↓
Claude generates final response
       ↓
Response displayed in UI
```

### How Claude Calls MCP Tools

1. **User sends query**: "Show me post 1"
2. **Claude receives tools definition** with `get_post` and `get_user`
3. **Claude responds with `tool_use`**: calls `get_post(id=1)`
4. **Agent executes tool** via MCP client
5. **Tool result sent back** as `tool_result` message
6. **Claude generates final answer** using the data

### Run the Demo

1. Set your Anthropic API key:
```bash
export ANTHROPIC_API_KEY=your-key-here
```

2. Build the MCP server:
```bash
cd jsonplaceholder-mcp
./gradlew build
```

3. Run the desktop app:
```bash
cd ..
./gradlew :composeApp:run
```

4. In the app:
   - Click **Connect** to start MCP server and initialize Claude client
   - Enter a query like: `Show me post 1` or `Get user 2`
   - Click **Ask Claude**
   - Watch the "Agent Steps" panel to see tool calls
   - See Claude's response in the left panel

### Example Queries

- "Show me post 1"
- "Get the user with id 3"
- "What is the title of post 5?"
- "Tell me about user 1 and their company"

### Example Flow

**Query:** "Show me post 1"

**Agent Steps:**
```
User: Show me post 1
Sending to Claude...
Claude response received (stop_reason: tool_use)
Claude calls tool: get_post
  Input: {"id":1}
  Result: Post #1\nUser ID: 1\nTitle: sunt aut facere...
Sending tool results back to Claude...
Claude response received (stop_reason: end_turn)
Final response ready
```

**Claude's Response:**
```
Here's post 1:

Title: sunt aut facere repellat provident occaecati excepturi optio reprehenderit
User ID: 1
Body: quia et suscipit suscipit recusandae...
```

## Project Structure

```
jsonplaceholder-mcp/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── src/main/kotlin/dev/skrip/jsonplaceholder/
    ├── Main.kt               # MCP server entry point
    ├── JsonPlaceholderApi.kt # HTTP client
    └── Models.kt             # Data classes

composeApp/src/jvmMain/kotlin/dev/skrip/aichallenge/
├── main.kt                   # Desktop app entry point
├── Day17DemoScreen.kt        # UI with query input
├── JsonPlaceholderAgent.kt   # LLM agent coordinator
├── ClaudeApiClient.kt        # Anthropic API client
└── McpJsonPlaceholderAgent.kt # MCP client wrapper
```
