# MCP protocol boundary

This release targets the modern MCP `2026-07-28` era for a deliberately small tool-only surface.

Implemented methods: `server/discover`, `tools/list`, `tools/call`.

Implemented modern behaviors include per-request `_meta`, `MCP-Protocol-Version` validation, `Mcp-Method` consistency and `Mcp-Name` binding for tool calls. Discover/list results include `resultType`, `ttlMs` and `cacheScope`; call results include `resultType`. `tools/list` uses `private` cache scope because authorization changes the visible catalog.

The server publishes protected-resource metadata at `/.well-known/oauth-protected-resource/mcp` (plus the root mirror used by this demo) and authentication challenges point clients at the metadata URL.

## Intentionally not implemented

Legacy `initialize`, prompts, resources, tasks, subscriptions, SSE/streaming, dynamic client registration, a full OAuth authorization server, and the complete MCP conformance surface.

The included official TypeScript v2 client workflow pins `2026-07-28`; it is a manual release gate and is not reported as locally executed.
