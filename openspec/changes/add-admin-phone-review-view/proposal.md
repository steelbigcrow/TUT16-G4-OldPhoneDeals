# Change: Add admin per-phone review view

## Why
- Improvement Plan calls out missing admin flow to view reviews by phone; current Node route is unused by the UI, lacks pagination guarantees, and Spring backend has no equivalent endpoint.
- Admin listing page only surfaces a truncated inline review preview, so admins cannot review full comment history or moderate visibility per phone reliably.

## What Changes
- Align the Node admin per-phone review endpoint so its pagination/sort/visibility/search contract is the parity target.
- Add a Spring Boot admin per-phone review endpoint with the same functionality and response shape while keeping a Spring-oriented path design (e.g., `/api/admin/reviews/phones/{phoneId}`) instead of cloning the Node path.
- Harden validation and error handling for invalid phone ids and unsupported query inputs so admins see clear messages instead of silent failures.

## Impact
- Affected specs: admin-phones
- Affected code: server adminReview routes/controller, spring-old-phone-deals AdminController/AdminService review handling
