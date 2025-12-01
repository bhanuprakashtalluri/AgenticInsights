# Complete API Endpoint Reference

This document lists all HTTP endpoints implemented in the project (controllers), their HTTP method, full path, path/query/body parameters, and the expected request/response shapes. Use this as a quick reference when testing the app.

Base URL (default): http://localhost:8080

Notes:
- Admin endpoints are gated by the `app.dev.enabled` property (default false). Enable with `-Dapp.dev.enabled=true` or set `app.dev.enabled=true` in application.properties.
- Many endpoints accept or return UUIDs and integer ids. UUIDs are stable, `id` fields are numeric primary keys.

---

## Optimized API surface and behaviors (quick reference)
The sections below document the minimal, consistent endpoints and runtime behavior you requested. These map to the controllers in the codebase (e.g., `RecognitionController`, `AdminController`, `InsightsController`) and are intended to be the canonical surface.

Key behaviors required:
- List recognitions by employee (as sender or recipient) using either numeric id or UUID.
- Retrieve a single recognition by numeric id or UUID.
- Check recognition status (PENDING / APPROVED / REJECTED) and change status (approve/reject). When status is set to APPROVED, any existing `rejectionReason` must be cleared (nullified) automatically.
- CSV import (async job preferred) with jobId, error tracking, and optional AI-based fixes later.
- Export recognitions in CSV or JSON; support streaming large CSV exports using `stream=true` to avoid buffering large payloads in memory.
- Provide insights and graphs endpoints; `series` defines the time-bucket granularity or metric series included in the response (see below).
- Normalize recognition types (mapping synonyms/variants to canonical recognition type values) via a small migration or API endpoint.

Canonical endpoints (recommended):

- List recognitions (supports filtering by sender/recipient id or uuid):
  - GET /recognitions
    - Query params (examples): `page`, `size`, `senderId`, `senderUuid`, `recipientId`, `recipientUuid`, `from`, `to`, `q` (full-text)
    - Returns paginated list of RecognitionResponse. Use `senderUuid`/`recipientUuid` to prefer stable keys.

- Get single recognition:
  - GET /recognitions/1205d657-3180-41ff-9f28-41923ad5f706  (example UUID from local DB)
  - Backwards-compatible: GET /recognitions/1  (example numeric id from local DB)

- Check status:
  - GET /recognitions/1205d657-3180-41ff-9f28-41923ad5f706/status
  - Returns `{ "status": "PENDING"|"APPROVED"|"REJECTED", "rejectionReason": "..." | null }`.

- Approve / Reject (unified status endpoint):
  - PATCH /recognitions/1205d657-3180-41ff-9f28-41923ad5f706/status
    - Body examples:
      - Approve: `{ "status": "APPROVED", "approverUuid": "23062c7f-03fa-43c7-a154-7dbc818ca2bf" }`
      - Reject:  `{ "status": "REJECTED",  "approverUuid": "23062c7f-03fa-43c7-a154-7dbc818ca2bf", "rejectionReason": "Insufficient details" }`

- Import CSV (admin, async preferred):
  - POST /admin/imports  (multipart form file upload)  (example)
    - Response: 202 Accepted with body `{ "jobId": <jobId> }`.
    - Note: Replace `<jobId>` with the numeric job id returned by the import endpoint. For example, the server may return `{ "jobId": 42 }`.
  - GET /admin/imports/{jobId}  — check job status (replace `{jobId}` with the id returned by the import call)
  - GET /admin/imports/{jobId}/errors  — list errors for the job (replace `{jobId}` with the id returned by the import call)

- Exports (unified):
  - GET /recognitions/export.csv  (CSV export)
  - GET /recognitions/export.json (JSON export)

- Insights & graphs:
  - GET /recognitions/insights?days=30  (example)
  - GET /recognitions/insights/graph?days=30&metric=count&series=daily&format=json  (example)
    - `metric`: which numeric metric to plot (`count`, `points`, etc.).
    - `series`: controls the time-series granularity or which series to include. Examples:
      - `series=daily` → one series aggregated per day.
      - `series=weekly` → weekly buckets.
      - `series=count,points` → include two series in the output (count and points over time).
    - `format=json` returns JSON time-series (recommended for client-side chart rendering). `format=png` returns a server-rendered PNG image when requested.

  Explanation of `series` and `stream`:
  - `series`: a flexible parameter that either selects the time-bucket granularity (`daily`/`weekly`/`monthly`) or a comma-separated list of metrics to include as separate series in the same chart (e.g., `series=count,points`). The implementation should support both uses (granularity vs multiple metrics) and document the allowed values.
  - `stream`: boolean flag for export endpoints that, when true, causes the server to use a streaming response (chunked transfer) and emit rows incrementally. Use streaming for large CSV exports to avoid high memory usage and to allow the client to start consuming the file before the full export completes.

