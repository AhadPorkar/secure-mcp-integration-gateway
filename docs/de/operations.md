# Betrieb

Lokal startet `python scripts/run_demo.py` den Gateway auf Loopback und schreibt Demo-Tokens nach `.local/access.json`.

Produktion benötigt TLS, OIDC/OAuth + vertrauenswürdiges JWKS, durable Shared Stores für Approval/Audit/Quota, Credential Rotation, Egress-Kontrolle, Metrics/Tracing, Backup/Restore und unabhängigen Security Review.

Der RC ist bewusst Single-Process. Mehrere Replikate sind erst sinnvoll, wenn State und Idempotenz auf shared/durable Infrastruktur migriert wurden.
