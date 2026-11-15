#!/usr/bin/env bash
# Quality gate de análisis estático (SAST): revisa el reporte SARIF de CodeQL y falla si hay hallazgos
# con severidad de seguridad alta o crítica (security-severity >= 7.0) que no estén tratados.
# Falla también si un SARIF falta, no se puede leer o no trae ninguna ejecución (run).
#
# Uso (desde la raíz del repositorio):
#   scripts/umbral-sast.sh <archivo.sarif> [archivo.sarif ...]
# Requiere jq (viene instalado en los runners ubuntu-latest de GitHub Actions).
#
# Un hallazgo se considera tratado si su regla y su archivo aparecen en .github/codeql/excepciones-sast.txt
# (una excepción por línea: "<regla> <archivo>   # nota"; el archivo con la ruta desde la raíz, p. ej.
# Minecraft2/src/...). Cada excepción debe tener su registro en Minecraft2/docs/seguridad.md: si ese
# documento no menciona la regla y el archivo, el script falla.
set -euo pipefail
cd "$(dirname "$0")/.."

UMBRAL="7.0"
EXCEPCIONES="${EXCEPCIONES:-.github/codeql/excepciones-sast.txt}"
DOC_SEGURIDAD="${DOC_SEGURIDAD:-Minecraft2/docs/seguridad.md}"
[[ $# -gt 0 ]] || { echo "Uso: scripts/umbral-sast.sh <archivo.sarif> [...]" >&2; exit 2; }
for sarif in "$@"; do
  [[ -f "$sarif" ]] || { echo "SAST gate failed: SARIF not found: $sarif" >&2; exit 1; }
done

# Excepciones en formato JSON: [{"regla": "...", "archivo": "..."}], cada una con registro en el documento.
excepciones='[]'
if [[ -f "$EXCEPCIONES" ]]; then
  lineas="$({ grep -vE '^[[:space:]]*(#|$)' "$EXCEPCIONES" || true; } | awk '{print $1" "$2}')"
  while read -r regla archivo; do
    [[ -n "$regla" ]] || continue
    { [[ -n "$archivo" && -f "$DOC_SEGURIDAD" ]] && grep -qF -- "$regla" "$DOC_SEGURIDAD" && grep -qF -- "$archivo" "$DOC_SEGURIDAD"; } \
      || { echo "SAST gate failed: excepción '$regla' '$archivo' sin registro en $DOC_SEGURIDAD" >&2; exit 1; }
  done <<< "$lineas"
  excepciones="$(jq -R -s 'split("\n") | map(select(length > 0) | split(" ") | {regla: .[0], archivo: .[1]})' <<< "$lineas")"
fi

hallazgos="$(jq -s --argjson umbral "$UMBRAL" --argjson excepciones "$excepciones" '
  [ .[] | if (.runs | type) == "array" and (.runs | length) > 0 then .runs[] else error("SARIF without runs") end
    | . as $run
    # Severidad de cada regla, según los metadatos de la consulta de CodeQL
    | ([$run.tool.driver.rules[]?, $run.tool.extensions[]?.rules[]?]
        | map({key: .id, value: ((.properties["security-severity"] // "0") | tonumber)})
        | from_entries) as $severidad
    | $run.results[]?
    | {
        regla: .ruleId,
        severidad: ($severidad[.ruleId] // 0),
        archivo: .locations[0].physicalLocation.artifactLocation.uri,
        linea: .locations[0].physicalLocation.region.startLine,
        mensaje: .message.text
      }
    | select(.severidad >= $umbral)
    | . as $h
    | .tratado = any($excepciones[]; .regla == $h.regla and .archivo == $h.archivo)
  ]' "$@")" || { echo "SAST gate failed: could not parse SARIF" >&2; exit 1; }

echo "==> Hallazgos de SAST con severidad >= $UMBRAL"
jq -r '.[] | "  [\(if .tratado then "tratado" else "PENDIENTE" end)] \(.severidad) \(.regla) \(.archivo):\(.linea) \(.mensaje)"' <<< "$hallazgos"

pendientes="$(jq '[.[] | select(.tratado | not)] | length' <<< "$hallazgos")"
total="$(jq 'length' <<< "$hallazgos")"
echo "==> $total hallazgo(s) altos o críticos, $pendientes sin tratar"

if [[ "$pendientes" -gt 0 ]]; then
  echo "QUALITY GATE DE SAST FALLIDO: corrija los hallazgos o regístrelos en Minecraft2/docs/seguridad.md" >&2
  exit 1
fi
