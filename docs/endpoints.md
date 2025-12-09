# API Endpoints Documentation (Current State)

This document lists all major API endpoints for the backend as of December 9, 2025. For each endpoint, you will find:
- **URL**
- **HTTP Method**
- **Required Headers**
- **Query/Path Parameters**
- **Request Body (if applicable)**
- **Response**
- **Role-based Access**
- **Description**

---

## 1. Auth Endpoints (`/api/auth`)

### POST `/api/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "username": "user@email.com",
    "password": "Password123"
  }
  ```
- **Response:** 200 OK (session cookie set), 401 Unauthorized
- **Roles:** All
- **Description:** Login with username (email) and password. Sets session cookie.

### PUT `/api/auth/update-password`
- **Headers:** `Content-Type: application/x-www-form-urlencoded`
- **Params:** `email`, `newPassword`
- **Response:** 200 OK, 404 Not Found
- **Roles:** All
- **Description:** Update password for user by email.

### POST `/api/auth/sync-users`
- **Headers:** None
- **Response:** 200 OK
- **Roles:** Admin only
- **Description:** Sync user table with employee table.

### GET `/api/auth/me`
- **Headers:** Session cookie
- **Response:**
  ```json
  {
    "email": "user@email.com",
    "role": "ADMIN"
  }
  ```
- **Roles:** All
- **Description:** Get current authenticated user info.

---

## 2. Employee Endpoints (`/employees`)

### POST `/employees`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "firstName": "Alice",
    "lastName": "Smith",
    "email": "alice.smith@company.com",
    "role": "MANAGER",
    "unitId": 1,
    "managerId": 2,
    "joiningDate": "2025-01-01"
  }
  ```
- **Response:** 201 Created, Employee object
- **Roles:** Admin, Manager
- **Description:** Create a new employee.

### GET `/employees`
- **Headers:** Session cookie
- **Params:** `page`, `size`, `role`, `managerId`, `unitId` (optional)
- **Response:** Page of Employee objects
- **Roles:**
  - **Employee:** Only sees their own info
  - **Teamlead:** Only sees employees where `managerId` matches their own ID
  - **Manager:** Only sees employees where `unitId` matches their own unit
  - **Admin:** Sees all employees
- **Description:** List employees, filtered by role-based access.

### GET `/employees/single`
- **Headers:** Session cookie
- **Params:** `id` or `uuid`
- **Response:** Employee object
- **Roles:** All (EMPLOYEE only sees own info)
- **Description:** Get employee by ID or UUID.

### PUT `/employees/single`
- **Headers:** `Content-Type: application/json`, Session cookie
- **Params:** `id` or `uuid`
- **Body:**
  ```json
  {
    "firstName": "Alice",
    "lastName": "Smith",
    "email": "alice.smith@company.com",
    "role": "MANAGER",
    "unitId": 1,
    "managerId": 2,
    "joiningDate": "2025-01-01"
  }
  ```
- **Response:** 200 OK, Employee object
- **Roles:** Admin, Manager
- **Description:** Update employee info.

### DELETE `/employees/single`
- **Headers:** Session cookie
- **Params:** `id` or `uuid`
- **Response:** 204 No Content
- **Roles:** Admin, Manager
- **Description:** Delete employee.

---

## 3. Recognition Endpoints (`/recognitions`)

### POST `/recognitions`
- **Headers:** `Content-Type: application/json`, Session cookie
- **Body:**
  ```json
  {
    "senderId": 1,
    "recipientId": 2,
    "category": "Appreciation",
    "level": "Gold",
    "message": "Great job!",
    "awardPoints": 10,
    "sentAt": "2025-12-09T10:00:00Z"
  }
  ```
- **Response:** 201 Created, Recognition object
- **Roles:** All
- **Description:** Create a new recognition.

### GET `/recognitions`
- **Headers:** Session cookie
- **Params:** `page`, `size`, `senderId`, `recipientId` (optional)
- **Response:** Page of Recognition objects
- **Roles:**
  - **Employee:** Only sees recognitions sent/received by them
  - **Teamlead:** Only sees recognitions for their team
  - **Manager:** Only sees recognitions for their unit
  - **Admin:** Sees all recognitions
- **Description:** List recognitions, filtered by role-based access.

### GET `/recognitions/single`
- **Headers:** Session cookie
- **Params:** `id` or `uuid`
- **Response:** Recognition object
- **Roles:** All (EMPLOYEE only if sender/recipient)
- **Description:** Get recognition by ID or UUID.

### PUT `/recognitions/single`
- **Headers:** `Content-Type: application/json`, Session cookie
- **Params:** `id` or `uuid`
- **Body:** Recognition object
- **Response:** 200 OK, Recognition object
- **Roles:** All (EMPLOYEE only if sender/recipient)
- **Description:** Update recognition.

