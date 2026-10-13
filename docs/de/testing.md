# Testing und Evidenz

```bash
python scripts/test.py
```

Der deterministische Release-Gate kompiliert Java 21 mit `-Xlint:all -Werror`, führt **26 Java Contract Assertions**, **40 echte HTTP/MCP Security-Szenarien** und **11 Repository-Integrity-Checks** aus. Gesamt: **77**.

Exakte Ausgabe: `docs/evidence/test-run.txt`.

Native Browser Acceptance:

```bash
python -m pip install -r requirements-browser.txt
python -m playwright install chromium
python scripts/test.py --browser
```

Die Authoring-Umgebung blockiert native Browser-Navigation; dieser Test ist daher ein manueller GitHub-Gate und wird nicht als lokal bestanden behauptet.

Ein zweiter manueller Workflow nutzt den offiziellen stabilen `@modelcontextprotocol/client` v2, pinnt `2026-07-28`, listet Tools und ruft `orders.lookup` auf. Auch dieser Gate ist nicht Teil der lokalen 77.
