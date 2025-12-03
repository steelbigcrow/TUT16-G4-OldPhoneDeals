## 1. Admin per-phone review API parity baseline (Node)
- [ ] 1.1 Confirm the Node GET `/api/admin/phones/{phoneId}/reviews` supports pagination, sort (rating|createdAt), visibility/search filters, normalized response shape, and invalid-id handling as the parity target; adjust if gaps remain.

## 2. Spring Boot per-phone review endpoint with Spring path
- [ ] 2.1 Implement a Spring Boot admin per-phone review endpoint (e.g., `/api/admin/reviews/phones/{phoneId}` with optional alias) matching Node functionality, params/defaults, and validation while keeping Spring-specific routing.
- [ ] 2.2 Normalize the Spring response to the Node contract (`success`, `total`, `page`, `limit`, `reviews[{reviewId, phoneId, phoneTitle, rating, comment, isHidden, createdAt, reviewerName, reviewerId?}]`) and ensure invalid phone ids return clear errors without partial data.

## 3. Validation
- [ ] 3.1 Run `openspec validate add-admin-phone-review-view --strict`.
- [ ] 3.2 Manual check: Node and Spring per-phone endpoints return the same payload shape for happy path and invalid phone ids; no frontend changes are introduced.
