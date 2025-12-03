# Change: Admin user-associated resources

## Why
- Improvement Plan.md lists admin user-linked resources (selling phones and reviews) as still pending and needs clarity before implementation.
- Backend and admin UI already have partial endpoints/modals; a spec is required to align behavior across Node/Spring APIs and the Angular admin experience.

## What Changes
- Define a new admin-users capability covering admin-protected endpoints to fetch a user's selling phones and submitted reviews with pagination, sorting, brand filters, and validation rules.
- Capture required response metadata and fields for phone summaries and review context, including visibility/hidden state and rating aggregates.
- Specify admin UI flows for viewing a user's phones and reviews with sorting/filter/pagination controls and error handling.

## Impact
- Affected specs: admin-users (new)
- Affected code: server admin user routes/controllers, Angular admin user management views/services, and Spring admin user endpoints for parity.
