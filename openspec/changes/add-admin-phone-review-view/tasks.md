## 1. Admin per-phone review API parity
- [ ] 1.1 Align Node GET `/api/admin/phones/{phoneId}/reviews` with pagination, sort (rating|createdAt), visibility/search filters, consistent response shape, and invalid-id handling.
- [ ] 1.2 Add/mirror Spring endpoint (e.g., `/api/admin/reviews/phones/{phoneId}` alias) with the same params/shape and validation through AdminService/AdminController.

## 2. Admin UI integration
- [ ] 2.1 Add AdminService method for per-phone reviews and wire a phone-level review modal/section with pagination/filter controls (search, visibility, sort) pulling from the new endpoint.
- [ ] 2.2 Hook hide/show/delete actions to refresh from the per-phone endpoint and surface success/error states without breaking the existing review management list.

## 3. Validation
- [ ] 3.1 Run `openspec validate add-admin-phone-review-view --strict`.
- [ ] 3.2 Manual check: per-phone view returns expected data (happy/invalid id) on Node and Spring backends; existing admin review list still loads.
