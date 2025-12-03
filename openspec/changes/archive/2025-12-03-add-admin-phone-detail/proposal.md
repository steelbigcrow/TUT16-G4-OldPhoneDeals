# Change: Admin phone detail for React frontend

## Why
Admin listing management in the new React frontend lacks a product detail view, making it impossible for admins to inspect a single phone with seller context as planned in `Node Frontend Plan.md`. The Node/Spring APIs expose (or will expose) `/api/admin/phones/{phoneId}` but the frontend has no flow consuming it.

## What Changes
- Add a new `admin-phones` capability describing the admin phone detail API response (includes seller basics and status fields) and failure handling for invalid IDs.
- Specify an admin phone detail page/route that fetches the detail, renders key fields (title, brand, price, stock, status, seller name/email, timestamps, image), and wires enable/disable and delete actions with confirmations and notifications.
- Update admin navigation to link from listing rows to the detail view so admins can drill into a single listing.

## Impact
- Affected specs: `admin-phones` (new).
- Affected code: React admin API client, admin listing/detail pages/routes (React), admin auth guard, and the backend admin phone detail endpoint to align response fields if missing.
