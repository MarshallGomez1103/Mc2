#!/usr/bin/env bash
# DAST con OWASP ZAP baseline contra los objetivos HTTP del entorno desplegado: /health (servidor) y noVNC (front).
# El protocolo de juego (TCP 7777) no es HTTP y queda fuera del alcance de ZAP.
# Umbral: falla si ZAP reporta alguna alerta de riesgo alto (riskcode 3) o si el escaneo no termina.
# Las alertas medias, bajas e informativas se publican en el reporte y se tratan en seguridad.md.
# Uso: scripts/zap-baseline.sh <directorio de reportes>
set -euo pipefail

SALIDA="${1:?Uso: scripts/zap-baseline.sh <directorio de reportes>}"
# ZAP 2.17.0 fijado por resumen (digest).
ZAP_IMAGEN="ghcr.io/zaproxy/zaproxy:2.17.0@sha256:9624669db72f54d86c4b46a7064518bd9bf79d8c61a4c31befa9923489f328af"
HEALTH_PORT="${MC2_HEALTH_PORT:-8080}"

mkdir -p "$SALIDA"
chmod 0777 "$SALIDA"   # el contenedor de ZAP escribe como el usuario zap

objetivos=(
  "health|http://127.0.0.1:${HEALTH_PORT}/health"
  "novnc|http://127.0.0.1:6080/vnc.html"
)

altos=0
for entrada in "${objetivos[@]}"; do
  nombre="${entrada%%|*}"
  url="${entrada#*|}"
  echo "==> ZAP baseline: $url"
  rc=0
  docker run --rm --network host -v "$SALIDA:/zap/wrk:rw" "$ZAP_IMAGEN" \
    zap-baseline.py -t "$url" -m 1 -I -J "zap-${nombre}.json" -r "zap-${nombre}.html" || rc=$?
  # Con -I las advertencias no cambian el código; 1 = FAIL de una regla y 3 = error del escaneo.
  if [[ "$rc" -ne 0 ]]; then
    echo "ZAP terminó con código $rc en $url" >&2
    exit 1
  fi
  [[ -s "$SALIDA/zap-${nombre}.json" ]] || { echo "Falta el reporte de ZAP para $url" >&2; exit 1; }
  n=$(python3 - "$SALIDA/zap-${nombre}.json" <<'PY'
import json, sys
datos = json.load(open(sys.argv[1], encoding="utf-8"))
alertas = [a for s in datos.get("site", []) for a in s.get("alerts", [])]
for a in alertas:
    print(f"  riesgo {a.get('riskcode')} · {a.get('alert')} ({a.get('count', '?')} instancias)", file=sys.stderr)
print(sum(1 for a in alertas if str(a.get("riskcode")) == "3"))
PY
)
  echo "    alertas de riesgo alto: $n"
  altos=$((altos + n))
done

if [[ "$altos" -gt 0 ]]; then
  echo "ZAP gate FAILED: $altos alerta(s) de riesgo alto" >&2
  exit 1
fi
echo "==> ZAP gate superado: sin alertas de riesgo alto"
