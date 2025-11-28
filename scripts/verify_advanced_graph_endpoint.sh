#!/usr/bin/env zsh
set -euo pipefail
BASE=${BASE:-http://localhost:8080}
LOG=tmp/advanced_graph_verification.log
mkdir -p tmp
: > $LOG

sec() { echo "\n==== $1 ====" | tee -a $LOG; }
log() { echo "[LOG] $1" | tee -a $LOG; }

sec "Health check (wait until UP)"
for i in {1..60}; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health" || true)
  if [[ "$code" == "200" ]]; then log "Server UP (HTTP 200)"; break; fi
  sleep 1
  if [[ $i -eq 60 ]]; then log "Server did not become healthy"; exit 1; fi
done

SERIES_VALUES=(daily weekly weakly monthly quarterly yearly bogus)

sec "Advanced graph endpoint tests"
for s in ${SERIES_VALUES[@]}; do
  url="$BASE/recognitions/graph-advanced.png?series=$s&days=30"
  outfile="tmp/graph_${s}.png"
  http=$(curl -s -o "$outfile" -w '%{http_code}' "$url" || echo 000)
  if [[ "$http" == "200" ]]; then
    size=$(wc -c < "$outfile" 2>/dev/null || echo 0)
    type=$(file -b "$outfile" 2>/dev/null || echo 'N/A')
    log "series=$s HTTP=$http size=$size type=$type"
  else
    log "series=$s HTTP=$http (no file saved)"
    rm -f "$outfile" || true
  fi
done

sec "Summary"
cat $LOG | tail -n +1
log "Done. Full log at $LOG"

