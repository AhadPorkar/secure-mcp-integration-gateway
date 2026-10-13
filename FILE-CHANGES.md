# File changes - Secure MCP Integration Gateway 1.0.0-rc.1

## Delivery model

This delivery is a **new standalone repository / sidecar**. It does not overwrite the existing `connector-hub` repository.

- Existing Connector Hub files modified: **0**.
- Existing Connector Hub files deleted: **0**.
- Files in the final standalone source tree: **91**.

The `integration/connector-hub/` directory contains mapping and handoff examples only.

## Created files

### `.env.example`

- `.env.example`

### `.github`

- `.github/workflows/browser-acceptance.yml`
- `.github/workflows/ci.yml`
- `.github/workflows/mcp-sdk-acceptance.yml`

### `.gitignore`

- `.gitignore`

### `CHANGELOG.md`

- `CHANGELOG.md`

### `Dockerfile`

- `Dockerfile`

### `LICENSE`

- `LICENSE`

### `README.de.md`

- `README.de.md`

### `README.md`

- `README.md`

### `SECURITY.md`

- `SECURITY.md`

### `START-HERE.md`

- `START-HERE.md`

### `docs`

- `docs/de/README.md`
- `docs/de/approval-binding.md`
- `docs/de/architecture.md`
- `docs/de/attack-matrix.md`
- `docs/de/configuration.md`
- `docs/de/connector-hub-integration.md`
- `docs/de/demo.md`
- `docs/de/mcp-protocol.md`
- `docs/de/operations.md`
- `docs/de/release-checklist.md`
- `docs/de/security.md`
- `docs/de/sources.md`
- `docs/de/tenant-isolation.md`
- `docs/de/testing.md`
- `docs/de/threat-model.md`
- `docs/en/README.md`
- `docs/en/approval-binding.md`
- `docs/en/architecture.md`
- `docs/en/attack-matrix.md`
- `docs/en/configuration.md`
- `docs/en/connector-hub-integration.md`
- `docs/en/demo.md`
- `docs/en/mcp-protocol.md`
- `docs/en/operations.md`
- `docs/en/release-checklist.md`
- `docs/en/security.md`
- `docs/en/sources.md`
- `docs/en/tenant-isolation.md`
- `docs/en/testing.md`
- `docs/en/threat-model.md`
- `docs/evidence/reviewer-console-preview.png`
- `docs/evidence/test-run.txt`
- `docs/evidence/verification.json`

### `integration`

- `integration/connector-hub/README.md`
- `integration/connector-hub/provider-mapping.example.json`

### `pom.xml`

- `pom.xml`

### `publishing`

- `publishing/LinkedIn_Secure_MCP_Gateway_DE.pdf`
- `publishing/LinkedIn_Secure_MCP_Gateway_EN.pdf`
- `publishing/Secure_MCP_Integration_Gateway_Release_DE.pdf`
- `publishing/Secure_MCP_Integration_Gateway_Release_EN.pdf`
- `publishing/cv-bullets-de.md`
- `publishing/cv-bullets-en.md`
- `publishing/linkedin-article-de.md`
- `publishing/linkedin-article-en.md`
- `publishing/linkedin-post-de.md`
- `publishing/linkedin-post-en.md`
- `publishing/release-brief-de.md`
- `publishing/release-brief-en.md`

### `requirements-browser.txt`

- `requirements-browser.txt`

### `scripts`

- `scripts/build.py`
- `scripts/issue_demo_token.py`
- `scripts/repository_checks.py`
- `scripts/run_demo.py`
- `scripts/test.py`

### `src`

- `src/main/java/io/ahad/mcpgateway/AdminAssets.java`
- `src/main/java/io/ahad/mcpgateway/AdminPage.java`
- `src/main/java/io/ahad/mcpgateway/ApprovalStore.java`
- `src/main/java/io/ahad/mcpgateway/AuditLedger.java`
- `src/main/java/io/ahad/mcpgateway/ConnectorHubClient.java`
- `src/main/java/io/ahad/mcpgateway/Crypto.java`
- `src/main/java/io/ahad/mcpgateway/DemoConnectorHubClient.java`
- `src/main/java/io/ahad/mcpgateway/DemoTokens.java`
- `src/main/java/io/ahad/mcpgateway/GatewayException.java`
- `src/main/java/io/ahad/mcpgateway/GatewayServer.java`
- `src/main/java/io/ahad/mcpgateway/HttpConnectorHubClient.java`
- `src/main/java/io/ahad/mcpgateway/Json.java`
- `src/main/java/io/ahad/mcpgateway/JwtVerifier.java`
- `src/main/java/io/ahad/mcpgateway/Main.java`
- `src/main/java/io/ahad/mcpgateway/QuotaLimiter.java`
- `src/main/java/io/ahad/mcpgateway/SecurityContext.java`
- `src/main/java/io/ahad/mcpgateway/ToolDefinition.java`
- `src/main/java/io/ahad/mcpgateway/ToolRegistry.java`
- `src/main/java/io/ahad/mcpgateway/ToolService.java`
- `src/test/java/io/ahad/mcpgateway/TestMain.java`

### `tests`

- `tests/attack_matrix.py`
- `tests/browser_test.py`
- `tests/mcp_sdk_smoke.mjs`

### Release integrity files

- `FILE-CHANGES.md`
- `MANIFEST.sha256`
