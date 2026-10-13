# Security-Modell

Der Demo-Verifier prüft Issuer, Audience, Ablauf, Subject und Tenant. Produktion muss ihn durch OAuth/OIDC + vertrauenswürdiges JWKS ersetzen.

Der Connector-Hub-Client besitzt ein eigenes Service Credential; Caller-`Authorization` wird nie weitergereicht. Tenant stammt nur aus dem verifizierten Security Context. Tool-Schemas sind geschlossen und enthalten weder Tenant noch URL/SQL/Credential-Felder.

Approval bindet Tenant, Requester, Tool-Revision und kanonischen Args-Hash. Self-Approval ist verboten; Ablauf wird bei Execution geprüft. Die Approval-ID wird als Upstream-Idempotency-Key verwendet.

Die Reviewer-Konsole nutzt strikte CSP, `textContent` statt dynamischem `innerHTML` und keine Token-Persistenz in Local/Session Storage.

Audit-Einträge sind SHA-256-verkettet und speichern Detail-Hashes statt roher Business-Argumente. Das ist Tamper Evidence, kein unveränderliches Compliance-Archiv.
