#!/usr/bin/env bash
# Carga contra el servidor desplegado (Corte 3, C3-04.3 a C3-04.6): SLO-CARGA-01 y SLO-RED-01.
# Uso, desde Minecraft2/:  perf/run-slo.sh [corta|larga]
#   corta: 3 repeticiones de 30 s (la que corre cd.yml después de cada despliegue).
#   larga: 3 repeticiones de 300 s (load.yml, workflow_dispatch).
# El servidor debe estar levantado con MC2_LOAD_ZOMBIES (80 en el contrato). Las variables MC2_* del
# contrato común eligen el destino; MC2_SLO_TICK_P95_MS y MC2_SLO_RTT_P95_MS permiten romper un umbral
# a propósito. Sale con código distinto de cero si falta un resultado, no se alcanza la población o se
# incumple cualquier umbral. Protocolo y umbrales: docs/devops.md.
set -euo pipefail

cd "$(dirname "$0")/.."

case "${1:-corta}" in
  corta) default_seconds=30 ;;
  larga) default_seconds=300 ;;
  *) echo "Uso: perf/run-slo.sh [corta|larga]" >&2; exit 2 ;;
esac

export MC2_SERVER_HOST="${MC2_SERVER_HOST:-localhost}"
export MC2_SERVER_PORT="${MC2_SERVER_PORT:-7777}"
export MC2_HEALTH_PORT="${MC2_HEALTH_PORT:-8080}"
export MC2_LOAD_ZOMBIES="${MC2_LOAD_ZOMBIES:-80}"
export MC2_LOAD_CLIENTS="${MC2_LOAD_CLIENTS:-3}"
export MC2_LOAD_REPETITIONS="${MC2_LOAD_REPETITIONS:-3}"
export MC2_LOAD_SECONDS="${MC2_LOAD_SECONDS:-$default_seconds}"
export MC2_LOAD_WARMUP_SECONDS="${MC2_LOAD_WARMUP_SECONDS:-60}"
export MC2_LOAD_CLIENT_WARMUP_SECONDS="${MC2_LOAD_CLIENT_WARMUP_SECONDS:-30}"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) sep=';' ;;
  *) sep=':' ;;
esac

if [[ ! -f target/test-classes/loadtest/NetworkLoadClient.class ]]; then
  echo "Compilando (mvn -q test-compile)..."
  mvn -B -ntp -q test-compile
fi

stamp="$(date -u +%Y%m%d-%H%M%S)"
export MC2_LOAD_OUT="${MC2_LOAD_OUT:-target/load-results/slo-${1:-corta}-${stamp}}"
mkdir -p "$MC2_LOAD_OUT"

{
  echo "scenario=slo-${1:-corta}"
  echo "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "commit=$(git rev-parse HEAD 2>/dev/null || echo "${GITHUB_SHA:-desconocido}")"
  echo "image_tag=${MC2_IMAGE_TAG:-sin-definir}"
  echo "target=${MC2_SERVER_HOST}:${MC2_SERVER_PORT} metrics=${MC2_SERVER_HOST}:${MC2_HEALTH_PORT}"
  echo "clients=${MC2_LOAD_CLIENTS} zombies=${MC2_LOAD_ZOMBIES} repetitions=${MC2_LOAD_REPETITIONS} seconds=${MC2_LOAD_SECONDS} population_wait=${MC2_LOAD_WARMUP_SECONDS} client_warmup=${MC2_LOAD_CLIENT_WARMUP_SECONDS}"
  echo "tick_p95_ms_max=${MC2_SLO_TICK_P95_MS:-16.67} rtt_p95_ms_max=${MC2_SLO_RTT_P95_MS:-100}"
  echo "java=$(java -version 2>&1 | head -1)"
  echo "runner=${RUNNER_NAME:-local} os=$(uname -sr)"
} > "$MC2_LOAD_OUT/manifest.txt"

set +e
java -Dsun.stdout.encoding=UTF-8 -cp "target/classes${sep}target/test-classes" loadtest.NetworkLoadClient \
  2>&1 | tee "$MC2_LOAD_OUT/console.txt"
status=${PIPESTATUS[0]}
set -e

echo "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')" >> "$MC2_LOAD_OUT/manifest.txt"
echo "exit_code=$status" >> "$MC2_LOAD_OUT/manifest.txt"
echo "Resultados en $MC2_LOAD_OUT"
exit "$status"
