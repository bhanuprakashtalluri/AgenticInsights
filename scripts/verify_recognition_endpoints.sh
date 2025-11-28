#!/usr/bin/env zsh
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
LOG=tmp/recognition_endpoint_verification.log
mkdir -p tmp
: > $LOG

log() { echo "[LOG] $1" | tee -a $LOG; }
sec() { echo "\n==== $1 ====" | tee -a $LOG; }
run() { echo "\n$ $1" | tee -a $LOG; setopt noglob; eval $1 2>&1 | tee -a $LOG; unsetopt noglob; }

sec "Health check"
for i in {1..60}; do
  if curl -s "$BASE/actuator/health" | grep -q 'UP'; then
    log "Server is UP"
    break
  fi
  sleep 1
  if [[ $i -eq 60 ]]; then
    log "Server did not become healthy in time"; exit 1
  fi
done

sec "List recognitions (page 0 size 3)"
LIST_JSON=$(curl -s "$BASE/recognitions?page=0&size=3")
FIRST_ID=$(echo $LIST_JSON | jq -r '.content[0].id // empty' || true)
FIRST_UUID=$(echo $LIST_JSON | jq -r '.content[0].uuid // empty' || true)
log "FIRST_ID=$FIRST_ID FIRST_UUID=$FIRST_UUID"

if [[ -z "$FIRST_ID" || -z "$FIRST_UUID" ]]; then
  log "Could not obtain initial recognition id/uuid"; fi

sec "Create recognition A"
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
CREATE_A_JSON=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST -H 'Content-Type: application/json' \
  -d '{"recognitionTypeId":1,"awardName":"Great Job","level":"gold","recipientId":2,"senderId":3,"sentAt":"'$NOW'","message":"Initial message A","awardPoints":10}' \
  "$BASE/recognitions")
