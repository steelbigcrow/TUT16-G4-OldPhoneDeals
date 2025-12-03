## ADDED Requirements
### Requirement: Admin can view reviews for a specific phone
Admin-protected per-phone review endpoints SHALL return and surface reviews for the phone with pagination, visibility, and search controls. Node backend serves GET `/api/admin/phones/{phoneId}/reviews`; Spring backend provides `/api/admin/reviews/phones/{phoneId}` (aliasing accepted) with matching params and response shape. Supported query params: page (default 1), limit (default 10), sortBy (`rating`|`createdAt`, default `createdAt`), sortOrder (`asc`|`desc`, default `desc`), visibility (`all`|`visible`|`hidden`, default `all`), and optional search over reviewer name or comment. Responses SHALL include `success`, `total`, `page`, `limit`, and `reviews` entries `{ reviewId, phoneId, phoneTitle, rating, comment, isHidden, createdAt, reviewerName, reviewerId? }`, and SHALL reject invalid inputs with clear errors and without partial review data.

#### Scenario: Paginate and filter phone reviews
- **WHEN** an authenticated admin calls the per-phone review endpoint with page/limit, sortBy=rating, sortOrder=asc, visibility=visible, and search matching reviewer name or comment for an existing phone
- **THEN** the API returns success=true with page/limit/total and a reviews array for that phone only sorted by rating ascending, excluding hidden reviews, with each entry containing reviewId, phoneId, phoneTitle, rating, comment, isHidden=false, createdAt, and reviewerName plus optional reviewerId.

#### Scenario: Reject invalid phone id
- **WHEN** the admin supplies a malformed or non-existent phoneId
- **THEN** the endpoint responds with success=false (or an error status) describing the issue and does not return review rows.

#### Scenario: Admin UI renders per-phone reviews and moderation actions
- **WHEN** an admin opens a phone’s review modal/section from the admin listings or phone detail view and adjusts search/visibility/sort or paginates
- **THEN** the UI calls the per-phone review endpoint with those params, renders rows with rating/comment/isHidden/reviewerName/createdAt, and uses existing hide/show/delete actions to refresh from that endpoint while surfacing any errors instead of stale data.