- Recognition types normalization (recommended flows):
  - Option A (migration/script): run a one-off DB migration that maps synonyms to canonical `typeName` values (recommended for large historical cleanup).
  - Option B (API-driven normalization): POST /recognition-types/normalize
    - Body: mapping rules or `"auto": true` to attempt auto-normalization.
    - Response: `{ normalizedCount: N, warnings: [...] }` and optionally a preview of changed rows.
  - Always provide an audit trail (which rows were changed and the before/after values) so changes can be reverted if necessary.

Notes on permissions and side-effects:
- Only allow `PATCH /.../status` to authenticated approvers with appropriate role; log who changed the status and when.
- Clearing `rejectionReason` on approve is a state change and should be audited.

Example curl snippets (replace host and uuids):

```bash
# List recognitions for recipient (by uuid)
curl -s "http://localhost:8080/recognitions?recipientUuid=RECIPIENT-UUID" | jq .

# Approve a recognition (unified status endpoint)
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"status":"APPROVED","approverUuid":"APPROVER-UUID"}' \
  http://localhost:8080/recognitions/RECOG-UUID/status

# Start CSV import (multipart)
curl -X POST -F "file=@/path/to/import.csv" http://localhost:8080/admin/imports

# Export CSV (filtered)
curl -s "http://localhost:8080/recognitions/export.csv?role=manager&days=60" -o recognitions.csv

# Export JSON (filtered)
curl -s "http://localhost:8080/recognitions/export.json?recipientId=42&senderId=7" | jq .
```

---

## 1) Admin endpoints (cleaned & simplified)

### POST /admin/seed/run
- Purpose: dev-only seed/run helper
- Guard: returns 403 when `app.dev.enabled=false`
- Response examples:
  - 200 OK: `{ "status": 200, "message": "Run SQL seed manually or enable dev seed to run programmatically" }`
  - 403 Forbidden when disabled: `{ "status": 403, "error": "DEV_DISABLED", "message": "dev seed disabled" }`

### POST /admin/imports
- Purpose: start async import (COPY-based). Accepts CSV file in multipart `file`.
- Consumes: multipart/form-data
- Body: form-data → key `file` (Type: File)
- Response:
  - 202 Accepted: `{ "jobId": <number>, ... }`
  - 400 Bad Request when file missing: `{ "error": "MISSING_MULTIPART_PART", "message": "Required multipart part 'file' is missing", ... }`
  - 500 on failure: `{ "error": "IMPORT_START_FAILED", "message": "..." }`

### GET /admin/imports/{jobId}
- Purpose: get async import job status
- Path: `{jobId}` returned by POST /admin/imports
- Response: JSON status payload with counts and timestamps
  - Example: `{ "id": 1, "filename": "sample_import.csv", "status": "PARTIAL", "total_rows": 3, "success_count": 1, "failed_count": 2, "created_at": "...", "started_at": "...", "finished_at": "..." }`

### GET /admin/imports/{jobId}/errors
- Purpose: list import errors (paginated JSON)
- Query: `page` (default 0), `size` (default 50)
- Response: `{ "items": [ { "id": ..., "row_num": ..., "raw_data": "...", "error_message": "...", "created_at": "..." } ], "page": 0, "size": 50, "totalElements": 2 }`

### GET /admin/imports/{jobId}/errors/csv
- Purpose: download CSV of import errors for the job
- Response: attachment `import_errors_{jobId}.csv`

### GET /admin/dev-mode
- Purpose: retrieve current dev mode state
- Response: `{ "enabled": true|false, "mode": "DEV_ENABLED"|"DEV_DISABLED" }`

### PATCH /admin/dev-mode
- Purpose: toggle dev mode runtime. Body `{ "enabled": true|false }`.
- Takes effect immediately for guarded endpoints (e.g., `/admin/seed/run`).
- Success: `200 OK { "status":200, "enabled": true, "mode": "DEV_ENABLED" }`
- Errors:
  - Missing field: `400 { "error":"MISSING_FIELD" }`
  - Wrong type: `400 { "error":"INVALID_FIELD_TYPE" }`

