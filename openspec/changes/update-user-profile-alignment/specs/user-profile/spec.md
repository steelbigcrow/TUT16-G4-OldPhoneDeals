## ADDED Requirements
### Requirement: Spring API exposes JWT-based user profile endpoints aligned with legacy Express behaviour
The system SHALL provide JWT-authenticated profile endpoints matching the React Node Frontend Plan while preserving core Express `/api/update-profile` semantics for user data changes. Specifically, Spring Boot SHALL:
- Expose `GET /api/profile` that infers the current user from the JWT, returns an `ApiResponse<UserProfileResponse>` with `success`, `message`, and `data`, and includes in `data` at least `id`, `email`, `firstName`, `lastName`, `emailVerified`, `createdAt`, and `updatedAt`.
- Expose `PUT /api/profile` that infers the current user from the JWT and accepts a JSON body with `firstName`, `lastName`, and `email`. When the requested `email` differs from the stored email, the request body SHALL also include `currentPassword`, which MUST match the user's existing password before any change is persisted. Name-only changes SHALL NOT require `currentPassword`.
- Keep or alias existing `/api/profile/{userId}` and `/api/profile/{userId}/change-password` endpoints as internal or legacy variants, but React clients SHALL use the id-less `/api/profile` endpoints as the primary contract.

#### Scenario: Retrieve current user profile
- **WHEN** an authenticated user calls `GET /api/profile` with a valid JWT
- **THEN** the API responds with `success=true`, `data` containing the user's id, email, firstName, lastName, emailVerified flag, createdAt, and updatedAt, and a success message such as `"User profile retrieved successfully"`.

#### Scenario: Update name only without password
- **WHEN** a signed-in user sends `PUT /api/profile` with a body that changes `firstName` and/or `lastName` but leaves `email` unchanged and omits `currentPassword`
- **THEN** the API updates only the name fields, keeps the email the same, and responds with `success=true` and updated `data` including the new names and refreshed `updatedAt`.

#### Scenario: Reject email change without current password
- **WHEN** a signed-in user sends `PUT /api/profile` with a new `email` value but omits `currentPassword`
- **THEN** the API responds with a validation error (e.g., HTTP 400 with `success=false`) indicating that the current password is required to change the email, and SHALL NOT persist any changes.

#### Scenario: Change email and name with password verification
- **WHEN** a signed-in user sends `PUT /api/profile` with updated `firstName`, `lastName`, and a new `email` plus a `currentPassword` that matches the stored password
- **THEN** the API updates the user's name and email, ensures email uniqueness, saves the changes, and returns `success=true` with updated `data` mirroring the structure of `GET /api/profile` and a message such as `"Profile updated successfully"`.

#### Scenario: Reject invalid current password or duplicate email
- **WHEN** a signed-in user attempts an email change with an incorrect `currentPassword` or an email that violates validation/uniqueness constraints
- **THEN** the API responds with `success=false` (or an error status) and a message such as `"Current password is incorrect"` or an appropriate validation error, and SHALL NOT update the profile.

### Requirement: Spring password change endpoint matches legacy reset behaviour at `/api/profile/change-password`
The system SHALL expose a password change endpoint that follows the Node Frontend Plan URL and legacy Express reset semantics while using Spring security best practices.
- HTTP contract: `PUT /api/profile/change-password` (or equivalent alias) SHALL accept a body `{ currentPassword, newPassword }`, infer the user from JWT, and wrap responses in `ApiResponse<Void>` with `success` and `message`.
- Behaviour: the endpoint SHALL verify that `currentPassword` matches the stored password, enforce a minimum new password length of 6 characters, encode and persist the new password, and leave existing JWTs valid to match the Express implementation.

#### Scenario: Successful password change
- **WHEN** an authenticated user calls the password change endpoint with the correct `currentPassword` and a `newPassword` of sufficient length
- **THEN** the API updates the password, leaves the current JWT valid, and returns `success=true` with a message such as `"Password changed successfully"`.

#### Scenario: Reject incorrect current password
- **WHEN** an authenticated user calls the password change endpoint with an incorrect `currentPassword`
- **THEN** the API responds with `success=false` (or a 400-style error) and a message `"Current password is incorrect"` without changing the stored password.

#### Scenario: Reject too-short new password
- **WHEN** an authenticated user calls the endpoint with a `newPassword` shorter than 6 characters
- **THEN** the API responds with a validation error explaining the minimum length requirement and does not update the password.

### Requirement: React profile pages use the Spring profile contract and keep behaviour consistent with Angular
The React frontend SHALL provide profile-related routes and forms that consume the Spring profile endpoints and preserve the user experience of the Angular + Express implementation.
- Routes: `/profile` (ProfilePage), `/profile/edit` (EditProfilePage), and `/profile/change-password` (ChangePasswordPage) SHALL be protected routes requiring authentication.
- Data loading: Profile-related pages SHALL use an API client (e.g., `api/profile.ts`) to call `GET /api/profile`, cache the result, and pre-fill forms with the current user's `firstName`, `lastName`, and `email`.
- Updates: `EditProfilePage` SHALL submit to `PUT /api/profile` and, when the user modifies the email field, require them to provide their current password before submission; name-only edits SHALL be allowed without re-entering the password.
- State sync: On successful profile update or password change, the React app SHALL refresh its current user state (and localStorage if used for persistence) so that headers/account menus show the latest name and email.

#### Scenario: Edit profile with name-only changes
- **WHEN** a signed-in user navigates to `/profile/edit`, updates their first and last name without changing email, and submits the form
- **THEN** the UI calls `PUT /api/profile` without `currentPassword`, shows a success notification on `success=true`, updates the displayed name across the app, and returns the user to the profile view or keeps them on the edit page with updated data.

#### Scenario: Edit profile with email change and password prompt
- **WHEN** a signed-in user edits their email on `/profile/edit`
- **THEN** the UI prompts for the current password, sends `PUT /api/profile` including `currentPassword`, and on success updates the local user state; if the backend reports `Current password is incorrect` or a validation error, the UI shows the error and does not update local state.

#### Scenario: Change password from React
- **WHEN** a signed-in user visits `/profile/change-password`, submits current and new passwords that satisfy the rules, and receives a `success=true` response
- **THEN** the UI shows a confirmation message (and may optionally redirect back to `/profile`), without forcing a logout, aligning with the legacy Express behaviour.
