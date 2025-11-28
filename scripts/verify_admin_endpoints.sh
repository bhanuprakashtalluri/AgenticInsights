#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/admin_endpoint_verification.log
mkdir -p tmp
: > $LOG

log() { echo "[LOG] $1" | tee -a $LOG; }
sec() { echo "\n==== $1 ====" | tee -a $LOG; }
run() { echo "\n$ $1" | tee -a $LOG; setopt noglob; eval $1 2>&1 | tee -a $LOG; unsetopt noglob; }
safe_jq() { local data="$1" filter="$2"; echo "$data" | jq -r "$filter" 2>/dev/null || echo ""; }

sec "Health check"
HC=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health")
log "Health HTTP=$HC"
if [[ "$HC" != "200" ]]; then log "Server not healthy"; exit 1; fi

sec "Seed run (dev enabled?)"
SEED_RESP=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST "$BASE/admin/seed/run")
SEED_CODE=$(echo $SEED_RESP | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Seed endpoint HTTP=$SEED_CODE"

CSV=tmp/sample_import.csv
if [[ ! -f $CSV ]]; then
  log "Sample import CSV not found at $CSV"; exit 1
fi

sec "Bulk upload missing file (expect 400)"
BULK_MISS=$(curl -s -w 'HTTP_CODE=%{http_code}' -F dummy=1 "$BASE/admin/recognitions/bulk-upload")
BULK_MISS_CODE=$(echo $BULK_MISS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Bulk upload missing file HTTP=$BULK_MISS_CODE"

sec "Bulk upload with file"
BULK_OK=$(curl -s -w 'HTTP_CODE=%{http_code}' -F file=@$CSV "$BASE/admin/recognitions/bulk-upload")
BULK_OK_CODE=$(echo $BULK_OK | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
BULK_JSON=$(echo $BULK_OK | sed 's/HTTP_CODE=.*//')
BULK_TOTAL=$(safe_jq "$BULK_JSON" '.totalRows // empty')
BULK_SUCCESS=$(safe_jq "$BULK_JSON" '.successCount // empty')
BULK_FAILED=$(safe_jq "$BULK_JSON" '.failedCount // empty')
BULK_ERRORS=$(safe_jq "$BULK_JSON" '.errors | length')
log "Bulk upload HTTP=$BULK_OK_CODE total=$BULK_TOTAL success=$BULK_SUCCESS failed=$BULK_FAILED errors=$BULK_ERRORS"

sec "Bulk import COPY missing file (expect 400)"
COPY_MISS=$(curl -s -w 'HTTP_CODE=%{http_code}' -F dummy=1 "$BASE/admin/recognitions/bulk-import-copy")
COPY_MISS_CODE=$(echo $COPY_MISS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Bulk import copy missing file HTTP=$COPY_MISS_CODE"

sec "Bulk import COPY with file"
COPY_OK=$(curl -s -w 'HTTP_CODE=%{http_code}' -F file=@$CSV "$BASE/admin/recognitions/bulk-import-copy")
COPY_OK_CODE=$(echo $COPY_OK | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
COPY_JSON=$(echo $COPY_OK | sed 's/HTTP_CODE=.*//')
COPY_JOB=$(safe_jq "$COPY_JSON" '.jobId // empty')
COPY_INSERTED=$(safe_jq "$COPY_JSON" '.inserted // empty')
COPY_FAILED=$(safe_jq "$COPY_JSON" '.failed // empty')
log "Bulk import copy HTTP=$COPY_OK_CODE jobId=$COPY_JOB inserted=$COPY_INSERTED failed=$COPY_FAILED"

sec "Async import start missing file (expect 400)"
ASYNC_MISS=$(curl -s -w 'HTTP_CODE=%{http_code}' -F dummy=1 "$BASE/admin/imports")
ASYNC_MISS_CODE=$(echo $ASYNC_MISS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Async start missing file HTTP=$ASYNC_MISS_CODE"

sec "Async import start with file"
ASYNC_START=$(curl -s -w 'HTTP_CODE=%{http_code}' -F file=@$CSV "$BASE/admin/imports")
ASYNC_CODE=$(echo $ASYNC_START | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
ASYNC_JSON=$(echo $ASYNC_START | sed 's/HTTP_CODE=.*//')
ASYNC_JOB=$(safe_jq "$ASYNC_JSON" '.jobId // empty')
log "Async start HTTP=$ASYNC_CODE jobId=$ASYNC_JOB"

if [[ -z "$ASYNC_JOB" ]]; then
  log "No async job id returned"; else
  sec "Poll async job status"
  for i in {1..30}; do
    STATUS_JSON=$(curl -s "$BASE/admin/imports/$ASYNC_JOB")
    JOB_STATUS=$(safe_jq "$STATUS_JSON" '.status // empty')
    SUCCESS=$(safe_jq "$STATUS_JSON" '.success_count // empty')
    FAILED=$(safe_jq "$STATUS_JSON" '.failed_count // empty')
    TOTAL=$(safe_jq "$STATUS_JSON" '.total_rows // empty')
    log "Poll #$i status=$JOB_STATUS total=$TOTAL success=$SUCCESS failed=$FAILED"
    if [[ "$JOB_STATUS" == "SUCCESS" || "$JOB_STATUS" == "PARTIAL" || "$JOB_STATUS" == "FAILED" ]]; then
      break
    fi
    sleep 1
  done

  sec "List import errors (paged first page)"
  ERRORS_PAGE=$(curl -s "$BASE/admin/imports/$ASYNC_JOB/errors?page=0&size=20")
  ERR_COUNT=$(safe_jq "$ERRORS_PAGE" '.totalElements // empty')
  log "Import errors totalElements=$ERR_COUNT"

  if [[ "$ERR_COUNT" != "0" ]]; then
    sec "Download errors CSV"
    curl -s -o tmp/import_errors_${ASYNC_JOB}.csv "$BASE/admin/imports/$ASYNC_JOB/errors/csv"
    head -n 3 tmp/import_errors_${ASYNC_JOB}.csv | tee -a $LOG
  fi
fi

sec "Summary matrix"
cat <<EOF | tee -a $LOG
Seed run HTTP: $SEED_CODE
Bulk upload missing file: $BULK_MISS_CODE (expect 400)
Bulk upload success: $BULK_OK_CODE totalRows=$BULK_TOTAL success=$BULK_SUCCESS failed=$BULK_FAILED errorsArrayLen=$BULK_ERRORS
Bulk copy missing file: $COPY_MISS_CODE (expect 400)
Bulk copy success: $COPY_OK_CODE jobId=$COPY_JOB inserted=$COPY_INSERTED failed=$COPY_FAILED
Async import missing file: $ASYNC_MISS_CODE (expect 400)
Async import start: $ASYNC_CODE jobId=$ASYNC_JOB finalStatus=$JOB_STATUS
Async import errors totalElements: $ERR_COUNT
EOF

log "Admin endpoint verification complete. See $LOG for details."
