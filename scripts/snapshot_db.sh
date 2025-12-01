#!/usr/bin/env zsh
# Dump all user tables in the public schema into timestamped CSV files under a temporary folder (./tmp/snapshots/<ts>/) for review.
# Requires psql in PATH and access to the Postgres database.
# Usage:
#   PGHOST=localhost PGPORT=5432 PGDATABASE=recognitions PGUSER=postgres PGPASSWORD=secret \
#     ARTIFACTS_PATH=./artifacts ./scripts/snapshot_db.sh

set -euo pipefail

: ${PGHOST:=localhost}
: ${PGPORT:=5432}
: ${PGDATABASE:=recognitions}
: ${PGUSER:=postgres}
: ${ARTIFACTS_PATH:=./artifacts}
: ${TMP_BASE:=./tmp}
# PGPASSWORD may be set in env; if not, we will fail fast to avoid interactive prompt.

# Timestamp and day in user-requested formats: mm-dd-yyyy-hh.mm.ss and mm-dd-yyyy
TS=$(date +%m-%d-%Y-%H.%M.%S)
# Use a temporary project-local directory for CSV snapshots so they can be inspected before moving
DEST=${TMP_BASE}/snapshots/${TS}
mkdir -p "$DEST"
# Use mm-dd-yyyy for the day folder to match verification logs
DAY=$(date +%m-%d-%Y)
LOG_BASE_DIR=${ARTIFACTS_PATH}/snapshot_logs/${DAY}
LOG_DIR=${LOG_BASE_DIR}
mkdir -p "$LOG_DIR"
LOGFILE=${LOG_DIR}/snapshot_${TS}.log

now_iso() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }
log() { printf "%s %s\n" "$(now_iso)" "$1" | tee -a "$LOGFILE"; }

log "Starting DB snapshot to $DEST"

# Discover all user tables in public schema, excluding flyway schema history
PSQL_ARGS=( -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -t -A )

list_tables() {
  if [[ -n "${PGPASSWORD:-}" ]]; then
    PGPASSWORD="$PGPASSWORD" psql "${PSQL_ARGS[@]}" -c "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT LIKE 'flyway_%' ORDER BY tablename;"
  else
    # if no password env var set, run psql with -w to avoid password prompt and let it fail
    psql -w "${PSQL_ARGS[@]}" -c "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT LIKE 'flyway_%' ORDER BY tablename;"
  fi
}

tables=$(list_tables)
if [[ -z "$tables" ]]; then
  log "No tables found in public schema or unable to query. Aborting."
  exit 1
fi

log "Found tables:"
log "$tables"

# Export each table using psql \copy (client-side copy writes CSV files)
while IFS= read -r t; do
  # sanitize table name
  if [[ -z "$t" ]]; then continue; fi
  out="$DEST/${t}.csv"
  log "Exporting table $t -> $out"
  if [[ -n "${PGPASSWORD:-}" ]]; then
    PGPASSWORD="$PGPASSWORD" psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "\copy (SELECT * FROM \"${t}\") TO '${out}' CSV HEADER"
  else
    psql -w -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "\copy (SELECT * FROM \"${t}\") TO '${out}' CSV HEADER"
  fi
  log "Exported $t to $out"
done <<< "$tables"

log "Snapshot complete; files in $DEST:"
ls -la "$DEST" | sed 's/^/  /' >> "$LOGFILE"
log "Snapshot finished"

echo "Snapshot written to $DEST (log: $LOGFILE)"
