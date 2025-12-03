## 1. Implementation
- [x] 1.1 Extend admin order listing contract to accept searchTerm, brandFilter, sortBy, sortOrder alongside existing filters.
- [x] 1.2 Implement filtering logic covering buyer name/email and order item titles, with brandFilter as case-insensitive match on item titles; keep pagination and default sort by createdAt desc.
- [x] 1.3 Add /api/admin/orders/export with format=csv|json using the same filters/sort, and set appropriate download headers.
- [x] 1.4 Adjust DTOs/validation to handle new query params and ensure responses include needed fields for export (e.g., buyer name/email, item summaries).
- [x] 1.5 Add tests or manual verification steps for filtered listings and both export formats (happy path + invalid format/params).

## 2. Validation
- [x] 2.1 Run openspec validate add-admin-order-export-filters --strict.
- [x] 2.2 Run mvn test (or targeted Spring module tests) after implementation.
