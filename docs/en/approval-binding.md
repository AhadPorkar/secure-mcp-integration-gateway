# Exact approval binding

A write approval is a one-purpose authorization record, not a generic permission to cancel an order.

The record binds: tenant, original requester, tool name, immutable tool revision, SHA-256 fingerprint of canonical business arguments, expiration time, independent reviewer, and review reason.

`approvalId` is deliberately excluded from the argument fingerprint. During execution the gateway reconstructs the canonical business arguments and checks every binding field again.

## What invalidates an approval

- another tenant;
- another requester;
- another tool;
- another tool revision;
- any change to an approved business argument;
- expiration;
- missing independent approval.

Self-approval is blocked. Cross-tenant approval lookup is hidden. An approval cannot be changed from pending to approved twice.

## Retry semantics

The approval identifier becomes the upstream idempotency key. A lost response can therefore be retried with the exact same approved arguments. The demo upstream stores one receipt per idempotency key. A production Connector Hub write provider must provide equivalent idempotency before this pattern is used for a real business effect.
