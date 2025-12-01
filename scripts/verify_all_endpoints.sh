#!/usr/bin/env zsh
# Comprehensive endpoint verification script for the current API surface.
# It exercises admin, employees, recognition-types, recognitions, metrics, leaderboard, and reports.
# Writes a detailed log in artifacts/verification_logs/<mm-dd-yyyy>/verify_all_<mm-dd-yyyy-hh.mm.ss>.log

set -uo pipefail

BASE=${BASE:-http://localhost:8080}
CSV_DIR=${CSV_DIR:-tmp}
ARTIFACTS_PATH=${ARTIFACTS_PATH:-./artifacts}

# Timestamp and day in user-requested formats: mm-dd-yyyy-hh.mm.ss and mm-dd-yyyy
TS=$(date +%m-%d-%Y-%H.%M.%S)
DAY=$(date +%m-%d-%Y)
# create a per-day verification_logs base
LOG_BASE_DIR=${ARTIFACTS_PATH}/verification_logs/${DAY}
LOG_DIR=${LOG_BASE_DIR}
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/verify_all_${TS}.log"

jq_exists() { command -v jq >/dev/null 2>&1; }

# portable ISO timestamp for log lines (UTC)
now_iso() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }

log() { printf "%s %s\n" "$(now_iso)" "$1" | tee -a "$LOG_FILE"; }
log_section() { log "---- $1 ----"; }

# helper to run a request and log response body + HTTP code
run_curl() {
  local method=$1; shift
  local url=$1; shift
  local out=/tmp/verify_resp_body.json
  local code
  if [[ "$method" = "GET" ]]; then
    code=$(curl -sS -w '%{http_code}' -o "$out" "$url" "$@" 2>/tmp/verify_curl_err || true)
  else
    code=$(curl -sS -w '%{http_code}' -o "$out" -X "$method" "$url" "$@" 2>/tmp/verify_curl_err || true)
  fi
  log "REQUEST: $method $url"
  if [[ -s /tmp/verify_curl_err ]]; then
    log "(curl stderr)"; sed -n '1,200p' /tmp/verify_curl_err | sed 's/^/  /' >> "$LOG_FILE"
    : > /tmp/verify_curl_err
  fi
  if jq_exists; then
    # try to pretty-print JSON, otherwise show raw
    if jq -e . "$out" >/dev/null 2>&1; then
      echo "RESPONSE (HTTP $code):" | tee -a "$LOG_FILE"
      jq . "$out" | sed 's/^/  /' >> "$LOG_FILE"
    else
      echo "RESPONSE (HTTP $code) (raw):" | tee -a "$LOG_FILE"
      sed -n '1,200p' "$out" | sed 's/^/  /' >> "$LOG_FILE"
    fi
  else
    echo "RESPONSE (HTTP $code) saved to $out" | tee -a "$LOG_FILE"
  fi
  echo >> "$LOG_FILE"
  printf "%s" "$code"
}

# Start verification
log "Starting API verification against $BASE"

# 1) Health
log_section "Health"
run_curl GET "$BASE/actuator/health"

# 2) Admin: dev-mode and seed gating
log_section "Admin - Dev Mode"
run_curl GET "$BASE/admin/dev-mode"
run_curl PATCH "$BASE/admin/dev-mode" -H 'Content-Type: application/json' -d '{"enabled":true}'

log_section "Admin - Upload negative test (missing file)"
run_curl POST "$BASE/admin/data/upload"

log_section "Admin - Upload combined sample"
if [[ -f "$CSV_DIR/sample_combined.csv" ]]; then
  run_curl POST "$BASE/admin/data/upload" -F "combined=@$CSV_DIR/sample_combined.csv"
else
  log "No $CSV_DIR/sample_combined.csv; skipping"
fi

