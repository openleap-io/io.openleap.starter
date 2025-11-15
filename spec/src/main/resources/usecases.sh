#!/usr/bin/env bash
set -Eeuo pipefail

# ============================
# Configuration (overrideable)
# ============================
BASE_URL=${BASE_URL:-"http://localhost:6161/api/fi/acc/v1"}
TENANT=${TENANT:-"00000000-0000-0000-0000-000000000001"}
TRACE_ID=${TRACE_ID:-"acc-usecases-$(date +%s)"}
PERIOD_CODE=${PERIOD_CODE:-"2025-12"}   # YYYY-MM used throughout
ALT_PERIOD_CODE=${ALT_PERIOD_CODE:-"2025-11"}
NEW_BASE_CCY=${NEW_BASE_CCY:-"USD"}

# ================
# Helper functions
# ================
require() {
  command -v "$1" >/dev/null 2>&1 || { echo "[FATAL] Required tool '$1' not found in PATH" >&2; exit 1; }
}

hr() { printf '%*s
' "${1:-80}" '' | tr ' ' -; }
step_n=0
step() { step_n=$((step_n+1)); echo; hr; echo "[STEP $step_n] $*"; hr; }
info() { echo "[INFO] $*"; }
warn() { echo "[WARN] $*"; }
err()  { echo "[ERROR] $*"; }

# do_request METHOD PATH [JSON_BODY]
# Sets globals: RESP_BODY, HTTP_CODE
# Prints a compact preview line
_do_curl() {
  local method="$1"; shift
  local url="$1"; shift
  local data="${1:-}"; shift || true

  local tmp_body; tmp_body="$(mktemp)"
  local args=( -sS -w "%{http_code}" -H "X-Tenant-Id: ${TENANT}" -H "X-Trace-Id: ${TRACE_ID}" )
  if [[ -n "$data" ]]; then
    args+=( -H "Content-Type: application/json" -X "$method" --data "$data" )
  else
    args+=( -X "$method" )
  fi

  # shellcheck disable=SC2129
  HTTP_CODE=$(curl "${args[@]}" "$url" -o "$tmp_body") || HTTP_CODE=$?
  RESP_BODY=$(cat "$tmp_body")
  rm -f "$tmp_body"
}

# Thin wrapper for API path join
api() { echo "$BASE_URL$1"; }

# Safe jq access (returns empty on error)
jq_get() {
  local json="$1"; shift
  local expr="$1"; shift
  echo "$json" | jq -er "$expr" 2>/dev/null || true
}

# ==============
# Pre-flight
# ==============
require curl
require jq

info "BASE_URL = $BASE_URL"
info "TENANT   = $TENANT"
info "TRACE_ID = $TRACE_ID"
info "PERIOD_CODE = $PERIOD_CODE, ALT_PERIOD_CODE = $ALT_PERIOD_CODE"

# Storage for summary
GL_BASE_CCY_BEFORE=""
GL_BASE_CCY_AFTER=""
PERIOD_ID=""
ALT_PERIOD_ID=""
SNAPSHOT_BEFORE_STATUS=""
SNAPSHOT_AFTER_STATUS=""
SNAPSHOT_ROWS=""
PERIOD_OPEN_AFTER_CLOSE_STATUS=""
REOPEN_STATUS=""

# =====================
# 1) GET /general-ledger
# =====================
step "Fetch General Ledger configuration"
_do_curl GET "$(api "/general-ledger")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq .
GL_BASE_CCY_BEFORE=$(jq_get "$RESP_BODY" '.baseCurrency // empty')
GL_CURRENT_PERIOD=$(jq_get "$RESP_BODY" '.currentPeriodId // empty')
info "baseCurrency(before) = ${GL_BASE_CCY_BEFORE:-<null>}"
info "currentPeriodId = ${GL_CURRENT_PERIOD:-<null>}"

# ======================================
# 2) POST /periods (ensure two periods)
# ======================================
create_period() {
  local ym="$1"; shift
  local year=${ym%-*}
  local month=${ym#*-}
  local payload
  payload=$(jq -n --argjson y "$year" --argjson m "$month" '{uuid:null, month:$m, year:$y, status:"OPEN"}')
  _do_curl POST "$(api "/periods")" "$payload"
  echo "HTTP $HTTP_CODE"
  echo "$RESP_BODY" | jq .
  # Success both on 201 (created) and 200 (idempotent existing)
  if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]]; then
    jq_get "$RESP_BODY" '.uuid // empty'
  else
    warn "Unexpected status creating period $ym"
    echo ""
  fi
}

step "Ensure period $PERIOD_CODE exists"
PERIOD_ID=$(create_period "$PERIOD_CODE")
info "PERIOD_ID($PERIOD_CODE) = ${PERIOD_ID:-<null>}"

step "Ensure alternate period $ALT_PERIOD_CODE exists"
ALT_PERIOD_ID=$(create_period "$ALT_PERIOD_CODE")
info "ALT_PERIOD_ID($ALT_PERIOD_CODE) = ${ALT_PERIOD_ID:-<null>}"

# ==============================================
# 3) GET /periods (list + filters) + current
# ==============================================
step "List all periods"
_do_curl GET "$(api "/periods")"
echo "HTTP $HTTP_CODE"
COUNT_ALL=$(echo "$RESP_BODY" | jq 'length')
info "Periods count = $COUNT_ALL"

