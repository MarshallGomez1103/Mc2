#!/usr/bin/env bash
# Quality gate de escaneo de la imagen Docker del servidor con Trivy (sistema operativo y librerías de la imagen).
# Umbral: ninguna vulnerabilidad CRITICAL que tenga corrección disponible. Si hay, el script falla.
#
# Uso (desde la raíz del repositorio):
#   scripts/escaneo-imagen.sh
# Construye Minecraft2/Dockerfile (contexto Minecraft2/), lo etiqueta mc2-server:ci, lo guarda en un tar
# y escanea ese tar. No se monta el socket de Docker en ningún contenedor.
# Deja el reporte completo (todas las severidades) en reportes/trivy-imagen.json.
# Requiere docker (o podman con ENGINE=podman), curl, sha256sum y tar; solo Linux x86_64.
set -euo pipefail
cd "$(dirname "$0")/.."

ENGINE="${ENGINE:-docker}"
IMAGEN="${IMAGEN:-mc2-server:ci}"
TRIVY_VERSION="0.76.2"
# SHA-256 de trivy_0.76.2_Linux-64bit.tar.gz, tomado de trivy_0.76.2_checksums.txt de la release v0.76.2.
TRIVY_SHA256="bc0fc726e017b8927a62b2d3531d7a84ee01769daf5c99ed866dced54089fdbf"
TRIVY_TARBALL="trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz"
TRIVY_URL="https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/${TRIVY_TARBALL}"

[[ "$(uname -s)-$(uname -m)" == "Linux-x86_64" ]] || { echo "Este script solo soporta Linux x86_64" >&2; exit 2; }

mkdir -p reportes
TRABAJO="$(mktemp -d)"
trap 'rm -f "$TRABAJO/$TRIVY_TARBALL" "$TRABAJO/trivy" "$TRABAJO/imagen.tar"; rmdir "$TRABAJO"' EXIT

echo "==> Descargando Trivy ${TRIVY_VERSION} y verificando su SHA-256"
curl -fsSL --retry 3 -o "$TRABAJO/$TRIVY_TARBALL" "$TRIVY_URL"
echo "${TRIVY_SHA256}  ${TRABAJO}/${TRIVY_TARBALL}" | sha256sum -c -
tar -xzf "$TRABAJO/$TRIVY_TARBALL" -C "$TRABAJO" trivy

echo "==> Construyendo $IMAGEN"
"$ENGINE" build -f Minecraft2/Dockerfile -t "$IMAGEN" Minecraft2
"$ENGINE" save "$IMAGEN" -o "$TRABAJO/imagen.tar"

# .trivyignore es opcional: solo se pasa si existe.
ignorar=()
[[ -f .trivyignore ]] && ignorar=(--ignorefile .trivyignore)

trivy() {
  "$TRABAJO/trivy" image --scanners vuln --input "$TRABAJO/imagen.tar" "${ignorar[@]}" "$@"
}

echo "==> Reporte completo: reportes/trivy-imagen.json"
trivy --quiet --format json --output reportes/trivy-imagen.json

echo "==> Quality gate: CRITICAL con corrección disponible"
trivy --severity CRITICAL --ignore-unfixed --exit-code 1
