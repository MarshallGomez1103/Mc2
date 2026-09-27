#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$(readlink -f -- "$0")")"
# Reconstruye desde el código recién actualizado; conserva mundos y preferencia local de GPU.
mvn -q -DskipTests package
exec java -Xmx2g --enable-native-access=ALL-UNNAMED -jar target/minecraft2-0.1.0-SNAPSHOT.jar "$@"
