# Exakte Freigabebindung

Eine Write-Freigabe ist keine allgemeine Berechtigung. Sie bindet Tenant, Requester, Tool, unveränderliche Tool-Revision, SHA-256-Fingerprint der kanonischen Business-Argumente, Ablaufzeit, unabhängigen Reviewer und Begründung.

Bei der Ausführung werden alle Bindungsfelder erneut geprüft. Andere Argumente, andere Revision, anderer Requester oder Tenant sowie Ablauf führen zu Deny. Self-Approval ist verboten.

Die Approval-ID dient als Upstream-Idempotency-Key. Ein realer Connector-Hub-Write-Provider muss diese Idempotenz selbst garantieren, bevor echte Business-Effekte freigeschaltet werden.
