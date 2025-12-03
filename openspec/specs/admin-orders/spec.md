# admin-orders Specification

## Purpose
TBD - created by archiving change add-admin-order-export-filters. Update Purpose after archive.
## Requirements
### Requirement: Admin orders support advanced filtering and sorting
Admin order listing SHALL accept page and pageSize with filters userId, startDate, endDate, searchTerm, brandFilter, and optional sortBy/sortOrder. Filtering SHALL match searchTerm against buyer full name/email and order item titles, and brandFilter SHALL perform a case-insensitive match against any order item title. Default sortBy is createdAt with sortOrder desc; invalid sort inputs fall back to createdAt desc. Responses SHALL include pagination metadata reflecting the filtered set.

#### Scenario: Filter by buyer search term and brand filter
- **WHEN** an admin calls GET /api/admin/orders with searchTerm matching the buyer name or email and brandFilter matching any item title plus page/pageSize
- **THEN** only orders satisfying both filters are returned, sorted by createdAt desc by default with pagination metadata (currentPage, totalPages, totalItems).

#### Scenario: Sort by total amount ascending
- **WHEN** an admin supplies sortBy=totalAmount and sortOrder=asc with any filters
- **THEN** the response orders results by totalAmount ascending while retaining applied filters and pagination metadata.

### Requirement: Admin orders can be exported in CSV or JSON
The system SHALL provide GET /api/admin/orders/export (admin-protected) that exports orders filtered by userId, startDate, endDate, searchTerm, and brandFilter and sorted by sortBy/sortOrder. The format query param defaults to csv and accepts csv or json.

#### Scenario: CSV export with filters
- **WHEN** an admin requests /api/admin/orders/export with format=csv and any combination of filters/sort
- **THEN** the response includes `Content-Type: text/csv` and a download `Content-Disposition`, and rows contain at least Timestamp (ISO), Buyer Name, Buyer Email, Items ("title x qty" list), and Total Amount from the filtered/sorted dataset.

#### Scenario: JSON export with filters
- **WHEN** an admin requests /api/admin/orders/export with format=json and any filters
- **THEN** the response returns application/json containing entries with timestamp (ISO), buyer name/email, items (title and quantity), and totalAmount reflecting the filtered/sorted dataset.

#### Scenario: Reject unsupported format
- **WHEN** an admin provides an unsupported format value to /api/admin/orders/export
- **THEN** the API returns an error indicating invalid format without generating an export file.

