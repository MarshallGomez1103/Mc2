> Registro histórico del Corte 2. Los conteos, parámetros y métricas corresponden a la etapa descrita; no son resultados del Corte 3.

# Combate, pistola y zombis kamikaze — 26 septiembre 2026

## Implementado

- Subir montañas deja de ser auto-step: gravedad-20, salto8 (constantes del jugador),
  apoyo y aterrizaje reales, sin doble salto; mínimo1s entre saltos. El ascenso de tres
  escalones tarda más de2s en la regresión, sin incrementos instantáneos de1bloque.
- Daño al jugador: velo rojo breve de0.3s, alfa máximo0.28 y desvanecimiento. Solo en
  partida activa; pausa/muerte no dejan una pantalla roja congelada. Golpe al zombi:
  color rojo sólido0.18s, luego restaura su textura. Incluso con IA apagada se desvanece.
- Regeneración elegida:5puntos/s tras5s sin golpes; un golpe reinicia la espera. Pausa
  y muerte congelan el reloj. Reaparecer sigue recuperando100.
- Pistola: aparece una sola vez cuando empieza la oleada3 o posterior, nunca antes.
  Ubicación aleatoria sobre terreno real (sin guardar un mundo nuevo), preferentemente
  alcanzable por A* y a3–32bloques del jugador. Si está encerrado en un pozo, puede
  aparecer en suelo seguro de arriba; construir una salida permite buscarla. Si no hay
  columna válida, reintenta cada5s en vez de generar un objeto dentro de bloques.
- Permanece en el suelo hasta acercarse, se recoge/equipa automáticamente. Q alterna;
  clic izquierdo dispara,1–7 seleccionan bloques. Daño3, alcance36, intervalo0.28s,
  munición ilimitada. Fallar un tiro no elimina bloques. Respeta paredes, esquinas,
  chunks ausentes y altura del reptante. Hay modelo en suelo/mano y destello del disparo.
- Huecos: A* llega al borde abierto y continúa con gravedad. En la entrada esperan
  mientras el anterior despeja el borde, con separación mínima0.65s. Recuerdan la
  entrada elegida aunque el jugador se aleje por la cueva, y siguen persiguiendo abajo.
- Caída>=8bloques:30% sigue vivo sin piernas y70% se desarma en seis piezas separadas
  que giran. Una muestra de RNG por aterrizaje profundo, inyectable en tests y seeded
  por mundo en juego. Caídas menores aterrizan sin esa transformación. Vacío sin piso
  mata al zombi. No hay explosión de bloques ni daño de área.
- Reptante: altura0.8,velocidad55%,sin salto y rutas con1bloque de espacio. Cabeza y
  torso visibles corresponden a la caja del cuerpo. Un choque contra la cabeza de
  otro zombi desarma al que cae; nunca sirve de plataforma ni crea una pila caminable.
- Por muerte real kamikaze: «Te mató el zombi kamikaze». Por VOID: «Aquí no hay piso».
  Las tres frases normales permanecen para ENEMY. Ninguna causa consume la frase de otra.
- Menús, sonidos, dificultad/rondas y formato JSON conservados. Vida, pistola, oleadas
  y zombis son de sesión: cerrar/cargar inicia otra sesión; morir/reaparecer conserva arma.

## Arquitectura

ZombiePhysics/KamikazePolicy son dominio puro;
PistolService/ZombieTargeting son aplicación; renderers/HUD solo dibujan. GameSession
avanza salud, enemigos, horda y pistola una vez por frame activo. El daño de los puños
ya utiliza el mismo targeting que la pistola, sin duplicar rayos ni oclusión.

## Verificación de esa etapa

- `mvn package`:303pruebas,0fallos,0errores,0ignoradas; JAR completo generado.
- Física: velocidad y gravedad del jugador, salto/techo/aterrizaje, pendiente de tres
  escalones, caída profunda sin atravesar piso, borde0.30 exacto y una sola muestra,
  túnel bajo, fila en pozo, cuerpo en fondo no bloquea entrada, impacto sobre cabeza,
  memoria del borde con jugador movido10bloques por cueva y relojes con IA desactivada.
- Combate: primera lesión y cooldown, salud fraccional/FPS/espera5s, pausa, causas exactas,
  muerte causada realmente por un reptante y regeneración integrada en GameSession.
- Arma: gate de oleada3 incluido en ciclo real de HordeManager, aparición única,
  recogida/equipado, suelo válido, posiciones reproducibles/diversas, jugador en pozo,
  no edición de terreno, alcance, cooldown, rayos a reptantes, paredes y esquinas.
- Smoke automático OpenGL de260frames: menús existentes, guardado/carga, fullscreen,
  resize, pistola suelo/mano, recogida, clic disparo, Q,volver a bloques, tiro fallido
  sin minería, velo rojo, zombi rojo,reptante,seis piezas y ambas muertes especiales.
  Los escenarios usan mundos temporales y algunas posiciones/causas forzadas para
  capturar los efectos; no es un playtest humano ni una medición de diversión.
- Capturas revisadas. PIT/carga anteriores siguen siendo históricos; no se ejecutaron
  de nuevo. No se tocó la selección de GPU ni se requiere instalar nada.
