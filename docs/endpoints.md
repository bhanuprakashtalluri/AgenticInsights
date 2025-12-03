# API Endpoints Documentation

---

## Admin

### Export Database
- **URL:** `/admin/export?format=csv|json|toon`
- **Method:** GET
- **Params:**
  - `format=csv|json|toon` (required)
- **Description:** Export the entire database in the selected format.

### Import Database
- **URL:** `/admin/import`
- **Method:** POST
- **Params:**
  - `format=csv|json|toon` (required)
- **Body:** File upload (CSV, JSON, or TOON)
- **Description:** Import data into the database from a file.

### Sequence Repair (Admin/DB)
- **Migration:** `V3__fix_employee_id_seq.sql`
- **Description:** Resets all main table sequences (employee, recognition_type, recognitions) to the next available value after manual data changes or restores. Prevents duplicate key errors on inserts.

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

### Delete Employee
- **URL:** `/employees/single?id=7` or `/employees/single?uuid=...`
- **Method:** DELETE
- **Description:** Delete an employee by id or uuid.

---

## Recognition Types

### List Recognition Types
- **URL:** `/recognitiontype`
- **Method:** GET
- **Params:**
  - `page` (default: 0)
  - `size` (default: 20)
- **Description:** List all recognition types.

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

### Delete Recognition Type
- **URL:** `/recognitiontype/single?id=1` or `/recognitiontype/single?uuid=...`
- **Method:** DELETE
- **Description:** Delete a recognition type by id or uuid.

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

### Search Recognitions
- **URL:** `/recognitions/search`
- **Method:** GET
- **Params:**
  - `id`, `uuid`, `name`, `unitId`, `typeId`, `points`, `role`, `status`, `category`, `page`, `size` (all optional, use `all` for any param to include all values)
- **Description:** Search recognitions by any combination of filters.

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

### Delete Recognition
- **URL:** `/recognitions/single?id=1` or `/recognitions/single?uuid=...`
- **Method:** DELETE
- **Description:** Delete a recognition by id or uuid.

### Approve/Reject Recognition
- **URL:** `/recognitions/approve?id=1` or `/recognitions/approve?uuid=...`
- **Method:** PATCH
- **Description:** Approve a recognition by id or uuid.

- **URL:** `/recognitions/reject?id=1` or `/recognitions/reject?uuid=...`
- **Method:** PATCH
- **Body:**
```json
{
  "reason": "Policy mismatch"
}
```
- **Description:** Reject a recognition by id or uuid, with a reason.

### Export Recognitions
- **URL:** `/recognitions/export?format=csv|json|toon&recipientId=all&senderId=all&role=all&status=all&category=all&managerId=all&days=30`
- **Method:** GET
- **Description:** Export recognitions in the selected format, with all filters as params. Use `all` to include all values for a filter.

### Graph Recognitions
- **URL:** `/recognitions/graph?groupBy=weeks&iterations=10&role=all&status=all&category=all`
- **Method:** GET
- **Produces:** image/png
- **Params:**
  - `groupBy` (days, weeks, months, years; default: days)
  - `iterations` (number of time units to include, e.g. 10 for last 10 weeks)
  - All other filters: `id`, `uuid`, `name`, `unitId`, `role`, `sender`, `receiver`, `manager`, `category`, `points`, `status`, `type` (all optional, use `all` to include all values)
- **Description:** Get a PNG graph of recognitions, grouped by the selected time frame and filters. Use `all` to include all values for a filter.

---

## Leaderboard

### Top Senders
- **URL:** `/leaderboard/top-senders?size=10&page=0&days=30&role=employee`
- **Method:** GET
- **Description:** Get the top senders leaderboard, with optional filters.

### Top Recipients
- **URL:** `/leaderboard/top-recipients?size=10&page=0&days=30&role=employee`
- **Method:** GET
- **Description:** Get the top recipients leaderboard, with optional filters.

---

## Metrics

### Metrics Summary
- **URL:** `/metrics/summary?days=30`
- **Method:** GET
- **Description:** Get a summary of metrics for the given time window.

### Status/Health
- **URL:** `/metrics/statusup`
- **Method:** GET
- **Description:** Check the status of the server.

---
