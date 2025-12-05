## 1. Backend - Spring profile API alignment
- [x] 1.1 Add JWT-based `GET /api/profile` endpoint that infers the current user from the token, delegates to `ProfileService`, and returns `ApiResponse<UserProfileResponse>` with `success`, `data`, and `message` consistent with other Spring endpoints.
- [x] 1.2 Extend the update profile contract to support changing `firstName`, `lastName`, and `email` together; introduce or update a request DTO (e.g., `UpdateProfileRequest`) to include these fields plus an optional `currentPassword` when email changes.
- [x] 1.3 Implement `PUT /api/profile` that infers the user from JWT instead of a path id, validates names/email, requires `currentPassword` if email is different from the stored value, checks password via the encoder, and returns an updated `UserProfileResponse` (including id, email, firstName, lastName, emailVerified, createdAt, updatedAt).
- [x] 1.4 Keep `/api/profile/{userId}` and `/api/profile/{userId}/change-password` available or deprecate them explicitly, but make `/api/profile`/`/api/profile/change-password` the primary frontend contract.
- [x] 1.5 Ensure error handling for profile updates mirrors legacy Express semantics where practical (e.g., distinguish "User not found", "Current password is incorrect", and validation errors) while using Spring Boot exceptions/handlers.

## 2. Backend - Password change alignment
- [x] 2.1 Confirm `PUT /api/profile/{userId}/change-password` (or new `PUT /api/profile/change-password`) accepts `currentPassword` and `newPassword`, validates minimum length, and verifies the current password with the encoder.
- [x] 2.2 Align error messages and status semantics with legacy behaviour (e.g., `Current password is incorrect`, validation error for too-short new password) inside the `ApiResponse` envelope.
- [x] 2.3 Document that changing password does not invalidate the current JWT, matching the comment in `ProfileServiceImpl` and legacy Express behaviour.

## 3. Frontend - React profile pages and client
- [x] 3.1 Add `react-frontend/src/types/user.ts` profile types and `api/profile.ts` client functions for `getProfile`, `updateProfile`, and `changePassword` using the Spring `ApiResponse` shape and `/api/profile` endpoints defined in `Node Frontend Plan.md`.
- [x] 3.2 Implement `ProfilePage`, `EditProfilePage`, and `ChangePasswordPage` routes (`/profile`, `/profile/edit`, `/profile/change-password`) that mirror the Angular flows: pre-fill profile form fields from the API/local state, require current password when changing email, and surface success/error messages.
- [x] 3.3 Wire profile changes into auth/user context (or localStorage) so that successful updates refresh the in-memory/current user information shown in headers/account menus.

## 4. Validation
- [ ] 4.1 Spring Boot: run `mvn spring-boot:run`, exercise `GET /api/profile`, `PUT /api/profile` (name-only change and email+password change), and password change endpoint with valid/invalid inputs, verifying `success`, `data` shape, and error messages.
- [ ] 4.2 React: start the dev server, test the profile edit and password change flows end-to-end against the Spring backend, confirming that forms validate fields, require current password only when email changes, and update visible user info after success.
