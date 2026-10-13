# Mandantentrennung

Tenant ist Identity-Eigenschaft und kein Modellargument. Token-Validierung liefert genau einen Tenant; Tool-Schemas besitzen kein Tenant-Feld; Upstream-Aufrufe erhalten nur den verifizierten Tenant; Approval, Reviewer-Abfragen und Audit sind tenant-gebunden.

Die Angriffsmatrix prüft Order-, Dokument-, Approval- und Execution-Isolation sowie direkte Tenant-Injection.