log_section "Admin - Upload per-table samples"
args=()
[[ -f "$CSV_DIR/sample_employees.csv" ]] && args+=( -F "employees=@$CSV_DIR/sample_employees.csv" )
[[ -f "$CSV_DIR/sample_recognition_types.csv" ]] && args+=( -F "recognition_types=@$CSV_DIR/sample_recognition_types.csv" )
[[ -f "$CSV_DIR/sample_recognitions.csv" ]] && args+=( -F "recognitions=@$CSV_DIR/sample_recognitions.csv" )
if (( ${#args[@]} )); then
  run_curl POST "$BASE/admin/data/upload" "${args[@]}"
else
  log "No per-table sample files; skipping"
fi

log_section "Admin - Download combined"
run_curl GET "$BASE/admin/data/download?format=combined"

log_section "Admin - Download recognitions"
run_curl GET "$BASE/admin/data/download?format=recognitions"

log_section "Admin - Seed (should succeed if dev-mode enabled)"
run_curl POST "$BASE/admin/seed/run"

# 3) Employees CRUD smoke tests
log_section "Employees - List"
EMP_LIST_CODE=$(run_curl GET "$BASE/employees?page=0&size=5")

log_section "Employees - Create"
EMP_CREATE_RESP_FILE=/tmp/verify_emp_create.json
EMP_CREATE_CODE=$(curl -sS -w '%{http_code}' -o "$EMP_CREATE_RESP_FILE" -X POST -H 'Content-Type: application/json' -d '{"firstName":"Smoke","lastName":"Test","email":"smoke@example.com","role":"employee"}' "$BASE/employees" 2>/dev/null || true)
log "Create HTTP $EMP_CREATE_CODE"
if jq_exists && jq -e . "$EMP_CREATE_RESP_FILE" >/dev/null 2>&1; then
  jq . "$EMP_CREATE_RESP_FILE" | sed 's/^/  /' >> "$LOG_FILE"
fi
EMP_ID=$(jq -r '.id // .id' "$EMP_CREATE_RESP_FILE" 2>/dev/null || echo "")
log "Created employee id: $EMP_ID"

if [[ -n "$EMP_ID" && "$EMP_ID" != "null" ]]; then
  log_section "Employees - Get by id"
  run_curl GET "$BASE/employees/$EMP_ID"

  log_section "Employees - Update"
  run_curl PUT "$BASE/employees/$EMP_ID" -H 'Content-Type: application/json' -d '{"role":"manager"}'

  log_section "Employees - Delete"
  run_curl DELETE "$BASE/employees/$EMP_ID"
else
  log "Skipping employee get/update/delete; no created id"
fi

# 4) Recognition types
log_section "Recognition Types - list"
run_curl GET "$BASE/recognition-types"

log_section "Recognition Types - create"
RT_RESP=/tmp/verify_rt_create.json
RT_CODE=$(curl -sS -w '%{http_code}' -o "$RT_RESP" -X POST -H 'Content-Type: application/json' -d '{"typeName":"smoke_test_type"}' "$BASE/recognition-types" 2>/dev/null || true)
log "Create HTTP $RT_CODE"
if jq_exists && jq -e . "$RT_RESP" >/dev/null 2>&1; then jq . "$RT_RESP" | sed 's/^/  /' >> "$LOG_FILE"; fi
RT_ID=$(jq -r '.id // empty' "$RT_RESP" 2>/dev/null || echo "")
log "Created recognition_type id: $RT_ID"

# 5) Recognitions - list, create, exports
log_section "Recognitions - list"
run_curl GET "$BASE/recognitions?page=0&size=5"

if [[ -n "$RT_ID" && -n "$EMP_ID" ]]; then
  log_section "Recognitions - create"
  REC_RESP=/tmp/verify_rec_create.json
  # build JSON safely (avoid broken quoting into -d)
  REC_JSON="{\"recognitionTypeId\":\"$RT_ID\",\"recipientId\":$EMP_ID,\"senderId\":$EMP_ID,\"message\":\"smoke recognition\",\"awardPoints\":5}"
  REC_CODE=$(curl -sS -w '%{http_code}' -o "$REC_RESP" -X POST -H 'Content-Type: application/json' -d "$REC_JSON" "$BASE/recognitions" 2>/dev/null || true)
  log "Create HTTP $REC_CODE"
  if jq_exists && jq -e . "$REC_RESP" >/dev/null 2>&1; then jq . "$REC_RESP" | sed 's/^/  /' >> "$LOG_FILE"; fi
else
  log "Skipping recognition create; missing RT_ID or EMP_ID"
fi

log_section "Recognitions - export CSV"
run_curl GET "$BASE/recognitions/export.csv"

log_section "Recognitions - export JSON"
run_curl GET "$BASE/recognitions/export.json"

# 6) Insights / graphs / metrics / leaderboard / reports
log_section "Insights - sample"
run_curl GET "$BASE/insights/employee/1?days=30"

log_section "Metrics - summary"
run_curl GET "$BASE/metrics/summary?days=30"

log_section "Leaderboard - top-senders"
run_curl GET "$BASE/leaderboard/top-senders"

log_section "Reports - list"
run_curl GET "$BASE/reports/list"

# 7) Clean up: disable dev mode
log_section "Cleanup - disable dev-mode"
run_curl PATCH "$BASE/admin/dev-mode" -H 'Content-Type: application/json' -d '{"enabled":false}'

log "Verification complete. Log saved to $LOG_FILE"

# Collect artifacts into artifacts/exports/{graphs,json,csv} for easier access (non-fatal)
# (collect_exports.sh is now redundant - producers write directly into artifacts/exports)
log "Exports are produced directly under ${ARTIFACTS_PATH}/exports; skipping collection step"

exit 0
