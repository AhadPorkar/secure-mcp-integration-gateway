# Start here - Secure MCP Integration Gateway 1.0.0-rc.1

## 1. Verify prerequisites

```text
java -version   # JDK 21 or newer
javac -version  # JDK 21 or newer
python --version # 3.10+
```

## 2. Run the deterministic release gate

```bash
python scripts/test.py
```

The captured RC evidence is 26 Java contracts + 40 live HTTP/MCP attack scenarios + 11 repository/release checks = **77 passed deterministic checks**.

Do not publish a green CI badge until the run on your own GitHub commit is green.

## 3. Run the demo

```bash
python scripts/run_demo.py
```

Open `http://127.0.0.1:8096/admin`. Demo tokens are written to `.local/access.json`; never commit that directory.

## 4. Optional acceptance gates

Native reviewer-console browser test:

```bash
python -m pip install -r requirements-browser.txt
python -m playwright install chromium
python scripts/test.py --browser
```

GitHub also contains two manual workflows: **Optional native browser acceptance** and **Optional official MCP v2 acceptance**. They are deliberately separate from the deterministic core gate.

## 5. Read before real Connector Hub integration

- `docs/en/connector-hub-integration.md`.
- `docs/en/security.md`.
- `docs/en/threat-model.md`.
- `docs/en/release-checklist.md`.
- `integration/connector-hub/provider-mapping.example.json`.

## 6. Release status

Suggested first tag: `v1.0.0-rc.1`.

It is a release candidate. It does not claim full MCP conformance, production identity readiness, native-browser acceptance, official SDK acceptance, or live Connector Hub provider acceptance until you run those explicit gates.
