# Tenant isolation

Tenant is an identity property, not model input.

Layered controls:

1. Verified token yields one tenant.
2. Tool schemas contain no tenant parameter.
3. Tool service always passes the verified tenant to the upstream adapter.
4. Approval records are tenant-bound.
5. Reviewer endpoints show only the reviewer's tenant.
6. Cross-tenant approval IDs and business identifiers are not disclosed as valid objects.
7. Audit queries are tenant-scoped.

The attack suite verifies order isolation, document isolation, approval review isolation, execution isolation and tenant-argument injection rejection.
