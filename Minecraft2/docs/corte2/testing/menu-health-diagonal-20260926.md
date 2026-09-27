# Menús, vida y navegación diagonal — 26 septiembre 2026

> Etapa anterior: el combate actual está en [combat-kamikaze-20260926.md](combat-kamikaze-20260926.md).

## Resultado

- Todos los menús muestran el mundo. Sin partida, un terreno aleatorio de 16 chunks
  existe solo en memoria, con cámara orbital lenta y velo ligero para leer las letras.
  Principal, opciones, gráficos, crear, listar y confirmaciones comparten ese fondo.
  La pausa sigue mostrando el mundo real congelado. Listas y campos son translúcidos.
- Los menús ya no muestran el modelo de GPU: Intel se presenta como Integrada y NVIDIA
  como Dedicada. Hardware desconocido queda como Automática sin adivinar su categoría.
  Los modelos y versión de OpenGL siguen en terminal. La selección requiere reinicio.
- Vida del jugador: 100, barra roja debajo de la estamina. Daño25 por golpe; primer
  contacto inmediato, después cada0.6s NORMAL /0.4s VERY_HARD. Cuatro golpes de un solo
  zombi matan aproximadamente a1.8s/1.2s. Varios zombis pueden golpear por separado.
  No se añade regeneración automática. Reaparecer recupera100; vacío sigue siendo letal.
- El intervalo pertenece a cada zombi y no se reinicia al salir y volver a su alcance.
  Se eliminó el temporizador anterior sin uso del modelo Zombie: una sola fuente de
  tiempo en EnemyUpdateService. Pausa y muerte congelan la simulación.
- Persecución: después de detectar, conserva el objetivo vivo. Una pared dentro del
  alcance de golpe ya no lo deja inmóvil: navega alrededor. Pared alta sigue bloqueando daño.
- A*: ocho direcciones. En terreno libre, 1→5 es un paso diagonal directo. Cada diagonal
  cuesta sqrt(2), cada recta1, más0.5 por desnivel. Heurística octil admisible. El cuerpo
  requiere espacio y apoyo en las dos columnas que flanquean la esquina, sin atravesar
  paredes, techos bajos, huecos o desniveles excesivos. La diagonal no acelera al zombi.
- Se retiró la frase larga. Permanecen exclusivamente Pereciste, Paila, Te agarraron.
- No se añadieron sonidos ni dependencias. Mundos JSON conservan formato; vida y
  zombis son estado de sesión. Tamaños, rondas y velocidad de carrera se conservan.

## Reparto y arquitectura

Tres agentes con archivos separados desarrollaron fondo, vida/ataques y navegación.
El principal integró GPU, frases y persecución, revisó y ejecutó las pruebas.
MenuWorldBackdrop solo renderiza paisaje; GraphicalGame coordina menús. PlayerLife
es dominio puro; GameHud dibuja vida. NavigationGrid valida pasos y AStarPathfinder
busca rutas. EnemyUpdateService coordina golpes, espera y búsqueda.

## Verificación

- Maven package:262 pruebas,0 fallos,0 errores,0 ignoradas. Incluye11 nuevas regresiones
  diagonales: 1→5, ruta óptima, ambos lados bloqueados, hueco lateral, techo, subida
  lateral oculta, escalón alto, velocidad, subida1, caída3 y pared construida tras planear.
- Regresiones de vida: daño y reaparición, primer golpe inmediato, espera independiente,
  daño de múltiples enemigos, pared cerrada protectora y pilar rodeable.
- Prueba de persecución: mover al jugador más allá del antiguo rango de pérdida30 no
  abandona la ruta; mantiene búsquedas espaciadas y presupuesto de2000 expansiones.
- Smoke automático OpenGL de224frames: todos los menús principales sobre paisaje,
  creación, opciones, GPU, sprint, pausa, fullscreen, redimensionar, barra vida75/100,
  tres frases, reaparición100/100, guardar, servicio nuevo/cargar, confirmar/cancelar,
  eliminación exclusiva del mundo temporal. El paisaje no aparece como mundo activo.
- GPU dedicada real confirmada NVIDIA RTX4060; portada solo dice Dedicada.
  Smoke normal usa Intel y muestra Integrada. No hardware AMD físico.
- Capturas revisadas de portada, lista, GPU, vida y muerte. No es playtest humano.
- Los mundos existentes mantienen SHA-256 y no se usan para las pruebas. El escenario
  del menú no se guarda. Sin commit, push, reset ni clean de Git.
- PIT/carga anteriores son históricos, no se ejecutaron de nuevo para este ajuste.

## Abrir y devolver

El JAR del repositorio está actualizado. Cerrar una instancia anterior y volver a abrir.
El entregable incluye Jugar_Minecraft2.sh y volver_al_estado_previo.py: revisión sin
argumentos; devolución de esta etapa con --apply --rebuild. Se verifica todo el
manifiesto antes de escribir y se detiene si hay cambios posteriores. No toca mundos.
