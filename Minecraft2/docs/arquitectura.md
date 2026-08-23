# Arquitectura por capas

## Propósito

La separación por capas evita que el menú conozca cómo se guarda un JSON y que el dominio dependa de una interfaz gráfica. También permite que tres estudiantes trabajen en áreas distintas con contratos claros.

## Capas y responsabilidades

### 1. Presentation

`MainMenu` muestra opciones, lee texto y presenta mensajes. Solo llama a servicios de aplicación. No genera terreno, mueve al jugador ni accede directamente a archivos.

### 2. Domain / Game Logic

Esta capa se organiza internamente en dos paquetes para no mezclar coordinación y modelo.

#### Application

`WorldApplicationService` representa los casos de uso: crear, listar, cargar, guardar y eliminar mundos. `ChunkGenerationService` coordina la generación de un chunk con `SimpleTerrainGenerator` y `BlockFactory`. Ninguno decide cómo dibujar o cómo codificar JSON.

#### Domain

Contiene el vocabulario principal:

- `World`: conjunto de chunks y origen de notificaciones de bloques. Conserva también la identidad
  del mundo (`id`, `name`), la semilla de generación, la fecha de creación y el jugador, es decir,
  todos los datos que exige el esquema JSON acordado.
- `Chunk`: contenedor mínimo de bloques.
- `Block` y `BlockType`: estado básico de cada voxel.
- `Player`: posición mínima del jugador.
- `Position`: coordenadas enteras.
- `BlockChange`: dato que describe una colocación o eliminación.

La generación inicial de terreno y chunks está implementada. Movimiento, gravedad, colisiones y reglas de interacción todavía no lo están.

### 3. Persistence

`WorldStorage` declara las operaciones CREATE, READ, UPDATE, DELETE y listado. No es un DAO ni se conecta a una base de datos.

`JsonWorldStorage` trabaja con la carpeta `worlds/`. Actualmente persiste un JSON mínimo y deja pendiente el esquema completo de chunks y bloques.

## Apoyo arquitectónico

`bootstrap.Minecraft2Application` crea las dependencias e inicia el menú. `bootstrap` no es una capa ni contiene reglas; solo evita que las capas se dependan mutuamente durante el cableado inicial.

`patterns` contiene exclusivamente Factory, Singleton y Observer.

## Dirección de dependencias

```text
Minecraft2Application (bootstrap)
   │
   ▼
MainMenu
   │
   ▼
WorldApplicationService ──────> WorldStorage
   │                              │
   ├──> WorldManager              └──> World
   └──> World

World ──> Observer/Subject
BlockFactory ──> Block, BlockType, Position
```

No hay referencias desde `domain` hacia `presentation`, `application` o `persistence`. `Position` vive en la raíz de `domain` para que `domain.block` y `domain.world` no se dependan mutuamente. Las interfaces genéricas de Observer tampoco conocen el dominio, lo que evita otra dependencia circular.

## Reglas para continuar

- Mantener la entrada y salida de usuario en `presentation`.
- Colocar coordinación de casos de uso en `application`.
- Colocar reglas del videojuego en `domain`.
- Mantener lectura y escritura de archivos en `persistence`.
- No añadir patrones distintos de Factory, Singleton y Observer.
- No convertir servicios, fábricas o almacenamiento en Singleton.
