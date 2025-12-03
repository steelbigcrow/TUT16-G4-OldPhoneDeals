## Context
- Improvement Plan highlights missing admin ability to view reviews by product; Node backend has a basic `/api/admin/phones/{phoneId}/reviews` without pagination/search, and Spring backend lacks a per-phone review endpoint entirely.
- We need Spring Boot to expose the same per-phone review capabilities as the Node server while keeping its own endpoint style.

## Goals / Non-Goals
- Goals: expose a per-phone review endpoint with pagination/sort/visibility/search, keep response shape consistent across Node and Spring, and allow Spring to keep a Spring-oriented path while matching functionality.
- Non-Goals: redesign global review management, change the review schema, add new analytics/aggregate stats, or modify the frontend.

## Decisions
- Endpoint shape: require admin auth; support `page`, `limit`, `sortBy` (`rating`|`createdAt`), `sortOrder` (`asc`|`desc`), optional `visibility` (`visible`|`hidden`|`all`), and `search` over reviewer name/comment. Default sort is `createdAt desc`; invalid params fall back to defaults.
- Path alignment: Node continues to serve `/api/admin/phones/{phoneId}/reviews`; Spring adds `/api/admin/reviews/phones/{phoneId}` (aliasing accepted) so both backends return the same payload `{ success, total, page, limit, reviews: [{ reviewId, phoneId, phoneTitle, rating, comment, isHidden, createdAt, reviewerName, reviewerId? }] }` while letting Spring preserve its path conventions.

## Risks / Trade-offs
- Duplicate review data fetches if clients call both global and per-phone endpoints; mitigate by keeping the per-phone endpoint paginated and reserving it for moderation use.
- Inconsistent review shapes between backends; mitigated by explicitly normalizing fields (`reviewId`, `isHidden` boolean) and defaults in the API layer.

## Migration Plan
- Implement/align backend endpoints and response normalization first, validate against both Node and Spring backends, and keep existing global review management untouched to avoid regressions.
