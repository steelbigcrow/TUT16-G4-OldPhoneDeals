## Context
- Spring admin orders currently support page, pageSize, userId, startDate, endDate only and lack an export endpoint.
- Node backend already supports searchTerm/brandFilter, sorting, and CSV/JSON exports for admin orders.
- Order documents embed items (phoneId, title, quantity, price) and only reference the buyer by userId; brand data is not stored on the order itself.

## Goals / Non-Goals
- Goals: add advanced filters (searchTerm, brandFilter), sort controls, and CSV/JSON export for admin orders; keep listing and export behavior consistent.
- Non-Goals: redesign persistence schema, introduce async/background export, or change non-admin order APIs.

## Decisions
- Filtering: consolidate logic in the service to apply userId and date ranges plus searchTerm matching buyer name/email or any order item title, and brandFilter as a case-insensitive match against order item titles.
- Sorting: accept sortBy (default createdAt) and sortOrder (default desc); support at least createdAt and totalAmount with a safe fallback to createdAt on invalid input.
- Export: expose GET /api/admin/orders/export with format=csv|json (default csv) and reuse the same filters/sort; CSV columns include Timestamp (ISO), Buyer Name, Buyer Email, Items ("title x qty" list), Total Amount; JSON returns structured fields (timestamp ISO, buyer name/email, items with title/quantity, totalAmount).
- Reuse: share the filter/sort pipeline between listing and export to avoid divergence and duplicated code paths.

## Risks / Trade-offs
- In-memory filtering mirrors current implementation and is simpler than a Mongo aggregation, but could be slower at large scale; acceptable for current dataset.
- Brand filtering depends on item titles containing brand strings since orders do not store canonical brand data; accuracy may vary.
- Export builds the payload in memory; large result sets could increase memory usage, so pairing with filters/pagination is recommended.

## Migration Plan
- Add controller/service contracts for new parameters and export endpoint; implement shared filtering/sorting utility; generate CSV/JSON responses; keep endpoints admin-protected; add tests or manual checks.

## Open Questions
- Should brandFilter look up phone brand via phoneId instead of item title text?
- Do we need to enforce max row count or require pagination parameters for export requests?
