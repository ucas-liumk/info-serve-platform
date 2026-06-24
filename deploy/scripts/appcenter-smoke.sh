#!/usr/bin/env bash
# appcenter-smoke.sh — End-to-end API smoke test for ruoyi-appcenter via gateway.
# Usage: bash deploy/scripts/appcenter-smoke.sh
# Requires: node (for login helper), curl, docker (for PG checks)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="http://127.0.0.1:19100/prod-api"
CID="e5cd7e4891bf95d1d19206ce24a7b32e"

echo "=== AppCenter Smoke Test ==="
echo ""

# --- Get token ---
echo "[1/8] Obtaining admin token..."
LOGIN_OUT=$(node "${SCRIPT_DIR}/appcenter-login.mjs" 2>/dev/null)
TOKEN=$(echo "$LOGIN_OUT" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
  echo "FAIL: Could not obtain access_token. Login response:"
  echo "$LOGIN_OUT"
  exit 1
fi
echo "  token: ${TOKEN:0:40}..."

AUTH="-H 'Authorization: Bearer $TOKEN' -H 'clientid: $CID'"

# Helper function
api_get() {
  local url="$1"
  curl -sf "$url" \
    -H "Authorization: Bearer $TOKEN" \
    -H "clientid: $CID"
}

api_post() {
  local url="$1"
  curl -sf -X POST "$url" \
    -H "Authorization: Bearer $TOKEN" \
    -H "clientid: $CID"
}

# --- categories ---
echo ""
echo "[2/8] GET /appcenter/portal/categories (expect 5 items)..."
CAT_RESP=$(api_get "${BASE_URL}/appcenter/portal/categories")
echo "  Response: $(echo "$CAT_RESP" | head -c 300)"
echo ""
if ! echo "$CAT_RESP" | grep -q '"code":200'; then
  echo "FAIL: categories did not return code:200"
  exit 1
fi
CAT_COUNT=$(echo "$CAT_RESP" | grep -o '"categoryId"' | wc -l | tr -d ' ')
echo "  Categories found: $CAT_COUNT"
if [ "$CAT_COUNT" -lt 5 ]; then
  echo "FAIL: expected >=5 categories, got $CAT_COUNT"
  exit 1
fi
echo "  PASS: $CAT_COUNT categories"

# --- apps list ---
# Note: /appcenter/portal/apps returns TableDataInfo directly ({"total":N,"rows":[...]})
# rather than the standard R wrapper ({"code":200,"data":...})
echo ""
echo "[3/8] GET /appcenter/portal/apps?pageNum=1&pageSize=8 (expect >=6 rows with favorited/recommended)..."
APPS_RESP=$(api_get "${BASE_URL}/appcenter/portal/apps?pageNum=1&pageSize=8")
echo "  Response: $(echo "$APPS_RESP" | head -c 400)"
echo ""
if ! echo "$APPS_RESP" | grep -q '"rows"'; then
  echo "FAIL: apps list did not return rows field"
  exit 1
fi
if ! echo "$APPS_RESP" | grep -q '"favorited"'; then
  echo "FAIL: apps response missing 'favorited' field"
  exit 1
fi
if ! echo "$APPS_RESP" | grep -q '"recommended"'; then
  echo "FAIL: apps response missing 'recommended' field"
  exit 1
fi
APPS_TOTAL=$(echo "$APPS_RESP" | grep -o '"total":[0-9]*' | head -1 | cut -d: -f2)
echo "  Total apps: $APPS_TOTAL"
if [ "${APPS_TOTAL:-0}" -lt 6 ]; then
  echo "FAIL: expected total>=6, got $APPS_TOTAL"
  exit 1
fi
echo "  PASS: total=$APPS_TOTAL, has favorited+recommended fields"

# --- use app 1 ---
echo ""
echo "[4/8] POST /appcenter/portal/apps/1/use (expect code:200, data=URL)..."
USE_RESP=$(api_post "${BASE_URL}/appcenter/portal/apps/1/use")
echo "  Response: $(echo "$USE_RESP" | head -c 300)"
echo ""
if ! echo "$USE_RESP" | grep -q '"code":200'; then
  echo "FAIL: use did not return code:200"
  exit 1
fi
if ! echo "$USE_RESP" | grep -qiE '"data":"http'; then
  echo "FAIL: use response data is not an http URL"
  exit 1
fi
echo "  PASS: use returned http URL"

# --- favorite app 1 (first time) ---
echo ""
echo "[5/8] POST /appcenter/portal/apps/1/favorite (expect code:200, idempotent)..."
FAV1_RESP=$(api_post "${BASE_URL}/appcenter/portal/apps/1/favorite")
echo "  1st call: $(echo "$FAV1_RESP" | head -c 200)"
echo ""
if ! echo "$FAV1_RESP" | grep -q '"code":200'; then
  echo "FAIL: 1st favorite did not return code:200"
  exit 1
fi

FAV2_RESP=$(api_post "${BASE_URL}/appcenter/portal/apps/1/favorite")
echo "  2nd call: $(echo "$FAV2_RESP" | head -c 200)"
echo ""
if ! echo "$FAV2_RESP" | grep -q '"code":200'; then
  echo "FAIL: 2nd favorite (idempotent) did not return code:200"
  exit 1
fi
echo "  PASS: favorite is idempotent"

# --- recommend app 1 ---
echo ""
echo "[6/8] POST /appcenter/portal/apps/1/recommend (expect code:200)..."
REC_RESP=$(api_post "${BASE_URL}/appcenter/portal/apps/1/recommend")
echo "  Response: $(echo "$REC_RESP" | head -c 200)"
echo ""
if ! echo "$REC_RESP" | grep -q '"code":200'; then
  echo "FAIL: recommend did not return code:200"
  exit 1
fi
echo "  PASS: recommend returned 200"

# --- favorites list ---
# Note: returns TableDataInfo directly ({"total":N,"rows":[...]})
echo ""
echo "[7/8] GET /appcenter/portal/favorites (expect app 1 present)..."
FAVS_RESP=$(api_get "${BASE_URL}/appcenter/portal/favorites")
echo "  Response: $(echo "$FAVS_RESP" | head -c 300)"
echo ""
if ! echo "$FAVS_RESP" | grep -q '"rows"'; then
  echo "FAIL: favorites list did not return rows field"
  exit 1
fi
FAVS_COUNT=$(echo "$FAVS_RESP" | grep -o '"appId"' | wc -l | tr -d ' ')
if [ "${FAVS_COUNT:-0}" -lt 1 ]; then
  echo "FAIL: expected >=1 item in favorites, got $FAVS_COUNT"
  exit 1
fi
echo "  PASS: favorites contains $FAVS_COUNT app(s)"

# --- admin application list ---
# Note: returns TableDataInfo directly ({"total":N,"rows":[...]})
echo ""
echo "[8/8] GET /appcenter/application/list?pageNum=1&pageSize=10 (admin perm, expect rows)..."
ADMIN_RESP=$(api_get "${BASE_URL}/appcenter/application/list?pageNum=1&pageSize=10")
echo "  Response: $(echo "$ADMIN_RESP" | head -c 300)"
echo ""
if ! echo "$ADMIN_RESP" | grep -q '"rows"'; then
  echo "FAIL: admin application list did not return rows field"
  exit 1
fi
ADMIN_TOTAL=$(echo "$ADMIN_RESP" | grep -o '"total":[0-9]*' | head -1 | cut -d: -f2)
echo "  PASS: admin list returned total=$ADMIN_TOTAL"

# --- PG database checks ---
echo ""
echo "=== Database Verification ==="
echo "use_count and favorite count in PostgreSQL:"
docker exec infosys-ruoyi-cloud-plus-postgres psql -U ruoyi -d ry-cloud -c \
  "SELECT app_id, use_count FROM app_application WHERE app_id=1; SELECT COUNT(*) AS favorite_count FROM app_favorite;" 2>&1

echo ""
echo "=== SMOKE TEST PASSED ==="
