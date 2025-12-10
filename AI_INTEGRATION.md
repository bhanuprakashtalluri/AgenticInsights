# AI Integration Setup

This document covers the AI integration for the MyTeam Agent, including API keys, configurations, models, and usage.

## API Keys

### Groq API Key
- **Environment Variable**: `GROQ_API_KEY`
- **Purpose**: Used for chat completions with Llama 70B model via Groq.
- **Setup**:
  1. Obtain a key from [Groq Console](https://console.groq.com/).
  2. Set the environment variable or add to `.env` file (not committed to version control).
  3. Example: `GROQ_API_KEY=your_key_here`

## AI Configuration

The AI is configured in `application.yml` under `spring.ai.openai`:

- **Base URL**: `https://api.groq.com/openai/v1` (Groq's OpenAI-compatible API)
- **Chat Model**: `llama3-70b-8192`
- **Temperature**: `0.7`
- **Embeddings Model**: `text-embedding-ada-002` (via OpenAI)
- **Cache**: Caffeine cache with `maximumSize=500, expireAfterWrite=10m`

## Prompt Templates

System prompt for the agent is in `src/main/resources/prompts/agent-system-prompt.st`:

```
You are an AI assistant for the MyTeam application, helping users with employee recognitions, insights, and management tasks.

Available tools:
- getEmployeeCount: Get the total number of employees.
- sendRecognition: Send a recognition to an employee. Parameters: sender (string), recipient (string), message (string).

User role: {role}
User input: {text}

First, determine the intent. If it matches a tool, invoke it. Otherwise, respond helpfully.

Respond in JSON format: {"intent": "tool_name or chat", "tool": "tool_name if applicable", "params": {...}, "response": "text if no tool"}
```

## Function Calling

The agent uses Spring AI's function calling to invoke MCP tools:
- `getEmployeeCount`: Returns mock count of 42.
- `sendRecognition`: Simulates sending a recognition.

Tools are defined in `McpLeaderImpl.java` and exposed as `FunctionCallback`s.

## Memory Management

Session-based memory using `InMemoryChatMemory` with conversation IDs from session.

## Testing

To test the agent:
1. Set `GROQ_API_KEY` in environment or `.env`.
2. Run the app: `./gradlew bootRun`
3. POST to `/api/agent/execute` with JSON: `{"text": "Get employee count"}`
4. The AI should respond with tool invocation results.

## Important Notes
- Never commit API keys to version control.
- The `.env` file is included in `.gitignore`.
- For production, use secure environment variable management.
- Current setup uses Groq for chat; embeddings via OpenAI.

## Architecture Overview

The AI agent follows this flow:
1. **AgentController**: Receives POST requests to `/api/agent/execute`, authenticates user, extracts role.
2. **AgentServiceImpl**: Performs RBAC check, builds ChatClient with memory and functions, calls AI.
3. **McpLeaderImpl**: Provides FunctionCallbacks for tools, executes tool logic.
4. **Spring AI**: Handles chat completion, function calling, and memory.

## Extending the Agent

### Adding New Tools
1. Add tool logic in `McpLeaderImpl.java`:
   ```java
   registerTool("newTool", input -> {
       // Implement tool logic
       return Map.of("result", "value");
   });
   ```
2. Create FunctionCallback:
   ```java
   functionCallbacks.add(FunctionCallback.builder()
       .function("newTool", this::newToolFunction)
       .inputType(NewToolInput.class)
       .build());
   ```
3. Define input record and function method.

### Updating Prompts
Edit `agent-system-prompt.st` to include new tools or instructions.

### Changing Models
Update `application.yml` under `spring.ai.openai.chat.options.model`.

## Environment Setup

- Install Java 21.
- Set `GROQ_API_KEY` in `.env` or environment.
- Run `./gradlew bootRun` to start the app.

## Troubleshooting

- **API Key Issues**: Ensure `GROQ_API_KEY` is set correctly.
- **Compilation Errors**: Check Spring AI version compatibility.
- **Tool Not Called**: Verify prompt includes tool descriptions.
- **Memory Not Working**: Check session ID in requests.

## Future Phases

- **Phase 3**: Multi-turn conversations, tool chaining, RAG with vector stores.
- **Phase 4**: Advanced integrations, custom models.
- **Phase 5**: Production deployment, monitoring.
