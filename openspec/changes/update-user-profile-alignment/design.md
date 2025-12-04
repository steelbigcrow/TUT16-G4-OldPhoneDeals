## Context
- Legacy Angular + Express implementation provides a `UserProfileService` that calls `GET /api/profile`, `POST /api/update-profile`, and `POST /api/reset-password`. The update endpoint accepts `{ id, firstName, lastName, email, password }`, verifies the current password on the server (`profile.controller.updateProfile`), updates the user, and returns `{ success, message, user }`. The change-password flow validates `currentPassword` and `newPassword` separately.
- The `Node Frontend Plan.md` expects the React app to interact with Spring Boot via JWT-based profile endpoints: `GET /api/profile` (current user), `PUT /api/profile` (update profile), and `POST /api/profile/change-password` (change password), with a shared `ApiResponse { success, message, data }` envelope.
- Current Spring Boot `ProfileController`/`ProfileServiceImpl` implement `GET /api/profile/{userId}`, `PUT /api/profile/{userId}`, and `PUT /api/profile/{userId}/change-password`. Update only allows `firstName`/`lastName`, does not require the current password, and uses a path `userId` rather than inferring the user from JWT.

## Goals / Non-Goals
- Goals:
  - Align Spring Boot profile endpoints with the legacy Express behaviour for profile updates (support name + email changes with current password verification for sensitive fields).
  - Align endpoint shapes and URLs with the React Node Frontend Plan (`/api/profile`, `/api/profile/change-password`) so the React client can rely on a stable contract independent of the underlying backend.
  - Specify React profile pages and client responsibilities so that profile edits and password changes behave consistently across Angular and React implementations.
- Non-Goals:
  - Refactor unrelated auth flows (login, register, reset-password email sending).
  - Implement full email-change verification (e.g., confirmation links); for now we mirror the simpler legacy behaviour while documenting the need for current password.

## Decisions
- API surface:
  - Introduce JWT-based `GET /api/profile` and `PUT /api/profile` that infer the user from the token and wrap responses in `ApiResponse<UserProfileResponse>`.
  - Treat `/api/profile/{userId}` and `/api/profile/{userId}/change-password` as internal/legacy endpoints; keep them for compatibility or deprecate them later, but steer the frontend to the id-less variants.
- Update contract:
  - Extend `UpdateProfileRequest` (or introduce a new DTO) to include `firstName`, `lastName`, `email`, and optionally `currentPassword`.
  - Require `currentPassword` to be correct when the requested email differs from the stored email; allow name-only changes without re-entering the password.
  - Enforce reasonable validation (name length, email format/uniqueness) and return structured validation errors via `ApiResponse`.
- Password change:
  - Keep the dedicated password change endpoint mapped to `/api/profile/change-password` at the HTTP layer, backed by the existing `ProfileService.changePassword` logic (current password check, min length, encoded storage) and `ApiResponse<Void>` response.
- Frontend behaviour:
  - `EditProfilePage` pre-fills fields using `GET /api/profile`, submits to `PUT /api/profile`, and prompts for current password when the email field is changed.
  - After a successful update, the React app updates its auth/user context (and localStorage if used) so headers/menus show the fresh profile data.

## Risks / Trade-offs
- Security vs UX: requiring the current password for email changes is secure but adds friction; we mitigate by only enforcing it when email actually changes.
- Backward compatibility: if any existing consumers call `/api/profile/{userId}` directly, introducing `/api/profile` and evolving DTOs must avoid breaking them; keeping the old endpoints as thin wrappers is safer.
- Data shape drift: mapping from legacy `{ success, message, user }` to Spring `ApiResponse<UserProfileResponse>` might require minor frontend transformations; we document the canonical shape here.

## Migration Plan
- Add and wire `GET /api/profile` and `PUT /api/profile` endpoints while keeping the existing `{userId}` variants.
- Extend/update profile DTOs and service logic to handle email + name updates with conditional password verification.
- Implement or update React profile pages and API client to use the new endpoints exclusively, verifying flows against the Node Frontend Plan expectations.
- Once verified, document preferred endpoints in README / API docs and consider marking the `{userId}` variants as deprecated for direct client use.

## Open Questions
- Should email changes eventually require out-of-band verification (e.g., confirmation email) beyond current password? (Out of scope for this change.)
- Do we need a separate endpoint/DTO for email-only changes, or is a unified `PUT /api/profile` sufficient for current coursework scope?
