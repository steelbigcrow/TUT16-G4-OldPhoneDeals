## 1. Backend - Spring order pagination
- [x] 1.1 Add paginated repository/service method returning orders sorted by `createdAt` desc for the authenticated user.
- [x] 1.2 Expose `GET /api/orders` (auth required) with `page` (1-based) and `pageSize` defaults, returning `ApiResponse` data that includes items plus pagination metadata; keep `/api/orders/user/{userId}` behaviour intact.
- [x] 1.3 Document and sanity-check the new contract against the legacy Node.js response shape (orders + total/totalPages/currentPage) to avoid breaking existing expectations.

## 2. Frontend - React order history UI
- [x] 2.1 Add shared order types and an API client for paginated orders that matches the Spring response envelope.
- [x] 2.2 Build a user order history page with pagination controls, loading/empty/error states, and hook/state wiring (TanStack Query or equivalent) that fetches by `page`/`pageSize`.
- [x] 2.3 Wire routing/guarding so authenticated users can reach the order history page without impacting admin routes.

## 3. Validation
- [ ] 3.1 Spring Boot: run `mvn spring-boot:run`, hit `GET /api/orders?page=1&pageSize=5` with a valid JWT, and verify items, `currentPage`, `pageSize`, `totalPages`, `totalItems`, and sort order.
- [ ] 3.2 React: start the dev server, navigate through order pages with > `pageSize` orders, and confirm pagination updates list content and metadata while handling empty/error states.
