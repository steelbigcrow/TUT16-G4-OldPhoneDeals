# Change: Add paginated user order history for the React/Spring stack

## Why
- The React frontend currently lacks a user order history view, yet the Node Frontend Plan expects paginated order listings as a core user feature.
- The new Spring Boot backend returns full order lists without pagination, and its paths differ from the legacy Node.js API; we need to align API shape and routing so the frontend can consume it efficiently.

## What Changes
- Add a Spring Boot `GET /api/orders` endpoint for authenticated users that returns paginated orders (fixed `createdAt desc`) with metadata mirroring the Node.js behaviour.
- Add order types, API client, and a React order history page routed at `/orders` with pagination controls、loading/empty/error states, targeting the Spring API instead of the legacy Node endpoint.
- Normalize the order response contract (items, totals, createdAt, pagination meta) across backend and frontend to reduce coupling to the legacy Node API.

## Impact
- Affected specs: user-orders
- Affected code: `spring-old-phone-deals` order controller/service/repository/DTOs; `react-frontend` API client/types, routing, and new order history UI; auth token handling for user requests.
