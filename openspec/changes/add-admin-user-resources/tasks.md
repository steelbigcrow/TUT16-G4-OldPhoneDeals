## 1. Specification
- [ ] 1.1 Review Improvement Plan.md and current admin user endpoints/UI to finalize phone/review retrieval filters, sorting options, metadata, and validation needs.
- [ ] 1.2 Define admin UI expectations for viewing a user's selling phones and reviews, including controls, pagination, and error handling.

## 2. Implementation
- [ ] 2.1 Backend: Implement admin-protected GET `/api/admin/users/{userId}/phones` with default page/limit, allowed sort fields, brand filter, user existence/ID validation, and responses containing total/page/limit plus phone summaries with rating/review counts.
- [ ] 2.2 Backend: Implement admin-protected GET `/api/admin/users/{userId}/reviews` with brand filtering and rating/createdAt sorting, returning phone context, average rating, review visibility, total/page/limit, and validation for IDs/pagination.
- [ ] 2.3 Frontend: Update admin user management modals to load and display user phones/reviews with sorting/filter/pagination controls, loading/error states, and field mapping consistent with APIs.
- [ ] 2.4 Service integration: Ensure admin service wrappers send query params correctly and handle unsupported options across Angular/Node/Spring implementations.
- [ ] 2.5 Validation: Exercise endpoints (e.g., Postman/curl) and admin UI to confirm filters, sorting, pagination, error handling, and metadata are honored.

## 3. Testing
- [ ] 3.1 Run available linters/tests for touched projects (e.g., Angular `npm test` if feasible; server sanity checks) or document manual verification outcomes.
