#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/recognition_type_verification.log
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

sec "List initial recognition types"
LIST=$(curl -s "$BASE/recognition-types")
COUNT_INIT=$(echo $LIST | jq 'length')
log "Initial count=$COUNT_INIT"
FIRST_ID=$(echo $LIST | jq -r '.[0].id')
FIRST_UUID=$(echo $LIST | jq -r '.[0].uuid')
log "First type id=$FIRST_ID uuid=$FIRST_UUID"

sec "Get by id (first)"
run "curl -s $BASE/recognition-types/$FIRST_ID | jq ."

sec "Get by uuid (first)"
run "curl -s $BASE/recognition-types/uuid/$FIRST_UUID | jq ."

NEW_NAME="special_card_$(date +%s)"
sec "Create new recognition type $NEW_NAME"
CREATE=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST -H 'Content-Type: application/json' -d '{"typeName":"'$NEW_NAME'","createdBy":1}' "$BASE/recognition-types")
CREATE_CODE=$(echo $CREATE | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
CREATE_JSON=$(echo $CREATE | sed 's/HTTP_CODE=.*//')
NEW_ID=$(safe_jq "$CREATE_JSON" '.id')
NEW_UUID=$(safe_jq "$CREATE_JSON" '.uuid')
log "Create HTTP=$CREATE_CODE id=$NEW_ID uuid=$NEW_UUID"

sec "Attempt duplicate (case-insensitive) of existing seed 'award' as 'AWARD' (expect failure)"
DUP=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{"typeName":"AWARD"}' "$BASE/recognition-types")
log "Duplicate create HTTP=$DUP"

sec "Attempt create without typeName (expect failure)"
NO_NAME=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' "$BASE/recognition-types")
log "Empty body create HTTP=$NO_NAME"

sec "Update new type name -> ${NEW_NAME}_updated"
UPDATED_NAME="${NEW_NAME}_updated"
UPDATE=$(curl -s -w 'HTTP_CODE=%{http_code}' -X PUT -H 'Content-Type: application/json' -d '{"typeName":"'$UPDATED_NAME'"}' "$BASE/recognition-types/$NEW_ID")
UPDATE_CODE=$(echo $UPDATE | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
UP_JSON=$(echo $UPDATE | sed 's/HTTP_CODE=.*//')
UP_NAME=$(safe_jq "$UP_JSON" '.typeName')
log "Update HTTP=$UPDATE_CODE typeName=$UP_NAME"

sec "Attempt update to conflicting name 'ecard' (expect failure)"
CONFLICT=$(curl -s -o /dev/null -w '%{http_code}' -X PUT -H 'Content-Type: application/json' -d '{"typeName":"ecard"}' "$BASE/recognition-types/$NEW_ID")
log "Conflict update HTTP=$CONFLICT"

sec "Delete created type"
DEL=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/recognition-types/$NEW_ID")
log "Delete HTTP=$DEL"

sec "Get deleted type (expect 404)"
GET_DELETED=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/recognition-types/$NEW_ID")
log "Get after delete HTTP=$GET_DELETED"

sec "Negative: get non-existent id 999999 (expect 404)"
NEG_GET=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/recognition-types/999999")
log "GET /recognition-types/999999 HTTP=$NEG_GET"

sec "Final list count"
LIST_FINAL=$(curl -s "$BASE/recognition-types")
COUNT_FINAL=$(echo $LIST_FINAL | jq 'length')
log "Final count=$COUNT_FINAL (should equal initial count if delete succeeded)"

sec "Summary matrix"
cat <<EOF | tee -a $LOG
Initial list count: $COUNT_INIT
Create new type: $CREATE_CODE (id=$NEW_ID)
Duplicate case-insensitive 'AWARD': $DUP (expect !=201)
Create without typeName: $NO_NAME (expect failure)
Update name: $UPDATE_CODE (new=$UP_NAME)
Conflict update to 'ecard': $CONFLICT (expect !=200)
Delete created: $DEL (expect 204)
Get deleted: $GET_DELETED (expect 404)
Get non-existent 999999: $NEG_GET (expect 404)
Final count: $COUNT_FINAL
EOF

log "Recognition type endpoint verification complete. See $LOG for details."
