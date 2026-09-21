package application;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.SimpleTerrainGenerator;
import domain.world.World;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.WorldStorage;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Casos de uso relacionados con la administración de mundos.
 * No contiene entrada/salida de la interfaz de usuario.
 */
public final class WorldApplicationService {
    private final WorldStorage storage;
    private final BlockFactory blockFactory;
    private final WorldManager worldManager;
    private final Random seedGenerator = new Random();

    public WorldApplicationService(WorldStorage storage, BlockFactory blockFactory) {
        this.storage = Objects.requireNonNull(storage, "storage no puede ser null");
        this.blockFactory = Objects.requireNonNull(blockFactory, "blockFactory no puede ser null");
        this.worldManager = WorldManager.getInstance();
    }

    public void createWorld(String name) throws IOException {
        createWorld(name, WorldSize.SMALL);
    }

    public void createWorld(String name, WorldSize size) throws IOException {
        Objects.requireNonNull(size, "size no puede ser null");
        long seed = seedGenerator.nextLong();
        World world = new World(name, seed, Instant.now().truncatedTo(ChronoUnit.SECONDS));
        if (storage.list().contains(world.getId())) {
            throw new IllegalStateException("Ya existe un mundo con el identificador: " + world.getId());
        }

        // El generador depende de la semilla, así que se construye por mundo y no por servicio.
        ChunkGenerationService generationService =
                new ChunkGenerationService(new SimpleTerrainGenerator(seed), blockFactory);
        generationService.generateChunks(size.chunksPerSide(), size.chunksPerSide())
                .forEach(world::addChunk);
        placePlayerOnSurface(world, size);

        storage.create(world);
        worldManager.load(world);
    }

    /**
     * Coloca al jugador sobre el punto de terreno más alto del chunk de origen.
     *
     * <p>La posición por defecto de {@code World} es fija y no puede conocer el relieve. Dejarla
     * tal cual tiene dos problemas: el jugador cae más de diez bloques cada vez que entra, y a
     * menudo aterriza en una hondonada rodeada de terreno más alto, con la cámara pegada a una
     * pared. Empezar en la cima resuelve ambos y da una vista abierta del mundo.
     *
     * <p>Se descartan las cimas de madera y hojas para no aparecer dentro de un árbol. Al cargar
     * un mundo guardado esto no se ejecuta: ahí manda la posición que el jugador tenía al salir.
     */
    private void placePlayerOnSurface(World world, WorldSize size) {
        int width = Chunk.WIDTH * size.chunksPerSide();
        int depth = Chunk.DEPTH * size.chunksPerSide();
        int centerX = width / 2;
        int centerZ = depth / 2;

        int bestX = centerX;
        int bestZ = centerZ;
        int bestTop = -1;
        int bestDistance = Integer.MAX_VALUE;

        // Buscar solo alrededor del centro: escanear todo un mundo grande sería costoso.
        for (int x = Math.max(0, centerX - Chunk.WIDTH / 2);
             x < Math.min(width, centerX + Chunk.WIDTH / 2); x++) {
            for (int z = Math.max(0, centerZ - Chunk.DEPTH / 2);
                 z < Math.min(depth, centerZ + Chunk.DEPTH / 2); z++) {
                int top = groundTopAt(world, x, z);
                if (top < bestTop) {
                    continue;
                }
                // A igual altura se prefiere el centro: nacer en el borde del mundo
                // deja media pantalla mirando al vacío.
                int distance = Math.abs(x - centerX) + Math.abs(z - centerZ);
                if (top > bestTop || distance < bestDistance) {
                    bestTop = top;
                    bestDistance = distance;
                    bestX = x;
                    bestZ = z;
                }
            }
        }

        Player player = world.getPlayer();
        player.setX(bestX + 0.5d);
        player.setZ(bestZ + 0.5d);
        player.setY(bestTop + 1d);
    }

    /** Altura del bloque de suelo más alto de una columna, o -1 si no hay ninguno. */
    private int groundTopAt(World world, int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            Position position = new Position(x, y, z);
            BlockType type = world.findChunk(position)
                    .flatMap(chunk -> chunk.getBlock(position))
                    .map(Block::getType)
                    .orElse(BlockType.AIR);

            if (type == BlockType.AIR) {
                continue;
            }
            // Un árbol no cuenta como suelo: aparecer dentro del tronco o de la copa
            // dejaría al jugador atascado.
            if (type == BlockType.WOOD || type == BlockType.LEAVES) {
                return -1;
            }
            return y;
        }
        return -1;
    }

    public List<String> listWorlds() throws IOException {
        return storage.list();
    }

    /** Consulta de solo lectura para que la presentación no tenga que hablar con el Singleton. */
    public Optional<String> currentWorldName() {
        return worldManager.getCurrentWorld().map(World::getName);
    }

    /** El mundo cargado, que la vista 3D necesita para dibujarlo y para moverse por él. */
    public Optional<World> currentWorld() {
        return worldManager.getCurrentWorld();
    }

    public void loadWorld(String id) throws IOException {
        World world = storage.read(id);
        worldManager.load(world);
    }

    public void saveCurrentWorld() throws IOException {
        World current = worldManager.getCurrentWorld()
                .orElseThrow(() -> new IllegalStateException("No hay un mundo cargado"));
        storage.update(current);
    }

    public void deleteWorld(String id) throws IOException {
        storage.delete(id);
        worldManager.getCurrentWorld()
                .filter(world -> world.getId().equals(id))
                .ifPresent(world -> worldManager.unload());
    }
}
