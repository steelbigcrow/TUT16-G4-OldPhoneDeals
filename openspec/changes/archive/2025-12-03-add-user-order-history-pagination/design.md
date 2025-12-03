## Context
- React app currently only exposes admin listings; user order history is missing even though `Node Frontend Plan.md` calls for TanStack Query-managed order lists.
- Legacy Node.js API provides paginated `/api/orders` (page/limit, returns orders + total/totalPages/currentPage). Spring Boot offers `/api/orders/user/{userId}` returning an unpaginated list wrapped in `ApiResponse`.
- We must align the frontend with the Spring Boot backend while keeping behaviour consistent with the legacy pagination semantics (createdAt desc, metadata).

## Goals / Non-Goals
- Goals: deliver paginated user order retrieval in Spring; expose a React order history page that consumes it with pagination controls and resilient loading/empty/error states; align data contracts (order fields, pagination meta).
- Non-Goals: rework admin order flows; redesign checkout/cart; migrate legacy Node.js backend (only align response shape where practical).

## Decisions
- API surface: add `GET /api/orders` that infers the user from the JWT, accepts `page` (1-based) and `pageSize` query params (defaults 1/10), sorts fixed by `createdAt desc` (no client override), and wraps data in `ApiResponse`.
- Response contract: `data` contains `items` (OrderResponse list) and `pagination` { `currentPage`, `pageSize`, `totalPages`, `totalItems` }; this maps cleanly to the legacy Node fields while staying explicit.
- Compatibility: keep `/api/orders/user/{userId}` (list) for backward compatibility, but steer the frontend to the new paginated endpoint.
- Frontend data layer: add an orders API client and use a query-driven hook (TanStack Query preferred per plan; if not added, a thin fetcher with internal loading/error state will back pagination).
- UI: dedicated `/orders` page (auth-only) with a table/list of orders showing created date, total, and item summaries plus pagination controls (prev/next + direct page jumps if needed); reuse/extend a shared pagination component rather than bespoke logic.

## Risks / Trade-offs
- Data shape drift: if the frontend assumes the legacy flat `{orders, total...}` payload, we must document mapping to `ApiResponse` to avoid integration bugs.
- Auth context gaps: the current React app lacks user auth scaffolding; adding an orders page assumes token availability and may require minimal guard wiring.
- Performance: page size defaults (10) keep payloads small; we avoid sorting customizations to keep scope tight.

## Migration Plan
- Introduce the new paginated endpoint alongside the existing one.
- Update the React client to consume the new contract and route; verify manually with seeded data.
- Remove or deprecate use of the legacy `/api/orders/user/{userId}` on the frontend after verification.

## Open Questions
- (None for this scope; sorting fixed to `createdAt desc`, page path `/orders`.)