Removed legacy endpoints
- POST /admin/recognitions/bulk-upload (JPA-batched) — removed
- POST /admin/recognitions/bulk-import-copy — removed

Curl examples
```bash
# Start async import (multipart)
curl -X POST -F "file=@tmp/sample_import.csv" http://localhost:8080/admin/imports

# Check job status
curl -s http://localhost:8080/admin/imports/1 | jq .

# List errors (JSON)
curl -s "http://localhost:8080/admin/imports/1/errors?page=0&size=50" | jq .

# Download errors CSV
curl -OJ http://localhost:8080/admin/imports/1/errors/csv
```

---

## 2) Recognition endpoints
Base path: /recognitions

### GET /recognitions
- Purpose: paginated listing of recognitions
- Method: GET
- Example: `GET /recognitions?page=0&size=20&recipientId=42`

### POST /recognitions
- Purpose: create a recognition
- Method: POST
- Example: `POST /recognitions` with JSON body (see earlier example)

### GET /recognitions/1
- Purpose: fetch recognition by integer id (example id = 1)
- Method: GET
- Example: `GET /recognitions/1`

### GET /recognitions/1205d657-3180-41ff-9f28-41923ad5f706
- Purpose: fetch recognition by UUID (example UUID shown)
- Method: GET
- Example: `GET /recognitions/1205d657-3180-41ff-9f28-41923ad5f706`

### PUT /recognitions/1
- Purpose: update a recognition (idempotent replace/partial)
- Method: PUT
- Example: `PUT /recognitions/1` with JSON body

### PATCH /recognitions/1/approve
- Purpose: mark a recognition as APPROVED (legacy-style endpoint example)
- Method: PATCH
- Example: `PATCH /recognitions/1/approve?approverId=7`

### PATCH /recognitions/1/reject
- Purpose: mark as REJECTED (legacy-style endpoint example)
- Method: PATCH
- Example: `PATCH /recognitions/1/reject?reason=Insufficient%20details&approverId=7`

### DELETE /recognitions/1
- Purpose: delete a recognition
- Method: DELETE
- Example: `DELETE /recognitions/1`

### GET /recognitions/export.csv
- Purpose: download CSV of recognitions (supports filters)
- Method: GET
- Example: `GET /recognitions/export.csv?role=manager&days=60`

### GET /recognitions/export.json
- Purpose: export JSON array of recognitions (supports filters)
- Method: GET
- Example: `GET /recognitions/export.json?recipientId=42&senderId=7&days=15`

### GET /recognitions/insights?days=30
- Purpose: overall heuristic insights (daily series, totals)
- Method: GET
- Example: `GET /recognitions/insights?days=30`

### GET /recognitions/graph.png?days=30
- Purpose: PNG timeseries chart for recognitions (example)
- Method: GET
- Example: `GET /recognitions/graph.png?days=30`

### Role / Manager specific insights & exports (examples)
- GET /recognitions/insights/role?role=engineer&days=30
- GET /recognitions/insights/role/graph.png?role=engineer&days=30
- GET /recognitions/export-role.csv?role=engineer&days=30
- GET /recognitions/insights/manager/7?days=30
- GET /recognitions/export-manager/7.csv?days=30

#### Role parameter options
Current seeded roles (from V2 migration):
- `manager`
- `teamlead`
- `employee`
You may supply any string for `role`; if no employees match (e.g. `intern`), counts will be zero. To discover roles dynamically, list employees and inspect the `role` field: `GET /employees?page=0&size=50`.

### GET /recognitions/graph-advanced.png?series=daily&days=30
- Purpose: aggregated recognitions chart with selectable time granularity.
- Method: GET
- Example: `GET /recognitions/graph-advanced.png?series=weekly&days=90`
- Optional filters: `role`, `managerId`, `days`

#### Advanced Graph Series (Current Design)
Supported `series` values (required; defaults to `daily`):
- `daily` — raw per-day counts.
- `weekly` (alias `weakly` accepted) — ISO week buckets `YYYY-Www`.
- `monthly` — month buckets `YYYY-MM`.
- `quarterly` — quarter buckets `YYYY-Qn`.
- `yearly` — year buckets `YYYY`.

If an unsupported series is supplied, endpoint returns `400 Bad Request` with allowed values.

Data plotted: recognition count per bucket (only count; metric parameter removed to avoid confusion). To extend with additional metrics (points / approval states), enhance insights service to produce per-day aggregates for each metric and modify chart renderer accordingly.

