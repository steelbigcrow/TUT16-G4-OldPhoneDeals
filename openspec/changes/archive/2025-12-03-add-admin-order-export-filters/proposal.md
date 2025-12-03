# Change: Add admin order export and advanced filtering

## Why
- Spring admin order management lacks the CSV/JSON export and richer filtering (searchTerm, brandFilter) already present in the Node backend, leaving admins unable to search by buyer or item text or download data.
- Improvement Plan calls for parity so admins can query orders flexibly and export them for analysis or reporting.

## What Changes
- Add an admin endpoint to export orders with CSV or JSON output, reusing the same filtering and sorting inputs as listings.
- Extend admin order listing to accept searchTerm, brandFilter, and sortBy/sortOrder while keeping pagination and existing filters (userId, startDate, endDate).
- Centralize filtering/sorting logic in the service layer and adjust DTOs/validation to cover the new parameters and export formats.

## Impact
- Affected specs: admin-orders
- Affected code: spring-old-phone-deals controller/service (AdminController, AdminService, AdminServiceImpl), order DTOs/export helpers, security for admin-only endpoints