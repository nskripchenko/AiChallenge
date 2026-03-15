# file-mcp

MCP server for file operations in safe directory.

## Tools

### save_to_file

Save content to a file.

**Input:**
- `filename` (string): Filename to save
- `content` (string): Content to save

**Output:** JSON with success, savedPath, filename, bytesWritten

### read_file

Read file content.

**Input:**
- `filename` (string): Filename to read

**Output:** JSON with success, filename, content, size

## Security

All files are saved/read from `pipeline-output/` directory only.

## Build

```bash
./gradlew jar
```

## Run

```bash
java -jar build/libs/file-mcp-1.0.0.jar
```