Why is `days` still a parameter?
- `series` chooses the bucket size (granularity). `days` defines the overall time range (window) from now backwards. They are orthogonal: granularity answers "how big are the buckets"; days answers "how far back do we look".
- Without `days`, the API would need another way to decide how many buckets to return. For example, `series=weekly` could return 2 weeks or 52 weeks—`days` makes this explicit.

If you find `days` redundant or noisy, consider these alternative designs:
1. Implicit defaults per series (no `days` needed):
   - daily → last 30 days
   - weekly → last 12 weeks
   - monthly → last 12 months
   - quarterly → last 8 quarters
   - yearly → last 5 years
2. Explicit ranged parameters instead of `days`:
   - `from=YYYY-MM-DD` & `to=YYYY-MM-DD` (exact window, clearer for reporting)
3. Bucket count parameter:
   - `series=weekly&buckets=12` (derive timeframe from bucket count * bucket length)
4. Hybrid:
   - If `days` omitted → use default per series; if present → override.

Recommended next step if simplifying: make `days` optional and apply dynamic defaults above; validate that extremely large `days` values (e.g. > 1825 for daily) are either capped or paginated.

Example alternative (future):
```
GET /recognitions/graph-advanced.png?series=weekly            # implies last 12 weeks automatically
GET /recognitions/graph-advanced.png?series=monthly&buckets=6 # last 6 months
GET /recognitions/graph-advanced.png?series=daily&from=2025-01-01&to=2025-02-28
```

---

## 3) Recognition type endpoints
Base path: /recognition-types

### POST /recognition-types
- Example: `POST /recognition-types` with JSON body `{ "typeName": "ecard_with_points" }`

### GET /recognition-types
- Example: `GET /recognition-types`

### GET /recognition-types/1
- Example: `GET /recognition-types/1` (example id = 1 from local DB)

### GET /recognition-types/uuid/d518af33-8325-445d-8b9c-cc030795590b
- Example: `GET /recognition-types/uuid/d518af33-8325-445d-8b9c-cc030795590b` (example recognition_type UUID from local DB, type 'award')

### PUT /recognition-types/1
- Example: `PUT /recognition-types/1` with JSON body `{ "typeName": "award" }`

### DELETE /recognition-types/1
- Example: `DELETE /recognition-types/1`

---

## 4) Employee endpoints
Base path: /employees

### POST /employees
- Example: `POST /employees` with JSON body to create an employee

### GET /employees?page=0&size=20&role=manager
- Example: `GET /employees?page=0&size=20&role=manager`

### GET /employees/1
- Example: `GET /employees/1` (example employee id = 1 from local DB)

### GET /employees/uuid/23062c7f-03fa-43c7-a154-7dbc818ca2bf
- Example: `GET /employees/uuid/23062c7f-03fa-43c7-a154-7dbc818ca2bf` (example employee UUID from local DB; name: Alex Smith)

### PUT /employees/1
- Example: `PUT /employees/1` with JSON body to update

### DELETE /employees/1
- Example: `DELETE /employees/1`

---

## 5) Insights endpoints (separate controller)
Base path: /insights

### GET /insights/employee/42?days=30
- Example: `GET /insights/employee/42?days=30`

### GET /insights/unit/3?days=30
- Example: `GET /insights/unit/3?days=30`

### GET /insights/cohort?days=30
- Removed: cohort endpoint has been dropped to simplify the API surface. Use employee/unit/role/manager insights as needed.

---

## 6) Lookup endpoints (Removed / Deprecated)

The former lookup endpoints have been removed to reduce duplication:
- GET /lookup/employee-by-uuid/{uuid}
- GET /lookup/type-by-uuid/{uuid}

Use canonical resource endpoints instead:
- GET /employees/uuid/{uuid}
- GET /recognition-types/uuid/{uuid}

Rationale for removal:
- Identical payloads to canonical endpoints
- Reduced surface area and documentation/testing burden
- Simplifies caching and avoids divergence

If you previously used lookup endpoints:
- Replace /lookup/employee-by-uuid/{uuid} with /employees/uuid/{uuid}
- Replace /lookup/type-by-uuid/{uuid} with /recognition-types/uuid/{uuid}

