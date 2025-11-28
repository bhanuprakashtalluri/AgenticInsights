#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/lookup_reports_metrics_verification.log
mkdir -p tmp
: > $LOG

log() { echo "[LOG] $1" | tee -a $LOG; }
sec() { echo "\n==== $1 ====" | tee -a $LOG; }
run() { echo "\n$ $1" | tee -a $LOG; setopt noglob; eval $1 2>&1 | tee -a $LOG; unsetopt noglob; }
safe_jq() { local data="$1" filter="$2"; echo "$data" | jq -r "$filter" 2>/dev/null || echo ""; }

sec "Health check"
HC=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health")
log "Health HTTP=$HC"
[[ "$HC" == "200" ]] || { log "Server unhealthy"; exit 1; }

# Resolve sample employee and type
EMP_PAGE=$(curl -s "$BASE/employees?page=0&size=1")
EMP_UUID=$(echo $EMP_PAGE | jq -r '.content[0].uuid')
TYPE_LIST=$(curl -s "$BASE/recognition-types")
TYPE_UUID=$(echo $TYPE_LIST | jq -r '.[0].uuid')
log "Using employeeUuid=$EMP_UUID typeUuid=$TYPE_UUID"

sec "Lookup employee by UUID"
EMP_LOOK=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/lookup/employee-by-uuid/$EMP_UUID")
log "Lookup employee HTTP=$EMP_LOOK"

sec "Lookup recognition type by UUID"
TYPE_LOOK=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/lookup/type-by-uuid/$TYPE_UUID")
log "Lookup type HTTP=$TYPE_LOOK"

sec "Lookup non-existent employee UUID (expect 404)"
BAD_EMP=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/lookup/employee-by-uuid/00000000-0000-0000-0000-000000000000")
log "Lookup bad employee HTTP=$BAD_EMP"

sec "Lookup non-existent type UUID (expect 404)"
BAD_TYPE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/lookup/type-by-uuid/00000000-0000-0000-0000-000000000000")
log "Lookup bad type HTTP=$BAD_TYPE"

# Metrics summary
sec "Metrics summary (30 days)"
METRICS=$(curl -s -w 'HTTP_CODE=%{http_code}' "$BASE/metrics/summary?days=30")
METRICS_CODE=$(echo $METRICS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
METRICS_JSON=$(echo $METRICS | sed 's/HTTP_CODE=.*//')
TOTAL_REC=$(safe_jq "$METRICS_JSON" '.totalRecognitions')
APPROVED=$(safe_jq "$METRICS_JSON" '.approvedCount')
REJECTED=$(safe_jq "$METRICS_JSON" '.rejectedCount')
log "Metrics summary HTTP=$METRICS_CODE totalRecognitions=$TOTAL_REC approved=$APPROVED rejected=$REJECTED"

# Reports
sec "List reports before generate-now"
REPORTS_BEFORE=$(curl -s "$BASE/reports/list")
COUNT_BEFORE=$(echo $REPORTS_BEFORE | jq 'length')
log "Reports count before=$COUNT_BEFORE"

FROM=$(date -u -v-1d +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
LABEL="adhoc_test"
sec "Generate report now"
GEN=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST "$BASE/reports/generate-now?from=$FROM&to=$TO&label=$LABEL")
GEN_CODE=$(echo $GEN | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
BASE_NAME=$(echo $GEN | sed 's/HTTP_CODE=.*//')
log "Generate-now HTTP=$GEN_CODE baseName=$BASE_NAME"

sleep 2

sec "List reports after generate-now"
REPORTS_AFTER=$(curl -s "$BASE/reports/list")
COUNT_AFTER=$(echo $REPORTS_AFTER | jq 'length')
log "Reports count after=$COUNT_AFTER"

# Attempt to download each artifact (csv json png) for the generated base name
sec "Download generated artifacts"
for ext in json csv png; do
  CODE=$(curl -s -o tmp/report_${BASE_NAME}.$ext -w '%{http_code}' "$BASE/reports/download/${BASE_NAME}.$ext")
  SIZE=$(wc -c < tmp/report_${BASE_NAME}.$ext 2>/dev/null || echo 0)
  log "Download ${ext} HTTP=$CODE size=$SIZE"
  [[ "$CODE" == "200" ]] || log "FAILED to download ${ext}";
  [[ "$SIZE" == "0" ]] && log "Artifact ${ext} is empty";
  [[ "$ext" == "png" ]] && file tmp/report_${BASE_NAME}.png | tee -a $LOG || true
done

sec "Negative download non-existent file"
BAD_DL=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/reports/download/nonexistent_file.csv")
log "Download nonexistent HTTP=$BAD_DL"

sec "Summary matrix"
cat <<EOF | tee -a $LOG
Lookup employee: $EMP_LOOK (expect 200)
Lookup type: $TYPE_LOOK (expect 200)
Lookup bad employee: $BAD_EMP (expect 404)
Lookup bad type: $BAD_TYPE (expect 404)
Metrics summary: $METRICS_CODE totalRecognitions=$TOTAL_REC approved=$APPROVED rejected=$REJECTED
Reports before count: $COUNT_BEFORE
Generate-now: $GEN_CODE baseName=$BASE_NAME
Reports after count: $COUNT_AFTER
Download json/csv/png: see above
Bad download nonexistent: $BAD_DL (expect 404)
EOF

log "Lookup, reports, metrics endpoint verification complete. See $LOG for details."
