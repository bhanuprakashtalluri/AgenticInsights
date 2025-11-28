#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/admin_endpoints_detailed.log
mkdir -p tmp
: > $LOG

log() { echo "[LOG] $1" | tee -a $LOG; }
sec() { echo "\n==== $1 ====" | tee -a $LOG; }
run() { echo "\n$ $1" | tee -a $LOG; setopt noglob; eval $1 2>&1 | tee -a $LOG; unsetopt noglob; }
safe_jq() { local data="$1" filter="$2"; echo "$data" | jq -r "$filter" 2>/dev/null || echo ""; }

# Ensure server up
sec "Health check"
HC=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health")
log "Health HTTP=$HC"
[[ "$HC" == "200" ]] || { log "Server unhealthy"; exit 1; }

CSV=tmp/sample_import.csv
[[ -f $CSV ]] || { log "Sample CSV missing at $CSV"; exit 1; }

# 1. Seed run
sec "Dev seed run"
SEED=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST "$BASE/admin/seed/run")
SEED_CODE=$(echo $SEED | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Seed run HTTP=$SEED_CODE body=$(echo $SEED | sed 's/HTTP_CODE=.*//')"

# 2. Bulk upload missing file
sec "Bulk upload missing file (expect 400)"
BULK_MISS=$(curl -s -w 'HTTP_CODE=%{http_code}' -F dummy=1 "$BASE/admin/recognitions/bulk-upload")
BULK_MISS_CODE=$(echo $BULK_MISS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
BULK_MISS_ERR=$(echo $BULK_MISS | sed 's/HTTP_CODE=.*//' | jq -r '.error // empty')
log "Bulk upload missing HTTP=$BULK_MISS_CODE error=$BULK_MISS_ERR"

# 3. Bulk upload with file
sec "Bulk upload with file"
BULK_OK=$(curl -s -w 'HTTP_CODE=%{http_code}' -F file=@$CSV "$BASE/admin/recognitions/bulk-upload")
BULK_OK_CODE=$(echo $BULK_OK | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
BULK_JSON=$(echo $BULK_OK | sed 's/HTTP_CODE=.*//')
BULK_TOTAL=$(safe_jq "$BULK_JSON" '.totalRows')
BULK_SUCCESS=$(safe_jq "$BULK_JSON" '.successCount')
BULK_FAILED=$(safe_jq "$BULK_JSON" '.failedCount')
BULK_ERRORS_LEN=$(safe_jq "$BULK_JSON" '.errors | length')
log "Bulk upload HTTP=$BULK_OK_CODE totalRows=$BULK_TOTAL success=$BULK_SUCCESS failed=$BULK_FAILED errorsLen=$BULK_ERRORS_LEN"

# 4. COPY import missing file
sec "COPY import missing file (expect 400)"
COPY_MISS=$(curl -s -w 'HTTP_CODE=%{http_code}' -F dummy=1 "$BASE/admin/recognitions/bulk-import-copy")
COPY_MISS_CODE=$(echo $COPY_MISS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
COPY_MISS_ERR=$(echo $COPY_MISS | sed 's/HTTP_CODE=.*//' | jq -r '.error // empty')
log "COPY missing HTTP=$COPY_MISS_CODE error=$COPY_MISS_ERR"

# 5. COPY import with file
sec "COPY import with file"
COPY_OK=$(curl -s -w 'HTTP_CODE=%{http_code}' -F file=@$CSV "$BASE/admin/recognitions/bulk-import-copy")
COPY_OK_CODE=$(echo $COPY_OK | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
COPY_JSON=$(echo $COPY_OK | sed 's/HTTP_CODE=.*//')
COPY_JOB=$(safe_jq "$COPY_JSON" '.jobId')
COPY_INSERTED=$(safe_jq "$COPY_JSON" '.inserted')
COPY_FAILED=$(safe_jq "$COPY_JSON" '.failed')
log "COPY import HTTP=$COPY_OK_CODE jobId=$COPY_JOB inserted=$COPY_INSERTED failed=$COPY_FAILED"

# 6. GET import job status (COPY job)
sec "Import job status (COPY job)"
if [[ -n "$COPY_JOB" ]]; then
  COPY_STATUS_RAW=$(curl -s "$BASE/admin/imports/$COPY_JOB")
  log "COPY job raw=$COPY_STATUS_RAW"
  COPY_STATUS=$(safe_jq "$COPY_STATUS_RAW" '.status // empty')
  COPY_SUCCESS=$(safe_jq "$COPY_STATUS_RAW" '.success_count // empty')
  COPY_FAILED_CNT=$(safe_jq "$COPY_STATUS_RAW" '.failed_count // empty')
  COPY_TOTAL=$(safe_jq "$COPY_STATUS_RAW" '.total_rows // empty')
  log "COPY job parsed status=$COPY_STATUS total=$COPY_TOTAL success=$COPY_SUCCESS failed=$COPY_FAILED_CNT"
fi

# 7. Import errors (COPY job)
sec "Import errors (COPY job)"
if [[ -n "$COPY_JOB" ]]; then
  ERR_PAGE=$(curl -s "$BASE/admin/imports/$COPY_JOB/errors?page=0&size=50")
  ERR_TOTAL=$(echo $ERR_PAGE | jq -r '.totalElements // empty')
  log "Errors totalElements=$ERR_TOTAL"
fi

# 8. Download errors CSV (COPY job)
sec "Errors CSV (COPY job)"
if [[ -n "$COPY_JOB" ]]; then
  ERR_CSV_CODE=$(curl -s -o tmp/import_errors_${COPY_JOB}.csv -w '%{http_code}' "$BASE/admin/imports/$COPY_JOB/errors/csv")
  ERR_CSV_SIZE=$(wc -c < tmp/import_errors_${COPY_JOB}.csv 2>/dev/null || echo 0)
  log "Errors CSV HTTP=$ERR_CSV_CODE size=$ERR_CSV_SIZE"
  head -n 2 tmp/import_errors_${COPY_JOB}.csv | tee -a $LOG
fi

# 9. Async import missing file
sec "Async import start missing file (expect 400)"
ASYNC_MISS=$(curl -s -w 'HTTP_CODE=%{http_code}' -F dummy=1 "$BASE/admin/imports")
ASYNC_MISS_CODE=$(echo $ASYNC_MISS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Async start missing HTTP=$ASYNC_MISS_CODE"

# 10. Async import with file
sec "Async import start with file"
ASYNC_START=$(curl -s -w 'HTTP_CODE=%{http_code}' -F file=@$CSV "$BASE/admin/imports")
ASYNC_CODE=$(echo $ASYNC_START | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
ASYNC_JSON=$(echo $ASYNC_START | sed 's/HTTP_CODE=.*//')
ASYNC_JOB=$(safe_jq "$ASYNC_JSON" '.jobId')
log "Async start HTTP=$ASYNC_CODE jobId=$ASYNC_JOB"

# 11. Poll async job
sec "Poll async job status"
if [[ -n "$ASYNC_JOB" ]]; then
  for i in {1..40}; do
    RAW=$(curl -s "$BASE/admin/imports/$ASYNC_JOB")
    STATUS=$(safe_jq "$RAW" '.status // empty')
    TOTAL=$(safe_jq "$RAW" '.total_rows // empty')
    SUCCESS=$(safe_jq "$RAW" '.success_count // empty')
    FAILED=$(safe_jq "$RAW" '.failed_count // empty')
    log "Poll #$i status=$STATUS total=$TOTAL success=$SUCCESS failed=$FAILED raw=$RAW"
    [[ "$STATUS" == "SUCCESS" || "$STATUS" == "PARTIAL" || "$STATUS" == "FAILED" ]] && break
    sleep 1
  done
fi

# 12. Async job errors
sec "Import errors (ASYNC job)"
if [[ -n "$ASYNC_JOB" ]]; then
  AERR_PAGE=$(curl -s "$BASE/admin/imports/$ASYNC_JOB/errors?page=0&size=50")
  AERR_TOTAL=$(echo $AERR_PAGE | jq -r '.totalElements // empty')
  log "Async errors totalElements=$AERR_TOTAL"
fi

# 13. Async errors CSV
sec "Errors CSV (ASYNC job)"
if [[ -n "$ASYNC_JOB" ]]; then
  AERR_CSV_CODE=$(curl -s -o tmp/import_errors_${ASYNC_JOB}.csv -w '%{http_code}' "$BASE/admin/imports/$ASYNC_JOB/errors/csv")
  AERR_CSV_SIZE=$(wc -c < tmp/import_errors_${ASYNC_JOB}.csv 2>/dev/null || echo 0)
  log "Async errors CSV HTTP=$AERR_CSV_CODE size=$AERR_CSV_SIZE"
  head -n 2 tmp/import_errors_${ASYNC_JOB}.csv | tee -a $LOG
fi

# 14. Negative job status for non-existent id
sec "Import job status non-existent (expect 500 or error)"
BAD_STATUS_CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/admin/imports/99999999")
log "Non-existent job status HTTP=$BAD_STATUS_CODE"

# 15. Summary
sec "Summary matrix"
cat <<EOF | tee -a $LOG
Seed run: $SEED_CODE
Bulk upload missing: $BULK_MISS_CODE
Bulk upload success: $BULK_OK_CODE total=$BULK_TOTAL success=$BULK_SUCCESS failed=$BULK_FAILED errorsLen=$BULK_ERRORS_LEN
COPY missing: $COPY_MISS_CODE
COPY success: $COPY_OK_CODE jobId=$COPY_JOB inserted=$COPY_INSERTED failed=$COPY_FAILED status=$COPY_STATUS
COPY errors total: $ERR_TOTAL CSV size=$ERR_CSV_SIZE
Async missing: $ASYNC_MISS_CODE
Async start: $ASYNC_CODE jobId=$ASYNC_JOB finalStatus=$STATUS total=$TOTAL success=$SUCCESS failed=$FAILED
Async errors total: $AERR_TOTAL CSV size=$AERR_CSV_SIZE
Bad job status HTTP: $BAD_STATUS_CODE
EOF

log "Detailed admin endpoint verification complete. See $LOG for details."