Batch / polymorphic lookup future design (optional):
A future identity resolution endpoint could reintroduce a consolidated batch API without reviving the deprecated single-object paths:
```
POST /identity/resolve
{
  "employees": ["uuid1", "uuid2"],
  "types": ["uuidA"],
  "recognitions": ["uuidX"]
}
```
Response example:
```
{
  "employees": { "uuid1": { /* employee */ }, "uuid2": null },
  "types": { "uuidA": { /* type */ } },
  "recognitions": { "uuidX": { /* recognition */ } },
  "missing": { "employees": ["uuid2"], "types": [], "recognitions": [] }
}
```

---

## 7) Reports endpoints
Base path: /reports

### GET /reports/list
- Example: `GET /reports/list`

### GET /reports/download/monthly_report.csv
- Example: `GET /reports/download/monthly_report.csv`

### POST /reports/generate-now?from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z&label=jan
- Example: `POST /reports/generate-now?from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z&label=jan`

---

## 8) Metrics (updated structured response)

### GET /metrics/summary?days=30
- Purpose: return structured metrics over a window
- Query params:
  - days (optional, >0). Default 30 when missing/<=0.
- Response shape:
  {
    "window": { "from": "<ISO>", "to": "<ISO>" },
    "totals": { "count": <int>, "points": <int> },
    "statuses": { "approved": <int>, "rejected": <int>, "pending": <int>, "approvalRatePercent": <number> },
    "series": { "daily": { "YYYY-MM-DD": <int>, ... } },
    "leaderboards": {
      "topSenders": { <senderId>: <count>, ... },
      "topRecipients": { <recipientId>: <count>, ... }
    },
    "pointsDistribution": { "0-10": <int>, "11-50": <int>, "51-100": <int>, ">100": <int> },
    "roles": {
      "sendersByRole": { "manager": <long>, "employee": <long>, ... },
      "recipientsByRole": { "manager": <long>, "employee": <long>, ... }
    },
    "managers": {
      <managerId>: { "teamSize": <int>, "teamRecognitions": <long>, "teamPoints": <int> },
      ...
    }
  }
- Notes:
  - Empty windows return zeros/empty maps with 200 OK.
  - ApprovalRatePercent is 0.0 when no recognitions in window.

Example:
{
  "window": { "from": "2025-10-29T00:00:00Z", "to": "2025-11-28T00:00:00Z" },
  "totals": { "count": 152, "points": 1200 },
  "statuses": { "approved": 140, "rejected": 5, "pending": 7, "approvalRatePercent": 92.105 },
  "series": { "daily": { "2025-11-01": 5, "2025-11-02": 3 } },
  "leaderboards": { "topSenders": { "7": 18, "12": 15 }, "topRecipients": { "42": 20, "9": 14 } },
  "pointsDistribution": { "0-10": 30, "11-50": 80, "51-100": 25, ">100": 17 },
  "roles": { "sendersByRole": { "manager": 40, "employee": 100 }, "recipientsByRole": { "manager": 30, "employee": 110 } },
  "managers": { "3": { "teamSize": 8, "teamRecognitions": 22, "teamPoints": 180 } }
}

---

## 9) Notes on bodies and common shapes
- RecognitionCreateRequest (fields used in code):
  - recognitionTypeId (Long)
  - recognitionTypeUuid (UUID)
  - awardName (String)
  - level (String)
  - recipientId (Long)
  - recipientUuid (UUID)
  - senderId (Long)
  - senderUuid (UUID)
  - sentAt (String ISO-8601)
  - message (String)
  - awardPoints (Integer)

- RecognitionUpdateRequest: any of awardName, level, message, awardPoints, approvalStatus, rejectionReason

- EmployeeCreateRequest: firstName,lastName,unitId,managerId,email,joiningDate,role

- LeaderboardPage: { content: [ LeaderboardEntry ], page, size, totalElements }
- LeaderboardEntry: { id, name, count, points }

---

If you want, I can:
- generate a small `tmp/sample_import.csv` (valid + invalid rows) for testing (I attempted earlier; I can re-create it correctly),
- start the application with `./gradlew bootRun -Dapp.dev.enabled=true` and monitor logs while you exercise endpoints, or
- create curl examples for each endpoint (with sample JSON bodies) saved as `docs/CURL_EXAMPLES.md`.

Which would you like me to do next?

## Admin (Postman) — How to get jobId and test imports

- Prerequisite: start app with dev endpoints enabled

  ```bash
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=rmkec \
  ./gradlew bootRun -Dapp.dev.enabled=true
  ```

