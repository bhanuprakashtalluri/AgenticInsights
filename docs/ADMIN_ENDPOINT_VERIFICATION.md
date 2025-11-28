# Admin Endpoint Verification Guide

This document explains how to manually verify all admin import endpoints using Postman or curl, based on the automated script at `scripts/verify_admin_endpoints_detailed.sh`.

## Overview
Admin endpoints handle bulk and staged imports of recognitions and tracking of job progress / errors. They are only available when `APP_DEV_ENABLED=true` (dev mode). The workflow supports three ingestion patterns:
1. Synchronous bulk upload (in-memory parse).
2. Synchronous PG COPY import (staging + move).
3. Asynchronous PG COPY import (background thread).

Each method writes tracking rows to `import_job` and optionally `import_error`.

## Prerequisites
- Application running with environment variable `APP_DEV_ENABLED=true`.
- PostgreSQL database migrated (Flyway includes tables: `staging_recognitions`, `import_job`, `import_error`, plus `started_at` column added by V5 migration).
- Sample CSV at `tmp/sample_import.csv` (seed example from repo) containing good and intentionally bad rows.
- Tooling: curl or Postman.

## Suggested Postman Environment Variables
```
base = http://localhost:8080
sampleCsv = /absolute/path/to/tmp/sample_import.csv (for reference; use file picker in Postman)
copyJobId = (set after COPY import response)
asyncJobId = (set after async import response)
```

## CSV Sample (Excerpt)
```
recognition_uuid,recognition_type_uuid,award_name,level,recipient_uuid,sender_uuid,sent_at,message,award_points,approval_status,rejection_reason
,d518af33-8325-445d-8b9c-cc030795590b,Great Job,gold,23062c7f-03fa-43c7-a154-7dbc818ca2bf,23062c7f-03fa-43c7-a154-7dbc818ca2bf,2025-11-27T10:00:00Z,"Well done!",25,PENDING,
,00000000-0000-0000-0000-000000000000,InvalidType,gold,23062c7f-03fa-43c7-a154-7dbc818ca2bf,23062c7f-03fa-43c7-a154-7dbc818ca2bf,2025-11-27T11:00:00Z,"Bad type",10,PENDING,
,d518af33-8325-445d-8b9c-cc030795590b,MissingRecipient,gold,,23062c7f-03fa-43c7-a154-7dbc818ca2bf,2025-11-27T12:00:00Z,"No recipient",10,PENDING,
```
Expected: 3 rows total; 1 success, 2 errors.

## Endpoint Matrix
| Step | Method | Endpoint | Purpose | Expected Status |
|------|--------|----------|---------|-----------------|
| Seed | POST | /admin/seed/run | Dev-only seed hint | 200 |
| Bulk upload (missing) | POST | /admin/recognitions/bulk-upload | Negative test (no file) | 400 |
| Bulk upload | POST | /admin/recognitions/bulk-upload | Parse + insert recognitions | 200 |
| COPY import (missing) | POST | /admin/recognitions/bulk-import-copy | Negative (no file) | 400 |
| COPY import | POST | /admin/recognitions/bulk-import-copy | Fast COPY + move | 200 |
| COPY job status | GET | /admin/imports/{jobId} | Inspect job stats | 200 |
| COPY errors (paged) | GET | /admin/imports/{jobId}/errors | List row errors | 200 |
| COPY errors CSV | GET | /admin/imports/{jobId}/errors/csv | Download errors | 200 |
| Async import (missing) | POST | /admin/imports | Negative (no file) | 400 |
| Async import | POST | /admin/imports | Start background job | 202 |
| Poll async status | GET | /admin/imports/{jobId} | Track progress | 200 (final SUCCESS/PARTIAL/FAILED) |
| Async errors (paged) | GET | /admin/imports/{jobId}/errors | List row errors | 200 |
| Async errors CSV | GET | /admin/imports/{jobId}/errors/csv | Download errors | 200 |
| Non-existent job | GET | /admin/imports/99999999 | Negative job lookup | 500 (current) |

## Response Field Reference
`import_job` status values: QUEUED, RUNNING, SUCCESS, PARTIAL, FAILED

Job status JSON keys (example):
```
{
  "id": 10,
  "filename": "sample_import.csv",
  "status": "PARTIAL",
  "total_rows": 3,
  "success_count": 1,
  "failed_count": 2,
  "created_at": "2025-11-27T07:04:54.624Z",
  "started_at": null,          // null for synchronous COPY
  "finished_at": "2025-11-27T07:04:54.641Z"
}
```

