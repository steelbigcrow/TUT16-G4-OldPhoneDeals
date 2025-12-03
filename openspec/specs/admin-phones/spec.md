# admin-phones Specification

## Purpose
TBD - created by archiving change add-admin-phone-detail. Update Purpose after archive.
## Requirements
### Requirement: Admin can retrieve phone detail by id
Admin-protected GET `/api/admin/phones/{phoneId}` SHALL return a single phone with `_id`, title, brand, image URL, price, stock, isDisabled, createdAt, and updatedAt plus seller `{ _id, firstName, lastName, email }`. Invalid or missing IDs SHALL return a failure response without partial phone data.

#### Scenario: Return phone detail with seller info
- **WHEN** an authenticated admin calls GET `/api/admin/phones/{phoneId}` for an existing phone
- **THEN** the response body includes success=true and the phone fields `_id`, title, brand, image, price, stock, isDisabled, createdAt, updatedAt, and seller `{ _id, firstName, lastName, email }`.

#### Scenario: Reject invalid phone id
- **WHEN** an admin calls GET `/api/admin/phones/{phoneId}` with an invalid or non-existent id
- **THEN** the API responds with success=false (or equivalent error status) and a descriptive error message without returning a phone object.

### Requirement: Admin can view phone detail page with status controls
The admin frontend SHALL provide a protected route (e.g., `/admin/listings/:phoneId`) that fetches the admin phone detail endpoint, displays the phone’s title, brand, image, price, stock, status (enabled/disabled), createdAt/updatedAt, and seller name/email, and offers enable/disable plus delete actions with confirmations and user-facing notifications.

#### Scenario: View and manage a phone from its detail page
- **WHEN** an authenticated admin opens the phone detail route for a valid phone id
- **THEN** the page shows the phone fields, seller name/email, and status, and the admin can trigger enable/disable or delete actions that call the admin phone APIs, confirm before proceeding, and reflect success or failure via updated UI and notifications.

#### Scenario: Surface detail fetch errors
- **WHEN** the detail fetch fails (e.g., invalid id or server error)
- **THEN** the page surfaces an error message and a way to return to the listings view without showing stale or partial data.

