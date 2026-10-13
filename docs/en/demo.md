# Five-minute recruiter demo

1. Start the gateway and show a scope-filtered tool catalog.
2. Read `ORD-A100` as an ACME operator.
3. Attempt another tenant's `ORD-B200`; show generic `not_found`.
4. Attempt `sql.execute`, a `url` field and a tenant field; show each denial.
5. Call `orders.cancel`; show the pending approval and explain that no write occurred.
6. Open `/admin`, use the separate reviewer token and approve the exact request.
7. Tamper with the cancellation reason; show `approval_binding_mismatch`.
8. Execute with the exact approved arguments; show the receipt.
9. Retry the exact call; show the same logical receipt/no second effect.
10. Show the audit chain and the 77-check evidence.

Recruiter message: **AI can select an authorized capability, but policy owns identity, tenancy, destinations and business authority.**
