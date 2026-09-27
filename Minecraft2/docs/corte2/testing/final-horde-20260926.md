# Entrega final: oleadas, reptante y restos

## Cambios

- Reptante: 28% de velocidad (NORMAL 1.204 bloques/s; VERY_HARD 1.316), torso horizontal detrás de cabeza adelantada, brazos sobre el suelo y textura UV original por parte.
- Ambas dificultades: 4 × 2^(oleada−1): 4, 8, 16, 32, 64, 128… Se conservan tiempos de ronda y velocidad de zombis de pie. Saturación en Integer.MAX_VALUE evita desbordamiento; no existe un tope de 6/12/1024. Rondas enormes no equivalen a una garantía de rendimiento.
- Apariciones ocupadas o sin suelo se reintentan sin descontar cantidad pendiente. Máximo 64 apariciones por actualización para intervalos de aparición cero.
- Al llegar al borde: 5 segundos de preparación por zombi; el mismo hueco acepta como máximo una entrada por segundo, además de verificar ocupación. El reloj se congela en pausa/Enemigos OFF.
- Las caídas profundas mantienen 30% de supervivencia reptante y 70% de desarmarse.
- Un cuerpo desarmado sale de la IA y deja 18 fragmentos texturizados. Sus trayectorias cortas chocan con suelo/paredes; permanecen inmóviles tras asentarse. No tienen daño ni hitbox, no bloquean rondas y no desaparecen por temporizador. Se conservan durante la sesión; no se escriben en JSON y se liberan al cerrar la partida.
- Mundos y graphics.properties siguen locales. No se impone la GPU del autor a otros equipos.
- jugar.sh reconstruye el JAR actualizado y arranca el juego; IntelliJ puede ejecutar bootstrap.Minecraft2Application.

## Verificación

- Maven package: 313 pruebas, 0 fallos/errores/ignoradas.
- Pruebas nuevas: espera de preparación, congelamiento OFF, cola ≥1 segundo, restos permanentes, suelo/estabilidad, velocidad reptante, pausa de fragmentos, transición de oleada con restos, cantidades duplicadas, saturación, reintento de spawn y presupuesto de aparición.
- Smoke automático OpenGL: 260 fotogramas, menús, guardado temporal, pantalla completa, pausa, pistola, daño, reptante, fragmentos asentados y mensajes especiales. Se usaron mundos desechables, no los guardados por el usuario. Las escenas visuales se prepararon automáticamente; no sustituye una partida humana.
- Capturas revisadas de reptante y restos asentados. Rendimiento de cientos/miles de enemigos no se midió en una partida larga; PIT y carga anteriores siguen como evidencia histórica.
- Publicación solo en main, avance normal sin force. Ramas de estudiantes intactas.

## Actualizar

Desde Mc2: git pull origin main, luego ./Minecraft2/jugar.sh (JDK17+ y Maven).
En IntelliJ: actualizar main, recargar Maven y ejecutar bootstrap.Minecraft2Application.
