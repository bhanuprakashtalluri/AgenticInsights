# CURL Examples — Real values from local DB

Use these curl commands (run from macOS/zsh). They reference example IDs/UUIDs discovered in your local DB and the sample CSVs in `tmp/`.

Base URL: http://localhost:8080

Notes:
- Replace `localhost:8080` with your base if different.
- The import endpoints are dev-only behind `app.dev.enabled` (set `-Dapp.dev.enabled=true`).

---

## Recognitions

# 1) List recognitions (paged)
curl -s "http://localhost:8080/recognitions?page=0&size=20" | jq .

# 2) Get recognition by numeric id (example id = 1)
curl -s "http://localhost:8080/recognitions/1" | jq .

# 3) Get recognition by UUID (example UUID)
curl -s "http://localhost:8080/recognitions/1205d657-3180-41ff-9f28-41923ad5f706" | jq .

# 4) Approve a recognition (unified status endpoint)
# Use an approverUuid (example using employee UUID)
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"status":"APPROVED","approverUuid":"123062c7f-03fa-43c7-a154-7dbc818ca2bf"}' \
  http://localhost:8080/recognitions/1205d657-3180-41ff-9f28-41923ad5f706/status | jq .

# 5) Reject a recognition
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"status":"REJECTED","approverUuid":"123062c7f-03fa-43c7-a154-7dbc818ca2bf","rejectionReason":"Insufficient details"}' \
  http://localhost:8080/recognitions/1205d657-3180-41ff-9f28-41923ad5f706/status | jq .

# 6) Create a recognition (POST)
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "recognitionTypeId": 1,
    "awardName": "Great Job",
    "level": "gold",
    "recipientUuid": "123062c7f-03fa-43c7-a154-7dbc818ca2bf",
    "senderUuid": "123062c7f-03fa-43c7-a154-7dbc818ca2bf",
    "sentAt": "2025-11-27T10:00:00Z",
    "message": "Thanks for the excellent work",
    "awardPoints": 25
  }' http://localhost:8080/recognitions | jq .

---

## Import / Export

# 7) Start CSV import (multipart file upload)
# (dev-only; ensure app.dev.enabled=true when running the server)
curl -X POST -F "file=@tmp/sample_import.csv" http://localhost:8080/admin/imports

# 8) Check import job status (example jobId = 12345)
curl -s http://localhost:8080/admin/imports/12345 | jq .

# 9) Download import errors CSV for jobId
curl -s http://localhost:8080/admin/imports/12345/errors/csv -o import_errors_12345.csv

# 10) Export recognitions as CSV (streamed)
curl -s "http://localhost:8080/recognitions/export?format=csv&stream=true" -o recognitions_export.csv

# 11) Export recognitions as JSON
curl -s "http://localhost:8080/recognitions/export?format=json" | jq . > recognitions_export.json

---

## Employees & Types

# 12) Get employee by id
curl -s "http://localhost:8080/employees/1" | jq .

# 13) Get employee by uuid
curl -s "http://localhost:8080/employees/uuid/123062c7f-03fa-43c7-a154-7dbc818ca2bf" | jq .

# 14) Get recognition type by id
curl -s "http://localhost:8080/recognition-types/1" | jq .

# 15) Get recognition type by uuid
curl -s "http://localhost:8080/recognition-types/uuid/1d518af3-3832-445d-8b9c-cc030795590b" | jq .

---

## Reports & Insights

# 16) Get insights (30 days)
curl -s "http://localhost:8080/recognitions/insights?days=30" | jq .

# 17) Get trend graph (json series)
curl -s "http://localhost:8080/recognitions/insights/graph?days=30&metric=count&series=daily&format=json" | jq .

---

If you want, I can also generate a Postman collection from these examples or make a shell script that runs a smoke test sequence (create -> approve -> export) against your running app.

