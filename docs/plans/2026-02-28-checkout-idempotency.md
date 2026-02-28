# Checkout Idempotency Hardening Implementation Plan

> **For Codex:** Apply TDD (red-green-refactor) for each behavior change; run targeted tests first, then full suite.

**Goal:** Harden checkout idempotency by normalizing UUID keys, binding keys to request payload, and unblocking stale `PROCESSING` checkouts.

**Architecture:** Keep the existing `Order`-document-based idempotency scheme, but (1) normalize incoming UUIDs to a canonical string form, (2) persist a deterministic request fingerprint on the `Order` and reject mismatched replays with `409`, and (3) detect stale `PROCESSING` orders and fail them deterministically so clients are not stuck in indefinite replays.

**Tech Stack:** Spring Boot, Spring MVC, Spring Data MongoDB, JUnit 5, Mockito, MockMvc.

---

### Task 1: Canonicalize `Idempotency-Key` UUID

**Files:**
- Modify: `spring-old-phone-deals/src/test/java/com/oldphonedeals/controller/OrderControllerTest.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/OrderController.java`

**Steps:**
1. Write failing controller test: uppercase UUID header should call service with canonical lowercase UUID.
2. Run: `mvn -q -f spring-old-phone-deals/pom.xml -Dtest=OrderControllerTest test` (expect FAIL).
3. Implement normalization in controller (parse UUID, pass `uuid.toString()` down).
4. Re-run same test (expect PASS).

### Task 2: Bind Idempotency Key To Checkout Request

**Files:**
- Modify: `spring-old-phone-deals/src/test/java/com/oldphonedeals/service/OrderServiceTest.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/entity/Order.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/OrderServiceImpl.java`

**Steps:**
1. Write failing service test: if an existing order is found for `(userId, idempotencyKey)` and its stored request hash differs from the incoming request, throw `DuplicateResourceException` (409).
2. Run: `mvn -q -f spring-old-phone-deals/pom.xml -Dtest=OrderServiceTest test` (expect FAIL).
3. Add `Order.idempotencyRequestHash` and store it when creating the processing order.
4. On replay, compare stored hash (if present) with computed hash; throw `DuplicateResourceException` on mismatch.
5. Re-run test (expect PASS).

### Task 3: Fail Stale `PROCESSING` Checkouts

**Files:**
- Modify: `spring-old-phone-deals/src/test/java/com/oldphonedeals/service/OrderServiceTest.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/OrderServiceImpl.java`

**Steps:**
1. Write failing service test: a `PROCESSING` order older than the stale threshold should be marked `FAILED` and throw `BadRequestException` with a timeout message.
2. Run: `mvn -q -f spring-old-phone-deals/pom.xml -Dtest=OrderServiceTest test` (expect FAIL).
3. Implement staleness check inside replay logic; call `markCheckoutFailed(...)` before throwing.
4. Re-run test (expect PASS).

### Task 4: Full Verification

**Steps:**
1. Run: `mvn -q -f spring-old-phone-deals/pom.xml test` (expect PASS).

