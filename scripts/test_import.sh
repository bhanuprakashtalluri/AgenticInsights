#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
CSV=${CSV:-tmp/sample_import.csv}
DEV=${DEV:-true}

info() { echo "[INFO] $1"; }
err() { echo "[ERROR] $1" >&2; }

info "Checking server health at $BASE/actuator/health"
if ! curl -sSf "$BASE/actuator/health" >/dev/null; then
  err "Server is not up. Start with: SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=rmkec ./gradlew bootRun -Dapp.dev.enabled=true"
  exit 1
fi

info "Starting async import via /admin/imports using $CSV"
HTTP_CODE=$(curl -s -o /tmp/import_response.json -w "%{http_code}" -X POST -F "file=@${CSV}" "$BASE/admin/imports")
cat /tmp/import_response.json | jq . || cat /tmp/import_response.json || true
if [[ "$HTTP_CODE" != "202" && "$HTTP_CODE" != "200" ]]; then
  err "Import start failed, HTTP $HTTP_CODE"
  exit 2
fi

JOB_ID=$(jq -r '.jobId // .job_id // empty' /tmp/import_response.json 2>/dev/null || true)
if [[ -z "$JOB_ID" ]]; then
  err "No jobId returned in response. Check /tmp/import_response.json"
  exit 3
fi
info "jobId=$JOB_ID"

info "Polling job status at /admin/imports/$JOB_ID"
for i in {1..30}; do
  STATUS_JSON=$(curl -s "$BASE/admin/imports/$JOB_ID")
  STATUS=$(echo "$STATUS_JSON" | jq -r '.status // empty' 2>/dev/null || true)
  echo "$STATUS_JSON" | jq . || echo "$STATUS_JSON"
  if [[ "$STATUS" == "SUCCESS" || "$STATUS" == "PARTIAL" || "$STATUS" == "FAILED" ]]; then
    break
  fi
  sleep 1
done

info "Fetching errors JSON at /admin/imports/$JOB_ID/errors"
curl -s "$BASE/admin/imports/$JOB_ID/errors?page=0&size=50" | jq . || true

info "Downloading errors CSV at /admin/imports/$JOB_ID/errors/csv"
curl -s "$BASE/admin/imports/$JOB_ID/errors/csv" -o "import_errors_${JOB_ID}.csv" || true
ls -l "import_errors_${JOB_ID}.csv" || true

info "Done."

