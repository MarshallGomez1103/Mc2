#!/usr/bin/env bash
# Reversión de Minecraft2 a una versión anterior ya publicada en GHCR, con verificación posterior.
#
# Uso:  scripts/rollback.sh [etiqueta]
#   Sin argumento vuelve a la «última buena» registrada por scripts/deploy.sh en MC2_DEPLOY_STATE
#   (~/.mc2-deploy). Con argumento (SHA o vX.Y.Z) vuelve a esa etiqueta.
# Reutiliza scripts/deploy.sh, de modo que la versión restaurada pasa la misma readiness y la misma prueba
# de humo. Sale con código distinto de cero si la reversión no queda verificada.
set -euo pipefail
cd "$(dirname "$0")/.."

state="${MC2_DEPLOY_STATE:-$HOME/.mc2-deploy}"
destino="${1:-}"
if [[ -z "$destino" && -f "$state/ultima-buena" ]]; then
  destino="$(cat "$state/ultima-buena")"
fi
if [[ -z "$destino" ]]; then
  echo "No hay versión a la que volver: pase una etiqueta (SHA o vX.Y.Z) o despliegue una primero." >&2
  exit 2
fi

origen="${MC2_IMAGE_TAG:-desconocida}"
echo "==> Reversión: ${origen} → ${destino}"
scripts/deploy.sh "$destino"

mkdir -p "$state"
printf '%s\treversión\t%s\t→\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$origen" "$destino" >> "$state/historial.tsv"
echo "==> Reversión verificada: ${destino} en servicio"
