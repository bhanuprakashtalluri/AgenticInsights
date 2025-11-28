#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/employee_endpoint_verification.log
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

sec "List employees (page size 5)"
LIST=$(curl -s "$BASE/employees?page=0&size=5")
FIRST_ID=$(echo $LIST | jq -r '.content[0].id')
FIRST_UUID=$(echo $LIST | jq -r '.content[0].uuid')
log "First employee id=$FIRST_ID uuid=$FIRST_UUID"
COUNT_PAGE=$(echo $LIST | jq -r '.content | length')
log "Page count=$COUNT_PAGE"

sec "List employees filtered by role=manager (size 3)"
LIST_MGR=$(curl -s "$BASE/employees?page=0&size=3&role=manager")
MGR_COUNT=$(echo $LIST_MGR | jq -r '.content | length')
log "Manager page count=$MGR_COUNT"

sec "Get employee by id (first)"
run "curl -s $BASE/employees/$FIRST_ID | jq ."

sec "Get employee by uuid (first)"
run "curl -s $BASE/employees/uuid/$FIRST_UUID | jq ."

NEW_EMAIL="new.employee.$(date +%s)@example.com"
JOIN_DATE=$(date -u +%Y-%m-%d)
sec "Create new employee"
CREATE=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST -H 'Content-Type: application/json' -d '{"firstName":"New","lastName":"Employee","unitId":99,"managerId":'$FIRST_ID',"email":"'$NEW_EMAIL'","joiningDate":"'$JOIN_DATE'","role":"employee"}' "$BASE/employees")
CREATE_CODE=$(echo $CREATE | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
CREATE_JSON=$(echo $CREATE | sed 's/HTTP_CODE=.*//')
NEW_ID=$(safe_jq "$CREATE_JSON" '.id')
NEW_UUID=$(safe_jq "$CREATE_JSON" '.uuid')
log "Create HTTP=$CREATE_CODE id=$NEW_ID uuid=$NEW_UUID email=$NEW_EMAIL"

sec "Update new employee (role upgrade to teamlead + unit change)"
UPDATE=$(curl -s -w 'HTTP_CODE=%{http_code}' -X PUT -H 'Content-Type: application/json' -d '{"role":"teamlead","unitId":100}' "$BASE/employees/$NEW_ID")
UPDATE_CODE=$(echo $UPDATE | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
UP_JSON=$(echo $UPDATE | sed 's/HTTP_CODE=.*//')
UP_ROLE=$(safe_jq "$UP_JSON" '.role')
UP_UNIT=$(safe_jq "$UP_JSON" '.unitId')
log "Update HTTP=$UPDATE_CODE role=$UP_ROLE unitId=$UP_UNIT"

sec "Delete new employee"
DEL=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/employees/$NEW_ID")
log "Delete HTTP=$DEL"

sec "Get deleted employee (expect 404)"
GET_DEL=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/employees/$NEW_ID")
log "Get after delete HTTP=$GET_DEL"

sec "Negative create (missing firstName)"
BAD_CREATE=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{"lastName":"MissingFirst"}' "$BASE/employees")
log "Bad create HTTP=$BAD_CREATE"

sec "Negative update non-existent id 999999 (expect 404)"
BAD_UPDATE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT -H 'Content-Type: application/json' -d '{"role":"teamlead"}' "$BASE/employees/999999")
log "Bad update HTTP=$BAD_UPDATE"

sec "Negative delete non-existent id 999999 (expect 404)"
BAD_DEL=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/employees/999999")
log "Bad delete HTTP=$BAD_DEL"

sec "Summary matrix"
cat <<EOF | tee -a $LOG
List first page size=5 count=$COUNT_PAGE
Filter role=manager managerPageCount=$MGR_COUNT
Create new employee: $CREATE_CODE (id=$NEW_ID)
Update new employee: $UPDATE_CODE (role=$UP_ROLE unitId=$UP_UNIT)
Delete new employee: $DEL (expect 204)
Get deleted employee: $GET_DEL (expect 404)
Bad create missing firstName: $BAD_CREATE (expect failure)
Bad update non-existent id: $BAD_UPDATE (expect 404)
Bad delete non-existent id: $BAD_DEL (expect 404)
EOF

log "Employee endpoint verification complete. See $LOG for details."