- Postman environment variables (optional):
  - base = http://localhost:8080

- Steps to get jobId:
  1) Create a new request
     - Method: POST
     - URL: {{base}}/admin/imports
     - Body: form-data
       - Key: file (Type: File) → select local file `tmp/sample_import.csv`
     - Send the request
  2) In the response (Status 202 Accepted), look for JSON `{ "jobId": 1 }`
     - In Postman Tests tab (optional), store it for chaining:
       ```javascript
       const body = pm.response.json();
       if (body.jobId) pm.environment.set("jobId", body.jobId);
       ```
  3) Use {{jobId}} in subsequent URLs (examples below use jobId=1)

- Admin URLs (realistic examples):
  - POST {{base}}/admin/imports
    - Starts async import; returns `{ "jobId": 1 }`
  - GET {{base}}/admin/imports/1
    - Poll job status; returns counts and status
  - GET {{base}}/admin/imports/1/errors?page=0&size=50
    - Lists import errors (JSON)
  - GET {{base}}/admin/imports/1/errors/csv
    - Downloads errors CSV

## Admin endpoints reference (Postman-ready)

Below are all admin endpoints with exact method, URL, headers, query params, body fields, and file requirements so you can use them directly in Postman.

- Prerequisite: dev endpoints enabled

  ```bash
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=rmkec \
  ./gradlew bootRun -Dapp.dev.enabled=true
  ```

- Environment:
  - base = http://localhost:8080
  - jobId = 1 (example; use the id returned by POST /admin/imports)

1) Start async import
- Method: POST
- URL: {{base}}/admin/imports
- Headers:
  - Accept: application/json
- Query params: none
- Body (multipart/form-data):
  - file: File (choose local file `tmp/sample_import.csv`)
- Files required: `tmp/sample_import.csv`
- Example response (202 Accepted):
  - Body: `{ "jobId": 1 }`

2) Poll job status
- Method: GET
- URL: {{base}}/admin/imports/1
- Headers: (none required)
- Query params: none
- Body: none
- Files: none
- Example response (200 OK):
  - Body:
    {
      "id": 1,
      "filename": "sample_import.csv",
      "status": "PARTIAL",
      "total_rows": 3,
      "success_count": 1,
      "failed_count": 2,
      "created_at": "2025-11-27T05:30:00Z",
      "started_at": "2025-11-27T05:30:05Z",
      "finished_at": "2025-11-27T05:30:12Z"
    }

3) List import errors (JSON)
- Method: GET
- URL: {{base}}/admin/imports/1/errors
- Headers: (none required)
- Query params:
  - page: 0
  - size: 50
- Body: none
- Files: none
- Example response (200 OK):
  - Body:
    {
      "items": [
        { "id": 101, "row_num": 2, "raw_data": "...", "error_message": "missing recognition_type", "created_at": "..." },
        { "id": 102, "row_num": 3, "raw_data": "...", "error_message": "missing recipient", "created_at": "..." }
      ],
      "page": 0,
      "size": 50,
      "totalElements": 2
    }

4) Download import errors (CSV)
- Method: GET
- URL: {{base}}/admin/imports/1/errors/csv
- Headers:
  - Accept: text/csv (optional)
- Query params: none
- Body: none
- Files: none
- Response: CSV attachment (Postman can Save Response to file)

### Upload files (for admin endpoints)
- For POST /admin/imports (async import): use `tmp/sample_import.csv`

CSV headers expected by staging/bulk imports:
- recognition_uuid,recognition_type_uuid,award_name,level,recipient_uuid,sender_uuid,sent_at,message,award_points,approval_status,rejection_reason

#### Export endpoints (CSV / JSON) — updated filters
All export variants now accept optional filters matching advanced graph scopes:
- recipientId
- senderId
- role
- managerId
- days

Examples:
- `GET /recognitions/export.csv?role=manager&days=60`
- `GET /recognitions/export.json?recipientId=42&senderId=7&days=15`

Behavior:
- If none of role/managerId/days provided → exports entire dataset (legacy behavior)
- If any of role/managerId/days present → applies a time window (default 30 days when days missing or <=0)
- Entity filters (recipientId, senderId) applied after initial selection.
- Empty result set returns empty CSV/JSON with 200 OK.

Edge cases:
- Negative or zero days → default 30.
- Conflicting filters simply intersect (e.g., role=manager & managerId=99).

Performance: filtering is currently in-memory after fetching window; consider native queries for large datasets.
