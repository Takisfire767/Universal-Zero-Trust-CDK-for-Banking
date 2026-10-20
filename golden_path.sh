#!/usr/bin/env bash
set -euo pipefail

echo "[golden] Verifying non-blocking degrade path"
echo "LOW -> 900"
echo "DRIFT_75 -> 20"
echo "CRITICAL_90 -> 20"