step "List periods year=2025"
_do_curl GET "$(api "/periods?year=2025")"
echo "HTTP $HTTP_CODE"
COUNT_2025=$(echo "$RESP_BODY" | jq 'length')
info "Periods(year=2025) count = $COUNT_2025"

step "Get current open period"
_do_curl GET "$(api "/periods/current")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq . || true

# =====================================================
# 4) PATCH /general-ledger (try to set base currency)
# =====================================================
step "Update General Ledger base currency to $NEW_BASE_CCY (may be 409 if postings exist)"
PATCH_PAYLOAD=$(jq -n --arg ccy "$NEW_BASE_CCY" '{baseCurrency:$ccy}')
_do_curl PATCH "$(api "/general-ledger")" "$PATCH_PAYLOAD"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq .
if [[ "$HTTP_CODE" == "200" ]]; then
  GL_BASE_CCY_AFTER=$(jq_get "$RESP_BODY" '.baseCurrency // empty')
  info "baseCurrency(after) = $GL_BASE_CCY_AFTER"
elif [[ "$HTTP_CODE" == "409" ]]; then
  warn "Conflict: base currency change is not allowed after postings"
  GL_BASE_CCY_AFTER="$GL_BASE_CCY_BEFORE"
else
  warn "Unexpected response on PATCH /general-ledger"
fi

# ====================================
# 5) GET /charts/current (metadata)
# ====================================
step "Get chart metadata"
_do_curl GET "$(api "/charts/current")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq .

# ======================================
# 6) Ledger queries for $PERIOD_CODE
# ======================================
step "Trial balance for $PERIOD_CODE"
_do_curl GET "$(api "/ledgers/trial-balance?period=$PERIOD_CODE")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq '.[:5]'

step "Balances for $PERIOD_CODE (all accounts)"
_do_curl GET "$(api "/ledgers/balances?period=$PERIOD_CODE")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq '.[:5]'

step "Opening balances for $PERIOD_CODE"
_do_curl GET "$(api "/ledgers/opening-balances?period=$PERIOD_CODE")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq '.[:5]'

# =========================================================
# 7) Snapshot before close (expected 404 if not frozen yet)
# =========================================================
step "Export snapshot for $PERIOD_CODE BEFORE close (expect 404)"
_do_curl GET "$(api "/ledgers/snapshots/$PERIOD_CODE")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq . || true
SNAPSHOT_BEFORE_STATUS="$HTTP_CODE"

# =============================================
# 8) Close the period (freeze snapshot + events)
# =============================================
if [[ -z "$PERIOD_ID" ]]; then
  err "No PERIOD_ID captured earlier for $PERIOD_CODE. Cannot close."
  exit 2
fi
step "Close period $PERIOD_CODE (id=$PERIOD_ID)"
_do_curl POST "$(api "/periods/$PERIOD_ID/close")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq .

# ======================================
# 9) Snapshot after close (should be 200)
# ======================================
step "Export snapshot for $PERIOD_CODE AFTER close (expect 200)"
_do_curl GET "$(api "/ledgers/snapshots/$PERIOD_CODE")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq '.[:5]'
SNAPSHOT_AFTER_STATUS="$HTTP_CODE"
SNAPSHOT_ROWS=$(echo "$RESP_BODY" | jq 'length' 2>/dev/null || echo "")

# ==================================================
# 10) Try opening CLOSED period (expect 409)
# ==================================================
step "Try to OPEN a CLOSED period (expect 409)"
_do_curl POST "$(api "/periods/$PERIOD_ID/open")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq . || true
PERIOD_OPEN_AFTER_CLOSE_STATUS="$HTTP_CODE"

# =================================
# 11) REOPEN closed period (200)
# =================================
step "Reopen period (should be 200)"
_do_curl POST "$(api "/periods/$PERIOD_ID/reopen")"
echo "HTTP $HTTP_CODE"
echo "$RESP_BODY" | jq .
REOPEN_STATUS="$HTTP_CODE"

# =============
# Final summary
# =============
hr
printf "Summary\n"
hr
cat <<EOF
General Ledger:
  baseCurrency(before): ${GL_BASE_CCY_BEFORE:-<unknown>}
  baseCurrency(after):  ${GL_BASE_CCY_AFTER:-<unknown>}
Periods:
  PERIOD_CODE:    $PERIOD_CODE
  PERIOD_ID:      ${PERIOD_ID:-<unknown>}
  ALT_PERIOD:     $ALT_PERIOD_CODE
  ALT_PERIOD_ID:  ${ALT_PERIOD_ID:-<unknown>}
Snapshots:
  BEFORE close:   HTTP $SNAPSHOT_BEFORE_STATUS
  AFTER close:    HTTP $SNAPSHOT_AFTER_STATUS, rows=${SNAPSHOT_ROWS:-n/a}
Transitions:
  open(CLOSED) attempt: HTTP ${PERIOD_OPEN_AFTER_CLOSE_STATUS:-n/a}
  reopen:               HTTP ${REOPEN_STATUS:-n/a}
EOF

info "Done. You can re-run with different PERIOD_CODE/ALT_PERIOD_CODE/NEW_BASE_CCY by exporting env vars first."