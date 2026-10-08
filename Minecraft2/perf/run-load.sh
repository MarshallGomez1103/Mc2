#!/usr/bin/env bash
# Carga headless de la IA de enemigos (recuperación C2, REC-E4/E8).
# Uso, desde Minecraft2/:  perf/run-load.sh baseline|stress [minutos reales máx., por defecto 20]
# Protocolo y SLO: docs/recuperacion-c2/carga.md
set -euo pipefail

cd "$(dirname "$0")/.."

case "${1:-}" in
  baseline) scenario=REC_BASELINE ;;
  stress)   scenario=REC_STRESS ;;
  *) echo "Uso: perf/run-load.sh baseline|stress [minutos reales máx.]" >&2; exit 2 ;;
esac
minutes="${2:-20}"
heap_flags="-Xms1g -Xmx1g"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) sep=';' ;;
  *) sep=':' ;;
esac

hash_of() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  else shasum -a 256 "$1" | cut -d' ' -f1; fi
}

cpu_name() {
  case "$(uname -s)" in
    Linux) lscpu 2>/dev/null | sed -n 's/^Model name:[[:space:]]*//p' | head -1 | sed "s/[[:space:]]*$//" ;;
    Darwin) sysctl -n machdep.cpu.brand_string 2>/dev/null ;;
    *) powershell.exe -NoProfile -Command "(Get-CimInstance Win32_Processor | Select-Object -First 1).Name.Trim()" 2>/dev/null | tr -d '\r' ;;
  esac
}

ram_gb() {
  case "$(uname -s)" in
    Linux) awk '/MemTotal/ {printf "%.1f", $2/1048576}' /proc/meminfo ;;
    Darwin) sysctl -n hw.memsize 2>/dev/null | awk '{printf "%.1f", $1/1073741824}' ;;
    *) powershell.exe -NoProfile -Command "[math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory/1GB,1).ToString([cultureinfo]::InvariantCulture)" 2>/dev/null | tr -d '\r' ;;
  esac
}

os_name() {
  case "$(uname -s)" in
    Linux|Darwin) uname -sr ;;
    *) powershell.exe -NoProfile -Command "\$o=Get-CimInstance Win32_OperatingSystem; \$o.Caption+' '+\$o.Version" 2>/dev/null | tr -d '\r' ;;
  esac
}

echo "Compilando (mvn -q test-compile)..."
mvn -q test-compile

stamp="$(date +%Y%m%d-%H%M%S)"
out="target/load-results/$(echo "$scenario" | tr '[:upper:]' '[:lower:]')-${stamp}"
mkdir -p "$out"

dirty="$(git status --porcelain -- src test perf | wc -l | tr -d ' ')"
{
  echo "scenario=$scenario"
  echo "started_local=$(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "commit=$(git rev-parse HEAD)"
  echo "branch=$(git rev-parse --abbrev-ref HEAD)"
  echo "uncommitted_files_src_test_perf=$dirty"
  echo "java=$(java -version 2>&1 | head -1)"
  echo "java_runtime=$(java -version 2>&1 | sed -n 2p)"
  echo "jvm_flags=$heap_flags"
  echo "wall_limit_minutes=$minutes"
  echo "os=$(os_name)"
  echo "cpu=$(cpu_name)"
  echo "logical_cpus=$(getconf _NPROCESSORS_ONLN 2>/dev/null || nproc 2>/dev/null || echo ?)"
  echo "ram_gb=$(ram_gb)"
  echo "sha256_EnemyLoadHarness.java=$(hash_of test/loadtest/EnemyLoadHarness.java)"
  echo "sha256_run-load.sh=$(hash_of perf/run-load.sh)"
  echo "sha256_run-load.ps1=$(hash_of perf/run-load.ps1)"
} > "$out/manifest.txt"

# shellcheck disable=SC2086
java $heap_flags -Dsun.stdout.encoding=UTF-8 -Dmc2.load.out="$out" \
  -cp "target/classes${sep}target/test-classes" loadtest.EnemyLoadHarness "$scenario" "$minutes" \
  2>&1 | tee "$out/console.txt"

echo "finished_local=$(date '+%Y-%m-%d %H:%M:%S %z')" >> "$out/manifest.txt"
echo "Resultados en $out (copiar los seleccionados a perf/results/ tras revisarlos)"
