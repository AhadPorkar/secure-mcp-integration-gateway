# Testing and evidence

## Deterministic release gate

Run:

```bash
python scripts/test.py
```

It compiles Java 21 with `-Xlint:all -Werror`, executes **26 Java contract assertions**, starts the real HTTP gateway for **40 security/MCP attack scenarios**, then runs **11 repository integrity checks**. Total: **77 verified checks**.

The captured output is `docs/evidence/test-run.txt`.

## Native browser acceptance

A Playwright suite exercises the reviewer console, CSP behavior, independent approval and token non-persistence. Run it only in an environment that permits browser navigation to the local test service:

```bash
python -m pip install -r requirements-browser.txt
python -m playwright install chromium
python scripts/test.py --browser
```

The authoring environment blocks native browser navigation by policy, so this suite is not included in the local passed count. The repository includes a manual GitHub workflow named **Optional native browser acceptance**.

## Official MCP v2 client gate

The second manual GitHub workflow installs the stable `@modelcontextprotocol/client` v2 package, pins protocol `2026-07-28`, connects over Streamable HTTP, lists tools and calls `orders.lookup` with a static bearer token.

It is intentionally separate and is not claimed as executed in the authoring environment. Run it before advertising broad official-SDK interoperability.
