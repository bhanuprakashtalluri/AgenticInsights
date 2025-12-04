# API Endpoints Documentation

---

## Admin

### Export Database
- **URL:** `/admin/export?format=csv|json|toon`
- **Method:** GET
- **Params:**
  - `format=csv|json|toon` (required)
- **Description:** Export the entire database in the selected format.
- **Authorization:** Manager only (`Authorization: Bearer <manager token>`)

### Import Database
- **URL:** `/admin/import`
- **Method:** POST
- **Params:**
  - `format=csv|json|toon` (required)
- **Body:** File upload (CSV, JSON, or TOON)
- **Description:** Import data into the database from a file.
- **Authorization:** Manager only (`Authorization: Bearer <manager token>`)

### Sequence Repair (Admin/DB)
- **Migration:** `V3__fix_employee_id_seq.sql`
- **Description:** Resets all main table sequences (employee, recognition_type, recognitions) to the next available value after manual data changes or restores. Prevents duplicate key errors on inserts.
- **Authorization:** Manager only

---

## Employee

### List Employees
- **URL:** `/employees`
- **Method:** GET
- **Params:**
  - `page` (default: 0)
  - `size` (default: 20)
  - `unitId` (optional, or `unitId=all`)
  - `role` (optional, or `role=all`)
- **Description:** List all employees, with optional filtering by unit or role.
- **Authorization:** Teamleader or manager (`Authorization: Bearer <teamleader|manager token>`)

### Search Employees
- **URL:** `/employees/search`
- **Method:** GET
- **Params:**
  - `id` (optional)
  - `uuid` (optional)
  - `name` (optional)
  - `unitId` (optional, or `unitId=all`)
  - `role` (optional, or `role=all`)
  - `page` (default: 0)
  - `size` (default: 20)
- **Description:** Search employees by id, uuid, name, unit, or role.
- **Authorization:** Teamleader or manager

### Create Employee
- **URL:** `/employees`
- **Method:** POST
- **Body:**
```json
{
  "firstName": "Grace",
  "lastName": "Moore",
  "unitId": 101,
  "managerId": 3,
  "email": "grace.moore@company.com",
  "joiningDate": "2022-03-01",
  "role": "employee"
}
```
- **Description:** Create a new employee.
- **Authorization:** Manager only

### Update Employee
- **URL:** `/employees/single?id=7` or `/employees/single?uuid=...`
- **Method:** PUT
- **Body:**
```json
{
  "firstName": "Grace",
  "lastName": "Moore",
  "unitId": 101,
  "managerId": 3,
  "email": "grace.moore@company.com",
  "joiningDate": "2022-03-01",
  "role": "employee"
}
```
- **Description:** Update an employee by id or uuid.
- **Authorization:** Teamleader or manager

### Delete Employee
- **URL:** `/employees/single?id=7` or `/employees/single?uuid=...`
- **Method:** DELETE
- **Description:** Delete an employee by id or uuid.
- **Authorization:** Manager only

---

## Recognition Types

### List Recognition Types
- **URL:** `/recognitiontype`
- **Method:** GET
- **Params:**
  - `page` (default: 0)
  - `size` (default: 20)
- **Description:** List all recognition types.
- **Authorization:** Manager only

### Search Recognition Types
- **URL:** `/recognitiontype/search`
- **Method:** GET
- **Params:**
  - `id` (optional)
  - `uuid` (optional)
  - `name` (optional)
  - `page` (default: 0)
  - `size` (default: 20)
- **Description:** Search recognition types by id, uuid, or name.
- **Authorization:** Manager only

### Create Recognition Type
- **URL:** `/recognitiontype`
- **Method:** POST
- **Body:**
```json
{
  "typeName": "award"
}
```
- **Description:** Create a new recognition type.
- **Authorization:** Manager only

### Update Recognition Type
- **URL:** `/recognitiontype/single?id=1` or `/recognitiontype/single?uuid=...`
- **Method:** PUT
- **Body:**
```json
{
  "typeName": "ecard_with_points"
}
```
- **Description:** Update a recognition type by id or uuid.
- **Authorization:** Manager only

### Delete Recognition Type
- **URL:** `/recognitiontype/single?id=1` or `/recognitiontype/single?uuid=...`
- **Method:** DELETE
- **Description:** Delete a recognition type by id or uuid.
- **Authorization:** Manager only

---

## Recognitions

### List Recognitions
- **URL:** `/recognitions`
- **Method:** GET
- **Params:**
  - `page` (default: 0)
  - `size` (default: 20)
  - `recipientId`, `senderId`, `recipientUuid`, `senderUuid` (optional)
- **Description:** List all recognitions, with optional filtering by sender/recipient.
- **Authorization:** Teamleader or manager

### Search Recognitions
- **URL:** `/recognitions/search`
- **Method:** GET
- **Params:**
  - `id`, `uuid`, `name`, `unitId`, `typeId`, `points`, `role`, `status`, `category`, `page`, `size` (all optional, use `all` for any param to include all values)
- **Description:** Search recognitions by any combination of filters.
- **Authorization:** Teamleader or manager

