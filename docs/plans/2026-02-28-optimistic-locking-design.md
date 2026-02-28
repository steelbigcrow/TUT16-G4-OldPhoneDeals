# Optimistic Locking (Phone Writes) Design

**Date:** 2026-02-28

## Goal

Add an optimistic locking mechanism to the Spring Boot backend to prevent lost updates when multiple requests update the same Phone record concurrently.

This design focuses on **Phone write operations** (seller and admin updates). Other aggregates (Order/Cart/User) are out of scope.

## Scope

- Seller Phone update:
  - `PUT /api/phones/{phoneId}`
  - `PUT /api/phones/{phoneId}/disable`
- Admin Phone update:
  - `PUT /api/admin/phones/{phoneId}`
  - `PUT /api/admin/phones/{phoneId}/toggle-disabled` (no request body, optimistic lock is handled via save failure)

## Approach (Chosen)

### Data Model

- Add `@Version` field to `Phone` (`Long version`).
- This enables Spring Data MongoDB optimistic locking on repository `save(...)`.

### API Contract

- Expose `version` in Phone response DTOs so clients/tests can read it:
  - `PhoneResponse.version`
  - `PhoneListItemResponse.version`
  - `PhoneManagementResponse.version`

- Accept optional `version` on update requests:
  - `PhoneUpdateRequest.version` (seller update)
  - `TogglePhoneStatusRequest.version` (seller disable/enable)
  - `UpdatePhoneRequest.version` (admin update)

**Compatibility:** all `version` fields are optional. Existing frontend requests that do not send version remain valid.

### Conflict Detection Rules

- If the request provides `version` and it differs from the current document version:
  - Fail fast with HTTP **409 CONFLICT**.
- If a concurrent update happens between read and save, Spring Data throws `OptimisticLockingFailureException`:
  - Map to HTTP **409 CONFLICT** as well.

### Error Mapping

- Introduce `VersionConflictException` for consistent 409 semantics.
- Global mapping in `GlobalExceptionHandler`:
  - `VersionConflictException` -> 409
  - `OptimisticLockingFailureException` -> 409

## Testing Strategy

- Unit tests:
  - Service-level stale version check returns `VersionConflictException`.
  - Repository optimistic-lock failure is mapped to `VersionConflictException`.

- Integration tests:
  - Use Testcontainers MongoDB and verify saving a stale entity instance triggers `OptimisticLockingFailureException`.

- Playwright E2E:
  - API-only test flow using seeded `/api/e2e/reset` data.
  - Update Phone once with current `version` (200), update again using stale `version` (409).