### DELETE `/recognitions/single`
- **Headers:** Session cookie
- **Params:** `id` or `uuid`
- **Response:** 204 No Content
- **Roles:** Admin, Manager
- **Description:** Delete recognition.

---

## 4. Recognition Type Endpoints (`/recognition-types`)

### POST `/recognition-types`
- **Headers:** `Content-Type: application/json`, Session cookie
- **Body:** RecognitionType object
- **Response:** 201 Created, RecognitionType object
- **Roles:** Admin, Manager
- **Description:** Create a new recognition type.

### GET `/recognition-types`
- **Headers:** Session cookie
- **Response:** Page of RecognitionType objects
- **Roles:** All
- **Description:** List recognition types.

### GET `/recognition-types/single`
- **Headers:** Session cookie
- **Params:** `id` or `uuid`
- **Response:** RecognitionType object
- **Roles:** All
- **Description:** Get recognition type by ID or UUID.

### PUT `/recognition-types/single`
- **Headers:** `Content-Type: application/json`, Session cookie
- **Params:** `id` or `uuid`
- **Body:** RecognitionType object
- **Response:** 200 OK, RecognitionType object
- **Roles:** Admin, Manager
- **Description:** Update recognition type.

### DELETE `/recognition-types/single`
- **Headers:** Session cookie
- **Params:** `id` or `uuid`
- **Response:** 204 No Content
- **Roles:** Admin, Manager
- **Description:** Delete recognition type.

---

## 5. Insights Endpoints (`/insights`)

### GET `/insights`
- **Headers:** Session cookie
- **Params:** `days` (optional)
- **Response:** Insights data
- **Roles:** All
- **Description:** Get global insights, filtered by role.

### GET `/insights/graph.png`
- **Headers:** Session cookie
- **Params:** Various (see controller)
- **Response:** PNG image
- **Roles:** All
- **Description:** Get global insights graph.

### GET `/insights/employee/{employeeId}`
- **Headers:** Session cookie
- **Response:** Insights data for employee
- **Roles:** All
- **Description:** Get insights for a specific employee.

### GET `/insights/unit/{unitId}`
- **Headers:** Session cookie
- **Response:** Insights data for unit
- **Roles:** All
- **Description:** Get insights for a specific unit.

### GET `/insights/role`
- **Headers:** Session cookie
- **Params:** `role`
- **Response:** Insights data for role
- **Roles:** All
- **Description:** Get insights for a specific role.

### GET `/insights/role/graph.png`
- **Headers:** Session cookie
- **Params:** `role`
- **Response:** PNG image
- **Roles:** All
- **Description:** Get graph for a specific role.

### GET `/insights/manager/{managerId}`
- **Headers:** Session cookie
- **Response:** Insights data for manager
- **Roles:** All
- **Description:** Get insights for a specific manager.

### GET `/insights/manager/{managerId}/graph.png`
- **Headers:** Session cookie
- **Response:** PNG image
- **Roles:** All
- **Description:** Get graph for a specific manager.

---

## 6. Leaderboard Endpoints (`/leaderboard`)

### GET `/leaderboard/top-senders`
- **Headers:** Session cookie
- **Response:** Leaderboard data
- **Roles:** All
- **Description:** Get top senders leaderboard.

### GET `/leaderboard/top-recipients`
- **Headers:** Session cookie
- **Response:** Leaderboard data
- **Roles:** All
- **Description:** Get top recipients leaderboard.

---

## 7. Metrics Endpoints (`/metrics`)

### GET `/metrics/summary`
- **Headers:** Session cookie
- **Response:** Metrics summary
- **Roles:** All
- **Description:** Get metrics summary.

---

## 8. Admin Endpoints (`/admin`)

### POST `/admin/seed/run`
- **Headers:** Session cookie
- **Response:** 200 OK
- **Roles:** Admin only
- **Description:** Run seed (dev mode).

### GET `/admin/dev-mode`
- **Headers:** Session cookie
- **Response:** Dev mode status
- **Roles:** Admin only
- **Description:** Get dev mode status.

### PATCH `/admin/dev-mode`
- **Headers:** Session cookie
- **Body:** Dev mode status
- **Response:** 200 OK
- **Roles:** Admin only
- **Description:** Set dev mode status.

### GET `/admin/export`
- **Headers:** Session cookie
- **Response:** Exported data
- **Roles:** Admin only
- **Description:** Export data.

---

## Notes
- All endpoints require authentication via session cookie.
- Role-based access is strictly enforced for all major endpoints.
- Employees only see their own info and recognitions.
- Teamleads and managers see only their team's/unit's data.
- Admins have full access.
- For more details, see `endpoint_role_matrix.md`.

