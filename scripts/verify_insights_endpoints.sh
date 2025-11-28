#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/insights_endpoint_verification.log
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

sec "Resolve sample employee"
EMP_PAGE=$(curl -s "$BASE/employees?page=0&size=1")
EMP_ID=$(echo $EMP_PAGE | jq -r '.content[0].id')
UNIT_ID=$(echo $EMP_PAGE | jq -r '.content[0].unitId')
log "Using employeeId=$EMP_ID unitId=$UNIT_ID"

DAYS=30

sec "Employee insights (id=$EMP_ID days=$DAYS)"
EMP_INS=$(curl -s -w 'HTTP_CODE=%{http_code}' "$BASE/insights/employee/$EMP_ID?days=$DAYS")
EMP_CODE=$(echo $EMP_INS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
EMP_JSON=$(echo $EMP_INS | sed 's/HTTP_CODE=.*//')
EMP_KEYS=$(echo $EMP_JSON | jq -r 'keys')
REC_COUNT=$(safe_jq "$EMP_JSON" '.receivedCount')
SENT_COUNT=$(safe_jq "$EMP_JSON" '.sentCount')
log "Employee insights HTTP=$EMP_CODE keys=$EMP_KEYS receivedCount=$REC_COUNT sentCount=$SENT_COUNT"

sec "Unit insights (unit=$UNIT_ID days=$DAYS)"
UNIT_INS=$(curl -s -w 'HTTP_CODE=%{http_code}' "$BASE/insights/unit/$UNIT_ID?days=$DAYS")
UNIT_CODE=$(echo $UNIT_INS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
UNIT_JSON=$(echo $UNIT_INS | sed 's/HTTP_CODE=.*//')
UNIT_COUNT=$(safe_jq "$UNIT_JSON" '.count')
UNIT_POINTS=$(safe_jq "$UNIT_JSON" '.totalPoints')
log "Unit insights HTTP=$UNIT_CODE count=$UNIT_COUNT totalPoints=$UNIT_POINTS"

sec "Cohort insights (days=$DAYS)"
COHORT_INS=$(curl -s -w 'HTTP_CODE=%{http_code}' "$BASE/insights/cohort?days=$DAYS")
COHORT_CODE=$(echo $COHORT_INS | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
COHORT_JSON=$(echo $COHORT_INS | sed 's/HTTP_CODE=.*//')
BUCKET_0_90=$(safe_jq "$COHORT_JSON" '."0-90".employees')
BUCKET_91_365=$(safe_jq "$COHORT_JSON" '."91-365".employees')
BUCKET_366_9999=$(safe_jq "$COHORT_JSON" '."366-9999".employees')
log "Cohort insights HTTP=$COHORT_CODE 0-90=$BUCKET_0_90 91-365=$BUCKET_91_365 366-9999=$BUCKET_366_9999"

sec "Negative: employee insights non-existent id 999999 (expect 200 with zeros or empty or 500?)"
EMP_BAD=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/insights/employee/999999?days=$DAYS")
log "Non-existent employee insights HTTP=$EMP_BAD"

sec "Negative: unit insights non-existent unit 999999 (expect 200 with zeros)"
UNIT_BAD=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/insights/unit/999999?days=$DAYS")
log "Non-existent unit insights HTTP=$UNIT_BAD"

sec "Negative: cohort insights with days=0 (edge)"
COHORT_ZERO=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/insights/cohort?days=0")
log "Cohort days=0 HTTP=$COHORT_ZERO"

sec "Summary matrix"
cat <<EOF | tee -a $LOG
Employee insights: HTTP=$EMP_CODE receivedCount=$REC_COUNT sentCount=$SENT_COUNT
Unit insights: HTTP=$UNIT_CODE count=$UNIT_COUNT totalPoints=$UNIT_POINTS
Cohort insights: HTTP=$COHORT_CODE buckets(0-90=$BUCKET_0_90,91-365=$BUCKET_91_365,366-9999=$BUCKET_366_9999)
Bad employee id HTTP=$EMP_BAD
Bad unit id HTTP=$UNIT_BAD
Cohort days=0 HTTP=$COHORT_ZERO
EOF

log "Insights endpoint verification complete. See $LOG for details."
