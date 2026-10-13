# Release-Checkliste

- [ ] `python scripts/test.py` auf dem exakten Release-Commit ist gruen.
- [ ] `docs/evidence/test-run.txt` wurde neu erzeugt.
- [ ] Keine `.local`-Daten, Secrets, Kundendaten oder Private Keys sind getrackt.
- [ ] README behauptet keine volle MCP-Conformance.
- [ ] Feste Connector-Hub-Pfade und Host-Allowlist wurden reviewed.
- [ ] Realer Write-Provider beweist Idempotenz unter Retry/Concurrency.
- [ ] Produktions-Identity nutzt OAuth/OIDC + JWKS.
- [ ] Approval/Audit/Quota sind vor Horizontal Scaling durable.
- [ ] Native Browser Acceptance wurde bei relevanter UI-Aussage ausgeführt.
- [ ] Offizieller MCP-v2-Client wurde vor breiter Interoperabilitätsaussage ausgeführt.
