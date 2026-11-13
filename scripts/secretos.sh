#!/usr/bin/env bash
# Secret-detection quality gate with Gitleaks over the WHOLE Git history.
# Threshold: zero findings. If there is one, the script fails.
#
# Usage (from the repository root, inside GitHub Actions or a container):
#   scripts/secretos.sh
# Leaves the report in reportes/gitleaks.json (secret values are masked by --redact; commit metadata is not).
#
# In CI the checkout must use `fetch-depth: 0`: a shallow clone would scan one commit and still look green,
# so the script refuses to run on a shallow clone or on a repository without commits.
#
# Gitleaks is NOT installed on the host: the pinned release is downloaded to a temporary folder,
# verified against the official checksums file and removed on exit. Any failed check aborts the run.
set -euo pipefail
cd "$(dirname "$0")/.."

GITLEAKS_VERSION="8.30.1"
GITLEAKS_ASSET="gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz"
GITLEAKS_SUMS="gitleaks_${GITLEAKS_VERSION}_checksums.txt"
GITLEAKS_URL="https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}"

die() { echo "Secrets gate: $*" >&2; exit 1; }

# The scanners run only in CI or in a container, never directly on a developer host.
if [[ "${GITHUB_ACTIONS:-}" != "true" && ! -e /.dockerenv && ! -e /run/.containerenv ]]; then
  die "run it inside GitHub Actions or a container (nothing is installed on the host)"
fi
[[ "$(uname -sm)" == "Linux x86_64" ]] || die "only Linux x86_64 is supported (got: $(uname -sm))"

# Silent-zero guard: make the size of the scan visible and refuse a partial history.
git rev-parse --git-dir > /dev/null 2>&1 || die "not a Git repository"
[[ "$(git rev-parse --is-shallow-repository)" == "false" ]] \
  || die "shallow clone: use 'fetch-depth: 0' in the checkout so the full history is scanned"
commits="$(git rev-list --all --count)"
(( commits > 0 )) || die "repository has no commits; refusing a scan that checks nothing"
echo "==> Commits reachable from all refs (to be scanned): $commits"

WORK="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/secretos-XXXXXX")"
cleanup() {
  rm -f -- "$WORK/$GITLEAKS_ASSET" "$WORK/$GITLEAKS_SUMS" "$WORK/gitleaks"
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

echo "==> Fetching Gitleaks v${GITLEAKS_VERSION} (checksum verified, temporary folder)"
fetch_verified "$GITLEAKS_ASSET" "$GITLEAKS_SUMS" "$GITLEAKS_URL"
tar -xzf "$WORK/$GITLEAKS_ASSET" -C "$WORK" gitleaks || die "gitleaks binary not found in $GITLEAKS_ASSET"
[[ -x "$WORK/gitleaks" ]] || die "extracted gitleaks is not executable"
"$WORK/gitleaks" version

mkdir -p reportes
# "git" scans every commit, not only the current files. --log-opts=--all covers all refs, not just HEAD.
# Gitleaks reads .gitleaksignore from the repository root if it exists.
echo "==> Quality gate: zero secrets in the full history"
rc=0
"$WORK/gitleaks" git . --log-opts=--all --redact --exit-code 1 \
  --report-format json --report-path reportes/gitleaks.json || rc=$?
if (( rc != 0 )); then
  echo "Secrets gate FAILED (gitleaks rc=$rc) after scanning up to $commits commits: finding or scanner error" >&2
  exit "$rc"
fi
echo "Secrets gate PASSED: no findings in $commits commits"
