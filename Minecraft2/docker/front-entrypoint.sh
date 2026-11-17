#!/usr/bin/env bash
# Arranque de mc2-front: pantalla virtual, VNC local, puente noVNC y cliente del juego.
# El VNC escucha solo en el localhost del contenedor; lo único publicado es noVNC en 6080.
set -euo pipefail

export DISPLAY=:0
screen="${MC2_SCREEN:-1280x720x24}"

Xvfb "$DISPLAY" -screen 0 "$screen" -nolisten tcp +extension GLX &
for _ in $(seq 1 50); do
  [[ -S /tmp/.X11-unix/X0 ]] && break
  sleep 0.1
done

x11vnc -display "$DISPLAY" -rfbport 5900 -localhost -forever -shared -nopw -quiet -noxdamage &
websockify --web /usr/share/novnc 6080 127.0.0.1:5900 &

# Si el jugador cierra la ventana, se vuelve a abrir el cliente: el contenedor sigue disponible en el navegador.
# El PID del cliente queda en /tmp/mc2-client.pid: el healthcheck exige que el juego esté vivo, no solo noVNC.
while true; do
  java -XX:MaxRAMPercentage=75 \
    -Dmc2.gpu=AUTO \
    -Dmc2.worlds.dir="${MC2_WORLD_DIR:-/data/worlds}" \
    -jar /app/app.jar &
  echo $! > /tmp/mc2-client.pid
  status=0
  wait $! || status=$?
  rm -f /tmp/mc2-client.pid
  echo "El cliente terminó con código $status; se reinicia en 2 s" >&2
  sleep 2
done
