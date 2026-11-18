# MCP server (LAN control surface)

The mod hosts a **Model Context Protocol** server, so an external agent can connect over the network
and drive the bot through the same levers as the Python (py4j) gateway. MCP wraps
`Py4jEntryPoint` — one source of truth, no duplicated logic (see
[AGENT_PY4J_LEVERS.md](AGENT_PY4J_LEVERS.md)).

## What it is

- Transport: **Streamable HTTP**, JSON-RPC 2.0, a single endpoint `POST /mcp`.
- Implementation: `com.sun.net.httpserver` (built into the JDK, no extra dependency),
  `adris.altoclef.mcp.McpServer`. It binds `0.0.0.0`, so it is reachable on the LAN.
- Methods: `initialize`, `tools/list`, `tools/call`, `ping`.
- Tools: a curated set of levers — perception, movement, combat, building + WorldEdit, protection,
  menus, and commands — each with a description and a JSON schema.

## Settings (`altoclef_settings.json`)

| Field | Default | Meaning |
|---|---|---|
| `mcpEnabled` | `false` | Opt-in. Turn it on to host the server. |
| `mcpPort` | `25350` | Port (bound on `0.0.0.0`). |
| `mcpAuthToken` | generated | Bearer secret, minted (a UUID) and saved on first start. Every request needs `Authorization: Bearer <token>`, otherwise a plain `401`. |

When enabled it starts automatically after the py4j gateway. The startup log line is
`MCP server started on 0.0.0.0:25350`.

## Connect an agent

Endpoint: `http://<bot-lan-ip>:25350/mcp` (for example `http://192.168.1.20:25350/mcp`).

Claude Code (HTTP transport):

```bash
claude mcp add --transport http unionclef http://192.168.1.20:25350/mcp \
  --header "Authorization: Bearer <mcpAuthToken>"
```

Or in `.mcp.json`:

```json
{ "mcpServers": { "unionclef": { "type": "http", "url": "http://192.168.1.20:25350/mcp",
    "headers": { "Authorization": "Bearer <mcpAuthToken>" } } } }
```

## Verify by hand

```bash
curl -s http://127.0.0.1:25350/mcp -H "Authorization: Bearer <mcpAuthToken>" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## Adding a tool

A new lever is one method on `Py4jEntryPoint` plus one `tool(...)` line in
`McpServer.registerTools()` (name, description, `schema(...)`, and a lambda that calls the method).
Keep it a thin wrapper, and keep the py4j catalog and this list in sync.
