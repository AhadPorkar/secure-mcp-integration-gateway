# MCP-Protokollgrenze

Der RC implementiert bewusst nur einen modernen MCP-`2026-07-28` Tool-Teilumfang: `server/discover`, `tools/list`, `tools/call`, per-request `_meta`, MCP Protocol/Method/Name Header-Bindung, aktuelle `resultType`/`ttlMs`/`cacheScope`-Felder und Protected Resource Metadata.

`tools/list` verwendet `private` Cache Scope, weil Authorization die sichtbaren Tools beeinflusst.

Nicht implementiert: Legacy `initialize`, Prompts, Resources, Tasks, Subscriptions, SSE/Streaming, DCR, vollständiger Authorization Server und komplette MCP-Conformance. Der offizielle v2 Client ist ein separates manuelles Release-Gate.
