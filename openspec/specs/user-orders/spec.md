# user-orders Specification

## Purpose
TBD - created by archiving change add-user-order-history-pagination. Update Purpose after archive.
## Requirements
### Requirement: Spring API returns paginated user order history
The system SHALL provide `GET /api/orders` (auth required) that infers the user from the JWT and returns `ApiResponse` data with `items` (OrderResponse list) and `pagination` metadata (`currentPage`, `pageSize`, `totalPages`, `totalItems`). Requests default to `page=1` and `pageSize=10` and sort by `createdAt` descending; invalid page inputs fall back to defaults or return validation errors instead of empty data.

#### Scenario: Default page retrieval
- **WHEN** an authenticated user calls `GET /api/orders` without query params
- **THEN** the response has `success=true`, `data.items` contains the newest orders first (up to 10), and `data.pagination` reports `currentPage=1`, `pageSize=10`, and `totalPages/totalItems` reflecting the user’s total orders.

#### Scenario: Specific page retrieval
- **WHEN** an authenticated user with more than five orders calls `GET /api/orders?page=2&pageSize=5`
- **THEN** the response returns at most five orders from positions 6-10 in descending createdAt order with `data.pagination.currentPage=2`, `pageSize=5`, and `totalPages` computed from the total orders.

### Requirement: React order history page renders paginated results
The React frontend SHALL expose an authenticated route `/orders` that lists the user’s orders using the Spring `GET /api/orders` contract, shows created date, totals, and item summaries per order, and provides pagination controls that drive `page`/`pageSize` queries. The page SHALL surface loading, empty, and error states instead of silently failing when the API denies access or returns no data.

#### Scenario: Paginate order history in the UI
- **WHEN** a signed-in user with more orders than one page visits `/orders` and clicks the next-page control
- **THEN** the UI requests the subsequent page from `GET /api/orders` with an incremented `page`, updates the order list and pagination indicators to match the response metadata, and preserves loading/error feedback during the fetch.

