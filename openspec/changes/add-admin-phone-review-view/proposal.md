# Change: Add admin per-phone review view

## Why
- Improvement Plan calls out missing admin flow to view reviews by phone; current Node route is unused by the UI, lacks pagination guarantees, and Spring backend has no equivalent endpoint.
- Admin listing page only surfaces a truncated inline review preview, so admins cannot review full comment history or moderate visibility per phone reliably.

## What Changes
- Add an admin-protected per-phone review endpoint with consistent query params (page/limit/sort/visibility/search) and response shape across Node and Spring backends.
- Extend the admin UI (phone/listing detail or modal) to fetch reviews for a specific phone, render the full list with pagination, and wire existing hide/show/delete controls to refresh from the per-phone endpoint.
- Harden validation and error handling for invalid phone ids and unsupported query inputs so admins see clear messages instead of silent failures.

## Impact
- Affected specs: admin-phones
- Affected code: server adminReview routes/controller, Angular admin listing/detail UI + admin.service, spring-old-phone-deals AdminController/AdminService review handling