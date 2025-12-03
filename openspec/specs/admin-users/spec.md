# admin-users Specification

## Purpose
TBD - created by archiving change add-admin-user-resources. Update Purpose after archive.
## Requirements
### Requirement: Admin can list a user's selling phones with filters
Admin-protected GET `/api/admin/users/{userId}/phones` SHALL return only phones sold by that user. Query params SHALL support page (default 1), limit (default 10), sortBy (createdAt|price|stock, default createdAt), sortOrder (asc|desc, default desc), and optional brand filter (ignored when set to "all"). The API SHALL validate userId format/existence and reject invalid pagination inputs. Responses SHALL include `success`, `total` for the filtered set, `page`, `limit`, and `phones` with `_id`, title, brand, price, stock, isDisabled, createdAt, updatedAt, averageRating, and reviewsCount.

#### Scenario: Paginate and filter listings
- **WHEN** an admin calls GET `/api/admin/users/{userId}/phones` with page=2, limit=5, sortBy=price, sortOrder=asc, and brand=Apple for an existing user
- **THEN** the response returns success=true with page=2, limit=5, total equal to the user's Apple listing count, and `phones` sorted by price ascending containing only that user's phones with the specified summary fields including rating and review counts.

#### Scenario: Reject invalid input
- **WHEN** the admin supplies a malformed userId or page/limit less than 1
- **THEN** the API responds with success=false (or an error status) describing the invalid input and omits the phone list.

### Requirement: Admin can list a user's submitted reviews across phones
Admin-protected GET `/api/admin/users/{userId}/reviews` SHALL return reviews authored by the user with pagination. Query params SHALL support page (default 1), limit (default 10), sortBy (`rating`|`createdAt`, default createdAt), sortOrder (asc|desc, default desc), and optional brand filter that limits to the phone brand (ignored when "all"). The API SHALL validate userId existence and pagination/sort inputs. Responses SHALL include `success`, `total`, `page`, `limit`, and `reviews` entries containing reviewId, phoneId, phoneTitle, phoneBrand, phonePrice, phoneStock, phone averageRating, phone reviewsCount, reviewRating, reviewComment, reviewCreatedAt, and isHidden status.

#### Scenario: Sort and filter reviews
- **WHEN** an admin requests `/api/admin/users/{userId}/reviews` with brand=Samsung, sortBy=rating, sortOrder=desc, page=1, and limit=3 for an existing user
- **THEN** the response returns success=true with total equal to the user's Samsung review count, page/limit metadata, and reviews sorted by rating descending containing the phone context and review fields including isHidden.

#### Scenario: Reject invalid user or pagination
- **WHEN** the admin calls the endpoint with a non-existent userId or limit < 1
- **THEN** the API responds with success=false (or an error) indicating the validation failure without returning review data.

### Requirement: Admin UI exposes user-linked phones and reviews
The admin user management view SHALL let an admin open a user's selling phones modal and reviews modal, drive pagination, sorting, and brand filtering, and surface load errors without blocking other admin actions.

#### Scenario: Browse selling phones with controls
- **WHEN** an admin opens the selling phones modal for a user and adjusts sort or brand filter or changes pages
- **THEN** the UI issues calls to `/api/admin/users/{userId}/phones` with the chosen parameters, updates rows (title, brand, price, stock, averageRating, reviewsCount), and shows pagination reflecting the API metadata while handling failures with user-facing errors.

#### Scenario: Browse reviews with controls
- **WHEN** an admin opens the reviews modal and switches between rating/createdAt sorting, filters by brand, or paginates
- **THEN** the UI calls `/api/admin/users/{userId}/reviews` accordingly and renders each review with phone title/brand/price, average rating, review rating/comment/createdAt, showing errors instead of stale data if requests fail.

