## ADDED Requirements
### Requirement: Admin can view reviews for a specific phone
Admin-protected per-phone review endpoints SHALL return reviews for the phone with pagination, visibility, and search controls. Node backend serves GET `/api/admin/phones/{phoneId}/reviews`; Spring backend provides `/api/admin/reviews/phones/{phoneId}` (aliasing accepted) with matching params and response shape while keeping its own path style. Supported query params: page (default 1), limit (default 10), sortBy (`rating`|`createdAt`, default `createdAt`), sortOrder (`asc`|`desc`, default `desc`), visibility (`all`|`visible`|`hidden`, default `all`), and optional search over reviewer name or comment. Responses SHALL include `success`, `total`, `page`, `limit`, and `reviews` entries `{ reviewId, phoneId, phoneTitle, rating, comment, isHidden, createdAt, reviewerName, reviewerId? }`, and SHALL reject invalid inputs with clear errors and without partial review data.

#### Scenario: Paginate and filter phone reviews
- **WHEN** an authenticated admin calls the per-phone review endpoint with page/limit, sortBy=rating, sortOrder=asc, visibility=visible, and search matching reviewer name or comment for an existing phone
- **THEN** the API returns success=true with page/limit/total and a reviews array for that phone only sorted by rating ascending, excluding hidden reviews, with each entry containing reviewId, phoneId, phoneTitle, rating, comment, isHidden=false, createdAt, and reviewerName plus optional reviewerId.

#### Scenario: Reject invalid phone id
- **WHEN** the admin supplies a malformed or non-existent phoneId
- **THEN** the endpoint responds with success=false (or an error status) describing the issue and does not return review rows.

#### Scenario: Spring endpoint matches Node contract
- **WHEN** an authenticated admin calls the Spring per-phone review endpoint (e.g., `/api/admin/reviews/phones/{phoneId}`) with default or explicit pagination/sort/visibility params for an existing phone
- **THEN** the API returns success=true with the same keys and field shapes as the Node endpoint (success, total, page, limit, reviews entries with reviewId/phoneId/phoneTitle/rating/comment/isHidden/createdAt/reviewerName/reviewerId?) regardless of the differing path.