HTTP_A=$(echo $CREATE_A_JSON | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
REC_A_ID=$(echo $CREATE_A_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.id // empty')
log "Created recognition A id=$REC_A_ID status=$HTTP_A"

sec "Approve recognition A"
APPROVE_JSON=$(curl -s -w 'HTTP_CODE=%{http_code}' -X PATCH "$BASE/recognitions/$REC_A_ID/approve")
HTTP_APPROVE=$(echo $APPROVE_JSON | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
STATUS_A=$(echo $APPROVE_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.approvalStatus // empty')
log "Approve result status=$HTTP_APPROVE approvalStatus=$STATUS_A"

sec "Create recognition B"
CREATE_B_JSON=$(curl -s -w 'HTTP_CODE=%{http_code}' -X POST -H 'Content-Type: application/json' \
  -d '{"recognitionTypeId":1,"awardName":"Awesome Work","level":"silver","recipientId":4,"senderId":5,"sentAt":"'$NOW'","message":"Initial message B","awardPoints":5}' \
  "$BASE/recognitions")
HTTP_B=$(echo $CREATE_B_JSON | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
REC_B_ID=$(echo $CREATE_B_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.id // empty')
log "Created recognition B id=$REC_B_ID status=$HTTP_B"

sec "Reject recognition B with reason"
REJECT_JSON=$(curl -s -w 'HTTP_CODE=%{http_code}' -X PATCH "$BASE/recognitions/$REC_B_ID/reject?reason=Insufficient%20details")
HTTP_REJECT=$(echo $REJECT_JSON | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
STATUS_B=$(echo $REJECT_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.approvalStatus // empty')
REASON_B=$(echo $REJECT_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.rejectionReason // empty')
log "Reject result status=$HTTP_REJECT approvalStatus=$STATUS_B rejectionReason=$REASON_B"

sec "Attempt reject recognition B without reason (expect 400)"
REJECT_BAD_JSON=$(curl -s -w 'HTTP_CODE=%{http_code}' -X PATCH "$BASE/recognitions/$REC_B_ID/reject")
HTTP_REJECT_BAD=$(echo $REJECT_BAD_JSON | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
log "Reject without reason HTTP=$HTTP_REJECT_BAD"

sec "Get recognition by id (A)"
run "curl -s $BASE/recognitions/$REC_A_ID | jq ."

sec "Get recognition by id (B)"
run "curl -s $BASE/recognitions/$REC_B_ID | jq ."

if [[ -n "$FIRST_UUID" ]]; then
  sec "Get recognition by UUID (first)"
  run "curl -s $BASE/recognitions/uuid/$FIRST_UUID | jq ."
fi

sec "Update recognition A (message + points)"
UPDATE_JSON=$(curl -s -w 'HTTP_CODE=%{http_code}' -X PUT -H 'Content-Type: application/json' \
  -d '{"message":"Updated message A","awardPoints":42}' "$BASE/recognitions/$REC_A_ID")
HTTP_UPDATE=$(echo $UPDATE_JSON | sed -n 's/.*HTTP_CODE=\([0-9]*\).*/\1/p')
UPDATED_POINTS=$(echo $UPDATE_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.awardPoints // empty')
UPDATED_MSG=$(echo $UPDATE_JSON | sed 's/HTTP_CODE=.*//' | jq -r '.message // empty')
log "Update A HTTP=$HTTP_UPDATE points=$UPDATED_POINTS message=$UPDATED_MSG"

sec "Export CSV"
run "curl -s -o tmp/recognitions.csv $BASE/recognitions/export.csv && head -n 3 tmp/recognitions.csv"

sec "Export JSON"
run "curl -s $BASE/recognitions/export.json | jq '. | length'"

sec "Export Stream CSV"
run "curl -s -o tmp/recognitions_stream.csv $BASE/recognitions/export-stream.csv && head -n 3 tmp/recognitions_stream.csv"

sec "Insights (30 days)"
run "curl -s '$BASE/recognitions/insights?days=30' | jq 'keys'"

sec "Graph PNG (30 days)"
run "curl -s -o tmp/graph.png '$BASE/recognitions/graph.png?days=30' && file tmp/graph.png || ls -l tmp/graph.png"

sec "Role insights (role=employee)"
run "curl -s '$BASE/recognitions/insights/role?role=employee&days=30' | jq '.summary'"

sec "Role graph PNG"
run "curl -s -o tmp/role_graph.png '$BASE/recognitions/insights/role/graph.png?role=employee&days=30' && file tmp/role_graph.png || ls -l tmp/role_graph.png"

sec "Manager insights (managerId=1)"
run "curl -s '$BASE/recognitions/insights/manager/1?days=30' | jq '.summary'"

sec "Manager graph PNG"
run "curl -s -o tmp/manager_graph.png '$BASE/recognitions/insights/manager/1/graph.png?days=30' && file tmp/manager_graph.png || ls -l tmp/manager_graph.png"

sec "Advanced graph PNG"
run "curl -s -o tmp/graph_adv.png '$BASE/recognitions/graph-advanced.png?metric=count&series=daily&days=30' && file tmp/graph_adv.png || ls -l tmp/graph_adv.png"

sec "Delete recognition A"
DEL_CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/recognitions/$REC_A_ID")
log "Delete A HTTP=$DEL_CODE"

sec "Fetch deleted recognition A (expect 404)"
GET_AFTER_DEL=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/recognitions/$REC_A_ID")
log "GET after delete HTTP=$GET_AFTER_DEL"

sec "Negative: GET non-existent id 999999"
NEG_CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/recognitions/999999")
log "GET /recognitions/999999 HTTP=$NEG_CODE"

sec "Summary matrix"
cat <<EOF | tee -a $LOG
Created A: $HTTP_A
Approved A: $HTTP_APPROVE ($STATUS_A)
Created B: $HTTP_B
Rejected B: $HTTP_REJECT ($STATUS_B / $REASON_B)
Reject without reason (B): $HTTP_REJECT_BAD (expect 400)
Update A: $HTTP_UPDATE
Delete A: $DEL_CODE (expect 204)
Get after delete: $GET_AFTER_DEL (expect 404)
Negative get 999999: $NEG_CODE (expect 404)
EOF

log "Verification complete. See $LOG for full details."