Errors page (paged):
```
{
  "items": [
    {
      "id": 13,
      "row_num": 2,
      "raw_data": "(2,,00000000-...)",
      "error_message": "missing recognition_type",
      "created_at": "2025-11-27T07:04:54.634567Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 2
}
```

## Curl Examples
Enable dev endpoints and run app (example):
```bash
APP_DEV_ENABLED=true SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=rmkec ./gradlew bootRun
```
Bulk upload (success):
```bash
curl -F file=@tmp/sample_import.csv http://localhost:8080/admin/recognitions/bulk-upload | jq .
```
COPY import:
```bash
COPY_JSON=$(curl -s -F file=@tmp/sample_import.csv http://localhost:8080/admin/recognitions/bulk-import-copy)
echo $COPY_JSON | jq .
COPY_JOB=$(echo "$COPY_JSON" | jq -r '.jobId')
```
Job status:
```bash
curl -s http://localhost:8080/admin/imports/$COPY_JOB | jq .
```
Errors (paged):
```bash
curl -s "http://localhost:8080/admin/imports/$COPY_JOB/errors?page=0&size=50" | jq .
```
Errors CSV:
```bash
curl -s -o import_errors_$COPY_JOB.csv http://localhost:8080/admin/imports/$COPY_JOB/errors/csv
head import_errors_$COPY_JOB.csv
```
Async import:
```bash
ASYNC_JSON=$(curl -s -F file=@tmp/sample_import.csv http://localhost:8080/admin/imports)
ASYNC_JOB=$(echo "$ASYNC_JSON" | jq -r '.jobId')
for i in {1..20}; do curl -s http://localhost:8080/admin/imports/$ASYNC_JOB | jq .; sleep 1; done
```

## Postman Test Automation Snippets
Save jobId after COPY / async import (Tests tab):
```javascript
const body = pm.response.json();
if (body.jobId) pm.environment.set('jobId', body.jobId);
```
Poll async job (Pre-request Script on a "Poll Async Job" request):
```javascript
const jobId = pm.environment.get('jobId');
if (!jobId) throw new Error('jobId not set');
let c = parseInt(pm.environment.get('pollCount') || '0', 10);
if (c > 30) throw new Error('Polling timeout');
pm.environment.set('pollCount', String(c + 1));
```
Poll response tests:
```javascript
const b = pm.response.json();
if (['SUCCESS','PARTIAL','FAILED'].includes(b.status)) {
  pm.environment.unset('pollCount');
  pm.environment.set('finalJobStatus', b.status);
  pm.test('Terminal status reached', () => true);
} else {
  postman.setNextRequest('Poll Async Job');
}
```

## Expected Values (Sample Run)
- Bulk upload: totalRows=3, successCount=1, failedCount=2
- COPY import: status=PARTIAL, total_rows=3, success_count=1, failed_count=2
- Async import: final status PARTIAL (same counts) within first few polls
- Errors CSV size > 0 (sample ~511 bytes)

## Common Failure Modes & Suggestions
| Issue | Cause | Suggested Improvement |
|-------|-------|-----------------------|
| 400 MISSING_MULTIPART_PART | No file part provided | Ensure form-data key `file` present |
| 500 on non-existent job | Unhandled lookup | Return 404 with structured JSON error |
| started_at null for sync COPY | Not set in code | Optionally set started_at = finished_at for sync jobs |
| Constraint / parsing failures not detailed | Minimal error messages | Expand `error_message` classification (e.g. separate missing sender/recipient/type) |

## Enhancement Ideas
1. Add `/admin/imports` (list all jobs) endpoint.
2. Return consistent error shape (`timestamp,status,error,message,path`).
3. Add validation on CSV headers and supply a preview of failed rows.
4. Include metrics endpoint for import job throughput.
5. Provide a reprocessing endpoint for failed rows.

## Script Reference
The automated verification script: `scripts/verify_admin_endpoints_detailed.sh` executes all steps end-to-end and logs to `tmp/admin_endpoints_detailed.log`.

Run script:
```bash
zsh scripts/verify_admin_endpoints_detailed.sh
less tmp/admin_endpoints_detailed.log
```

## Quick Status Assertions (Manual)
- Bulk upload success: HTTP 200 JSON includes `totalRows`, `successCount`, `failedCount`.
- COPY job status: `status` one of SUCCESS/PARTIAL/FAILED.
- Errors endpoints: `totalElements` matches failedCount.
- CSV error file: First line header, subsequent lines error rows.

---
Use this guide to reproduce, inspect, and validate admin import behavior reliably in Postman or via curl.

