#!/usr/bin/env bash
# Dependency (SCA) quality gate with Trivy.
# Threshold: no HIGH or CRITICAL vulnerability that has a fix available. If there is one, the script fails.
#
# Usage (from the repository root, inside GitHub Actions or a container):
#   scripts/sca.sh
# Leaves the full report (all severities) in reportes/trivy-dependencias.json.
#
# Trivy is NOT installed on the host: the pinned release is downloaded to a temporary folder,
# verified against the official checksums file and removed on exit. Any failed check aborts the run.
set -euo pipefail
cd "$(dirname "$0")/.."

TRIVY_VERSION="0.76.2"
TRIVY_ASSET="trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz"
TRIVY_SUMS="trivy_${TRIVY_VERSION}_checksums.txt"
TRIVY_URL="https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}"
PROJECT_DIR="Minecraft2"   # the repository root has no pom.xml

die() { echo "SCA gate: $*" >&2; exit 1; }

# The scanners run only in CI or in a container, never directly on a developer host.
if [[ "${GITHUB_ACTIONS:-}" != "true" && ! -e /.dockerenv && ! -e /run/.containerenv ]]; then
  die "run it inside GitHub Actions or a container (nothing is installed on the host)"
fi
[[ "$(uname -sm)" == "Linux x86_64" ]] || die "only Linux x86_64 is supported (got: $(uname -sm))"
[[ -f "$PROJECT_DIR/pom.xml" ]] || die "$PROJECT_DIR/pom.xml not found; run it from the repository root"
command -v mvn > /dev/null || die "mvn is required on PATH"

WORK="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/sca-XXXXXX")"
cleanup() {
  rm -f -- "$WORK/$TRIVY_ASSET" "$WORK/$TRIVY_SUMS" "$WORK/trivy"
  rmdir -- "$WORK" 2> /dev/null || true
}
trap cleanup EXIT

# Download one release asset plus its checksums file, require exactly one matching checksum line and verify it.
# --strict makes malformed lines an error; --ignore-missing is avoided on purpose (it can pass with nothing verified).
fetch_verified() {
  local asset="$1" sums="$2" url="$3" line
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 --retry 3 -o "$WORK/$asset" "$url/$asset" \
    || die "download failed: $asset"
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 --retry 3 -o "$WORK/$sums" "$url/$sums" \
    || die "download failed: $sums"
  line="$(awk -v f="$asset" '$2 == f' "$WORK/$sums")"
  [[ -n "$line" && "$(wc -l <<< "$line")" -eq 1 ]] || die "$sums does not list exactly one checksum for $asset"
  (cd "$WORK" && printf '%s\n' "$line" | sha256sum -c --strict -) || die "checksum verification FAILED for $asset"
}

echo "==> Fetching Trivy v${TRIVY_VERSION} (checksum verified, temporary folder)"
fetch_verified "$TRIVY_ASSET" "$TRIVY_SUMS" "$TRIVY_URL"
tar -xzf "$WORK/$TRIVY_ASSET" -C "$WORK" trivy || die "trivy binary not found in $TRIVY_ASSET"
[[ -x "$WORK/trivy" ]] || die "extracted trivy is not executable"
"$WORK/trivy" --version

# Trivy reads pom.xml and resolves transitive dependencies. Fill the local Maven cache first so it does not
# have to download every pom from Maven Central (which rate-limits repeated requests).
echo "==> Downloading dependencies to the local Maven cache"
mvn -B -q -ntp -f "$PROJECT_DIR/pom.xml" dependency:go-offline

mkdir -p reportes
# The ignore file is passed only if it exists, so a missing file never changes the behavior.
ignore=()
[[ -f .trivyignore ]] && ignore=(--ignorefile "$PWD/.trivyignore")

scan() {
  "$WORK/trivy" fs --scanners vuln --no-progress --skip-dirs target "${ignore[@]}" "$@" "$PROJECT_DIR"
}

echo "==> Full report: reportes/trivy-dependencias.json"
scan --quiet --format json --output reportes/trivy-dependencias.json

# Silent-pass guard: the report must contain at least one parsed Maven (pom) result, otherwise nothing was analysed.
jq -e '[.Results[]? | select(.Type == "pom")] | length > 0' reportes/trivy-dependencias.json > /dev/null \
  || die "Trivy parsed no Maven dependencies from $PROJECT_DIR/pom.xml; refusing a scan that checks nothing"

echo "==> Quality gate: HIGH or CRITICAL with a fix available"
rc=0
scan --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 || rc=$?
if (( rc != 0 )); then
  echo "SCA gate FAILED (trivy rc=$rc): HIGH/CRITICAL vulnerability with a fix, or scanner error" >&2
  exit "$rc"
fi
echo "SCA gate PASSED: no HIGH/CRITICAL vulnerability with a fix available"
