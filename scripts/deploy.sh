#!/usr/bin/env bash
# Despliegue de Minecraft2 con Docker Compose desde GHCR, readiness y prueba de humo.
#
# Uso (desde cualquier carpeta):  scripts/deploy.sh <etiqueta>
#   <etiqueta>: SHA completo del commit o versión vX.Y.Z publicados en el registro. Nunca «latest».
#        scripts/deploy.sh --humo   solo la prueba de humo contra los servicios ya levantados.
#
# Variables: MC2_REGISTRY (por defecto ghcr.io/marshallgomez1103), MC2_LOAD_ZOMBIES (0 en juego, 80 para carga),
# MC2_READY_TIMEOUT (segundos de espera de los healthchecks, 180) y MC2_DEPLOY_STATE (carpeta de estado,
# ~/.mc2-deploy). Solo una versión que pasa el humo se registra como «última buena»; scripts/rollback.sh vuelve a ella.
# Sale con código distinto de cero si la descarga, la readiness o el humo fallan.
set -euo pipefail
cd "$(dirname "$0")/.."

tag="${1:-}"
solo_humo=0
[[ "$tag" == "--humo" ]] && solo_humo=1
if [[ -z "$tag" ]]; then
  echo "Uso: scripts/deploy.sh <sha|vX.Y.Z>" >&2
  exit 2
fi
if (( ! solo_humo )) && [[ "$tag" == "latest" || ! "$tag" =~ ^(v[0-9]+\.[0-9]+\.[0-9]+|[0-9a-f]{40})$ ]]; then
  echo "Etiqueta no desplegable: '$tag'. Se exige un SHA de 40 caracteres o vX.Y.Z (nunca latest)." >&2
  exit 2
fi

export MC2_IMAGE_TAG="$tag"
export MC2_REGISTRY="${MC2_REGISTRY:-ghcr.io/marshallgomez1103}"
export MC2_LOAD_ZOMBIES="${MC2_LOAD_ZOMBIES:-0}"
health_port="${MC2_HEALTH_PORT:-8080}"
game_port="${MC2_SERVER_PORT:-7777}"
state="${MC2_DEPLOY_STATE:-$HOME/.mc2-deploy}"
mkdir -p "$state"
[[ -f .env ]] || cp .env.example .env

# Las variables exportadas tienen prioridad sobre .env en Compose.
compose() { docker compose --env-file .env "$@"; }

diagnostico() {
  echo "::group::Estado y registros de los servicios" || true
  compose ps || true
  compose logs --no-color --tail 80 || true
  echo "::endgroup::" || true
}

if (( ! solo_humo )); then
  echo "==> Desplegando ${MC2_REGISTRY}/mc2-{server,front}:${tag} (MC2_LOAD_ZOMBIES=${MC2_LOAD_ZOMBIES})"
  compose pull --quiet
  if ! compose up -d --no-build --wait --wait-timeout "${MC2_READY_TIMEOUT:-180}"; then
    echo "Readiness: los servicios no quedaron saludables a tiempo" >&2
    diagnostico
    exit 1
  fi
fi

echo "==> Prueba de humo"
falla() { echo "HUMO FALLIDO: $*" >&2; diagnostico; exit 1; }

curl -fsS --max-time 5 "http://127.0.0.1:${health_port}/health" >/dev/null || falla "/health no responde 200"

metricas="$(curl -fsS --max-time 5 "http://127.0.0.1:${health_port}/metrics")" || falla "/metrics no responde 200"
for metrica in mc2_tick_duration_seconds mc2_tick_failures_total mc2_zombies_active \
               mc2_players_connected mc2_population_deficit_ticks_total; do
  grep -q "^${metrica}" <<< "$metricas" || falla "/metrics no expone ${metrica}"
done

# Protocolo v1: hello → welcome en menos de 5 s.
bienvenida="$(timeout 5 bash -c '
  exec 3<>"/dev/tcp/127.0.0.1/$1"
  printf "%s\n" "{\"type\":\"hello\",\"name\":\"humo\",\"skinId\":0}" >&3
  head -1 <&3
  printf "%s\n" "{\"type\":\"bye\"}" >&3
' _ "$game_port")" || falla "el puerto ${game_port} no respondió al hello"
grep -q '"welcome"' <<< "$bienvenida" || falla "respuesta inesperada al hello: ${bienvenida}"

curl -fsS --max-time 5 "http://127.0.0.1:6080/vnc.html" >/dev/null || falla "noVNC (6080) no responde"

if (( solo_humo )); then echo "==> Humo correcto"; exit 0; fi
echo "==> Humo correcto: ${tag} desplegado"
if [[ -f "$state/ultima-buena" && "$(cat "$state/ultima-buena")" != "$tag" ]]; then
  cp "$state/ultima-buena" "$state/anterior"
fi
echo "$tag" > "$state/ultima-buena"
printf '%s\tdespliegue\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$tag" >> "$state/historial.tsv"
