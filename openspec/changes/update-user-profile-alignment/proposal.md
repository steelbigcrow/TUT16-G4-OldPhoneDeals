# Change: Update user profile APIs to align Spring Boot with legacy Express logic and React plan

## Why
- Legacy Express.js backend exposes `POST /api/update-profile` that lets users update first name, last name, and email in a single call while verifying the current password; the Angular client `UserProfileService.updateProfile` relies on this behaviour and response shape (`success`, `message`, `user`).
- The new Spring Boot backend provides `/api/profile/{userId}` and `/api/profile/{userId}/change-password`, but only allows updating `firstName` and `lastName` without password verification and requires a path user id, which diverges from the Node Frontend Plan React API contract (`GET/PUT /api/profile`, `POST /api/profile/change-password`) and the legacy security expectations.
- We need a clear contract so the React frontend can call simple profile endpoints (`/api/profile`, `/api/profile/change-password`) while the Spring backend aligns semantics with the Express implementation (fields and password checks) without regressing security.

## What Changes
- Define JWT-based Spring profile endpoints that match the React Node Frontend Plan (`GET /api/profile`, `PUT /api/profile`, `PUT /api/profile/change-password`) while keeping `/api/profile/{userId}` aliases for internal/admin use if needed.
- Extend the profile update contract so a user can update `firstName`, `lastName`, and `email` in one request, and require the current password when changing email (to preserve the legacy Express behaviour where sensitive changes are gated by password verification).
- Define React profile pages (`/profile`, `/profile/edit`, `/profile/change-password`) and API client behaviour against the Spring `ApiResponse` envelope so that form fields, validation, and success/error handling are explicitly specified.

## Impact
- Affected specs: user-profile (new capability).
- Affected backend code: `spring-old-phone-deals` `ProfileController`, `ProfileService`/`ProfileServiceImpl`, `UpdateProfileRequest` (and possibly a dedicated email-change DTO), `ChangePasswordRequest`, and `ApiResponse` error handling.
- Affected legacy context: Express `profile.controller.js` and Angular `edit-profile` flow remain the behavioural reference but are not modified.
- Affected frontend code: `react-frontend` `api/profile.ts`, `types/user.ts`, profile-related pages/components, and auth/user context or local storage updates after profile changes.
