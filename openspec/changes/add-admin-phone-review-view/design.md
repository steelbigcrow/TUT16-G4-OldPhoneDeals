## Context
- Improvement Plan highlights missing admin ability to view reviews by product; Node backend has a basic `/api/admin/phones/{phoneId}/reviews` without pagination/search, and Spring backend lacks a per-phone review endpoint entirely.
- Admin UI only shows a truncated inline review snippet in the listings table and the review management page lists all reviews without a per-phone focus, so moderation per phone is cumbersome.

## Goals / Non-Goals
- Goals: expose a per-phone review endpoint with pagination/sort/visibility/search, keep response shape consistent across Node and Spring, and surface a phone-level review view in the admin UI with hide/show/delete actions wired to refresh from that endpoint.
- Non-Goals: redesign global review management, change the review schema, or add new analytics/aggregate stats.

## Decisions
- Endpoint shape: require admin auth; support `page`, `limit`, `sortBy` (`rating`|`createdAt`), `sortOrder` (`asc`|`desc`), optional `visibility` (`visible`|`hidden`|`all`), and `search` over reviewer name/comment. Default sort is `createdAt desc`; invalid params fall back to defaults.
- Path alignment: Node continues to serve `/api/admin/phones/{phoneId}/reviews`; Spring adds `/api/admin/reviews/phones/{phoneId}` with optional alias/forwarder so both backends return the same payload `{ success, total, page, limit, reviews: [{ reviewId, phoneId, phoneTitle, rating, comment, isHidden, createdAt, reviewerName, reviewerId? }] }`.
- UI integration: reuse the admin listing/phone detail view to open a modal/section that fetches from the per-phone endpoint, with filters (search, visibility, sort) and pagination; hide/show/delete actions reuse existing admin review endpoints and refresh the list on completion.

## Risks / Trade-offs
- Duplicate review data fetches (listings already include small review slices); mitigate by fetching full reviews only when the admin opens the per-phone view and paginating results.
- Inconsistent review shapes between backends; mitigated by explicitly normalizing fields (`reviewId`, `isHidden` boolean) and defaults in the API layer.

## Migration Plan
- Implement/align backend endpoints and response normalization first, then wire frontend modal to new API, finally validate against both Node and Spring backends.
- Keep existing global review management untouched to avoid regressions while adding per-phone view.
