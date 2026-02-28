# Optimistic Locking (Phone Writes) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add optimistic locking to Phone write operations in the Spring Boot backend, including unit tests, integration tests, and a Playwright E2E test.

**Architecture:** Spring Data MongoDB `@Version` field on `Phone` + optional request `version` validation + map conflicts to HTTP 409.

**Tech Stack:** Spring Boot 3.2.x, Spring Data MongoDB, JUnit 5, Mockito, Testcontainers (MongoDB), Playwright.

---

### Task 1: Conflict Exception + 409 Mapping

**Files:**
- Create: `spring-old-phone-deals/src/main/java/com/oldphonedeals/exception/VersionConflictException.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/exception/GlobalExceptionHandler.java`

**Steps:**
1. Add `VersionConflictException`.
2. Map `VersionConflictException` and `OptimisticLockingFailureException` to HTTP 409.
3. Run: `cd spring-old-phone-deals && mvn test`

### Task 2: Add `@Version` To Phone Entity

**Files:**
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/entity/Phone.java`

**Steps:**
1. Add `@Version private Long version;`
2. Run: `cd spring-old-phone-deals && mvn test`

### Task 3: Expose Version In DTOs

**Files:**
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/phone/PhoneResponse.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/phone/PhoneListItemResponse.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/admin/PhoneManagementResponse.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/PhoneServiceImpl.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/AdminServiceImpl.java`

**Steps:**
1. Add `version` fields to DTOs.
2. Map entity version in conversion helpers.
3. Run: `cd spring-old-phone-deals && mvn test`

### Task 4: Accept Optional Version On Update Requests

**Files:**
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/request/phone/PhoneUpdateRequest.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/request/phone/TogglePhoneStatusRequest.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/request/admin/UpdatePhoneRequest.java`

**Steps:**
1. Add `private Long version;` to each request DTO.
2. Run: `cd spring-old-phone-deals && mvn test`

### Task 5: Enforce Optimistic Lock On Writes

**Files:**
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/PhoneServiceImpl.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/AdminServiceImpl.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/service/PhoneService.java`
- Modify: `spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/PhoneController.java`

**Steps:**
1. Validate stale `version` when provided (throw `VersionConflictException`).
2. Catch `OptimisticLockingFailureException` on `save(...)` and convert to `VersionConflictException`.
3. Run: `cd spring-old-phone-deals && mvn test`

### Task 6: Unit Tests

**Files:**
- Modify: `spring-old-phone-deals/src/test/java/com/oldphonedeals/service/PhoneServiceTest.java`

**Steps:**
1. Add tests for stale version conflict and optimistic lock exception mapping.
2. Run: `cd spring-old-phone-deals && mvn test`

### Task 7: Integration Tests (MongoDB Testcontainers)

**Files:**
- Modify: `spring-old-phone-deals/pom.xml`
- Create: `spring-old-phone-deals/src/test/java/com/oldphonedeals/integration/mongo/AbstractMongoIT.java`
- Create: `spring-old-phone-deals/src/test/java/com/oldphonedeals/integration/mongo/PhoneOptimisticLockIT.java`

**Steps:**
1. Add `org.testcontainers:mongodb` test dependency.
2. Write Testcontainers-based repository test asserting stale save throws `OptimisticLockingFailureException`.
3. Run: `cd spring-old-phone-deals && mvn test`

### Task 8: Playwright E2E Test (API-Only)

**Files:**
- Create: `react-frontend/e2e/optimistic-locking.spec.ts`

**Steps:**
1. Reset dataset via `/api/e2e/reset`.
2. Login seller and fetch phone `version`.
3. Update once with current `version` (200).
4. Update again using stale `version` (409).