### Create Recognition
- **URL:** `/recognitions`
- **Method:** POST
- **Body:**
```json
{
  "recognitionTypeId": 3,
  "recipientId": 7,
  "senderId": 8,
  "category": "Great Job",
  "level": "gold",
  "message": "Excellent teamwork!",
  "awardPoints": 30,
  "sentAt": "2025-11-01T10:00:00Z"
}
```
- **Description:** Create a new recognition.
- **Authorization:** Employee, teamleader, or manager (employees can only send, not update/delete)

### Update Recognition
- **URL:** `/recognitions/single?id=1` or `/recognitions/single?uuid=...`
- **Method:** PUT
- **Body:**
```json
{
  "category": "Milestone Achieved",
  "level": "silver",
  "message": "Great milestone!",
  "awardPoints": 25,
  "approvalStatus": "APPROVED"
}
```
- **Description:** Update a recognition by id or uuid.
- **Authorization:** Only the manager of the respective employee can perform this action (`Authorization: Bearer <manager token>`)

### Delete Recognition
- **URL:** `/recognitions/single?id=1` or `/recognitions/single?uuid=...`
- **Method:** DELETE
- **Description:** Delete a recognition by id or uuid.
- **Authorization:** Only the manager of the respective employee can perform this action

### Approve/Reject Recognition
- **URL:** `/recognitions/approve?id=1` or `/recognitions/approve?uuid=...`
- **Method:** PATCH
- **Description:** Approve a recognition by id or uuid.
- **Authorization:** Only the manager of the respective employee can perform this action

- **URL:** `/recognitions/reject?id=1` or `/recognitions/reject?uuid=...`
- **Method:** PATCH
- **Body:**
```json
{
  "reason": "Policy mismatch"
}
```
- **Description:** Reject a recognition by id or uuid, with a reason.
- **Authorization:** Only the manager of the respective employee can perform this action

### Export Recognitions
- **URL:** `/recognitions/export?format=csv|json|toon&recipientId=all&senderId=all&role=all&status=all&category=all&managerId=all&days=30`
- **Method:** GET
- **Description:** Export recognitions in the selected format, with all filters as params. Use `all` to include all values for a filter.
- **Authorization:** Teamleader or manager

### Graph Recognitions
- **URL:** `/recognitions/graph?groupBy=weeks&iterations=10&role=all&status=all&category=all`
- **Method:** GET
- **Produces:** image/png
- **Params:**
  - `groupBy` (days, weeks, months, years; default: days)
  - `iterations` (number of time units to include, e.g. 10 for last 10 weeks)
  - All other filters: `id`, `uuid`, `name`, `unitId`, `role`, `sender`, `receiver`, `manager`, `category`, `points`, `status`, `type` (all optional, use `all` to include all values)
- **Description:** Get a PNG graph of recognitions, grouped by the selected time frame and filters. Use `all` to include all values for a filter.
- **Authorization:** Teamleader or manager

---

## Leaderboard

### Top Senders
- **URL:** `/leaderboard/top-senders?size=10&page=0&days=30&role=employee`
- **Method:** GET
- **Description:** Get the top senders leaderboard, with optional filters.
- **Authorization:** Teamleader or manager

### Top Recipients
- **URL:** `/leaderboard/top-recipients?size=10&page=0&days=30&role=employee`
- **Method:** GET
- **Description:** Get the top recipients leaderboard, with optional filters.
- **Authorization:** Teamleader or manager

---

## Metrics

### Metrics Summary
- **URL:** `/metrics/summary?days=30`
- **Method:** GET
- **Description:** Get a summary of metrics for the given time window.
- **Authorization:** Teamleader or manager

### Status/Health
- **URL:** `/metrics/statusup`
- **Method:** GET
- **Description:** Check the status of the server.
- **Authorization:** Any authenticated user

---

## Authentication & Authorization

### Login
- **URL:** `/api/auth/login`
- **Method:** POST
- **Body:**
  ```json
  {
    "username": "employee1",
    "password": "yourpassword"
  }
  ```
- **Response:**
  ```json
  {
    "token": "<JWT token>"
  }
  ```
- **Usage:** Use the token in `Authorization: Bearer <token>` for all protected endpoints.

### Refresh Token
- **URL:** `/api/auth/refresh`
- **Method:** POST
- **Body:**
  ```json
  {
    "refreshToken": "<refresh token>"
  }
  ```
- **Response:**
  ```json
  {
    "token": "<new JWT token>"
  }
  ```
- **Usage:** Use the new token for continued access. The refresh token is rotated and updated in the database.

### Add Employee (Manager Only)
- **URL:** `/api/manager/employees`
- **Method:** POST
- **Body:**
  ```json
  {
    "username": "newemployee",
    "password": "securepassword",
    "roles": ["EMPLOYEE"]
  }
  ```
- **Authorization:** Requires a valid manager JWT token in the `Authorization: Bearer <token>` header.

---

## Audit Logging
- All login and refresh actions are logged in the `AuditLog` table.
- Each log entry records username, action (LOGIN_SUCCESS, LOGIN_FAIL, REFRESH_SUCCESS, REFRESH_FAIL), timestamp, and details.

---
