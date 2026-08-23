package manualtest;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;
import persistence.InvalidWorldFileException;
import persistence.JsonWorldStorage;
import persistence.WorldStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Comprobación manual de la persistencia JSON: CT-08 (guardar y cargar conserva todo)
 * y CT-09 (un archivo inválido nunca se carga en silencio).
 *
 * No hay JUnit configurado todavía, así que sigue la convención de
 * {@link WorldBlockOperationsTest}: un {@code main()} que lanza {@link AssertionError} si algo falla.
 * Trabaja sobre una carpeta temporal, nunca sobre {@code worlds/}.
 */
public final class WorldPersistenceTest {
    private static final BlockFactory FACTORY = new BlockFactory();
    private static final Instant CREATED_AT = Instant.parse("2026-08-20T20:30:00Z");

    /** Un chunk válido y completo, reutilizado al construir los archivos inválidos. */
    private static final String SAMPLE_CHUNK =
            "{ \"x\": 0, \"z\": 0, \"blocks\": [ {\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"STONE\"} ] }";

    private WorldPersistenceTest() {
    }

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("mc2-persistencia");
        try {
            verifyRoundTrip(directory);
            verifyPrecisionRoundTrip(directory);
            verifyStorageErrors(directory);
            verifyInvalidFiles(directory);

            System.out.println();
            System.out.println("PRUEBAS DE PERSISTENCIA SUPERADAS (CT-08 y CT-09)");
        } finally {
            deleteRecursively(directory);
        }
    }

    // ------------------------------------------------------------------- CT-08

    private static void verifyRoundTrip(Path directory) throws IOException {
        WorldStorage storage = new JsonWorldStorage(directory, FACTORY);
        World original = sampleWorld("mundo_ct08");
        storage.create(original);

        World loaded = storage.read("mundo_ct08");

        check(loaded.getId().equals(original.getId()), "id");
        check(loaded.getName().equals(original.getName()), "name");
        check(loaded.getSeed() == original.getSeed(), "seed");
        check(loaded.getCreatedAt().equals(original.getCreatedAt()), "createdAt");
        checkPlayer(loaded.getPlayer(), original.getPlayer());
        check(loaded.getChunks().size() == original.getChunks().size(), "número de chunks");
        check(blocksOf(loaded).equals(blocksOf(original)), "bloques (posición absoluta y tipo)");

        System.out.println("CT-08 ida y vuelta: OK (" + loaded.getChunks().size() + " chunks, "
                + blocksOf(loaded).size() + " bloques, incluye un chunk negativo)");
    }

    private static void verifyPrecisionRoundTrip(Path directory) throws IOException {
        WorldStorage storage = new JsonWorldStorage(directory, FACTORY);
        Instant precise = Instant.parse("2026-08-20T20:30:00.123456789Z");
        Player player = new Player(-0.125, 63.9375, 1024.5);
        player.setYaw(359.5f);
        player.setPitch(-88.75f);

        // Mundo sin chunks: comprueba también la rama "chunks": [] del escritor.
        World world = new World("mundo_precision", "mundo_precision", Long.MIN_VALUE, precise, player);
        storage.create(world);

        World loaded = storage.read("mundo_precision");
        check(loaded.getCreatedAt().equals(precise), "createdAt con nanosegundos");
        check(loaded.getSeed() == Long.MIN_VALUE, "semilla en el extremo del rango long");
        check(loaded.getChunks().isEmpty(), "mundo sin chunks");
        checkPlayer(loaded.getPlayer(), player);

        System.out.println("Precisión (nanosegundos, decimales, semilla extrema, cero chunks): OK");
    }

    // -------------------------------------------------------- errores de archivo

    private static void verifyStorageErrors(Path directory) throws IOException {
        WorldStorage storage = new JsonWorldStorage(directory, FACTORY);
        storage.create(sampleWorld("mundo_duplicado"));

        expectFailure(() -> storage.create(sampleWorld("mundo_duplicado")), "crear un identificador repetido");
        expectFailure(() -> storage.read("mundo_inexistente"), "leer un mundo inexistente");
        expectFailure(() -> storage.delete("mundo_inexistente"), "eliminar un mundo inexistente");
        expectFailure(() -> storage.update(sampleWorld("mundo_inexistente")), "actualizar un mundo inexistente");
        expectFailure(() -> storage.read("identificador invalido"), "identificador con espacios");

        // Guardar dos veces seguidas debe seguir dejando un archivo válido (escritura atómica).
        World stored = storage.read("mundo_duplicado");
        storage.update(stored);
        check(blocksOf(storage.read("mundo_duplicado")).equals(blocksOf(stored)),
                "cargar y volver a guardar conserva los bloques");

        check(storage.list().contains("mundo_duplicado"), "el mundo aparece en el listado");
        storage.delete("mundo_duplicado");
        check(!storage.list().contains("mundo_duplicado"), "el mundo eliminado desaparece del listado");

        System.out.println("Duplicados, inexistentes, identificadores inválidos y reescritura: OK");
    }

    // ------------------------------------------------------------------- CT-09

    private static void verifyInvalidFiles(Path directory) throws IOException {
        WorldStorage storage = new JsonWorldStorage(directory, FACTORY);
        Map<String, String> cases = new LinkedHashMap<>();

        cases.put("json_incompleto", "{ \"schemaVersion\": 1, ");
        cases.put("json_basura", "esto no es json");
        cases.put("raiz_no_objeto", "[ 1, 2, 3 ]");
        cases.put("cadena_sin_cerrar", "{ \"id\": \"abierta }");
        cases.put("contenido_sobrante", render(validFields("contenido_sobrante")) + "{ \"otro\": 1 }");
        cases.put("version_futura", broken("version_futura", f -> f.put("schemaVersion", "2")));
        cases.put("sin_version", broken("sin_version", f -> f.remove("schemaVersion")));
        cases.put("id_con_espacios", broken("id_con_espacios", f -> f.put("id", "\"id invalido\"")));
        cases.put("id_no_coincide", broken("id_no_coincide", f -> f.put("id", "\"otro_identificador\"")));
        cases.put("nombre_vacio", broken("nombre_vacio", f -> f.put("name", "\"\"")));
        cases.put("semilla_texto", broken("semilla_texto", f -> f.put("seed", "\"48271\"")));
        cases.put("fecha_invalida", broken("fecha_invalida", f -> f.put("createdAt", "\"20 de agosto\"")));
        cases.put("jugador_ausente", broken("jugador_ausente", f -> f.remove("player")));
        cases.put("jugador_sin_yaw", broken("jugador_sin_yaw",
                f -> f.put("player", "{ \"x\": 8.5, \"y\": 32.0, \"z\": 8.5, \"pitch\": 0.0 }")));
        cases.put("jugador_fuera_del_mundo", broken("jugador_fuera_del_mundo",
                f -> f.put("player", "{ \"x\": 0, \"y\": 200, \"z\": 0, \"yaw\": 0, \"pitch\": 0 }")));
        cases.put("chunks_no_lista", broken("chunks_no_lista", f -> f.put("chunks", "{}")));
        cases.put("chunk_repetido", broken("chunk_repetido",
                f -> f.put("chunks", "[ " + SAMPLE_CHUNK + ", " + SAMPLE_CHUNK + " ]")));
        cases.put("bloque_fuera_de_rango", broken("bloque_fuera_de_rango",
                f -> f.put("chunks", chunkWith("{\"x\": 16, \"y\": 2, \"z\": 3, \"type\": \"STONE\"}"))));
        cases.put("altura_fuera_de_rango", broken("altura_fuera_de_rango",
                f -> f.put("chunks", chunkWith("{\"x\": 1, \"y\": 64, \"z\": 3, \"type\": \"STONE\"}"))));
        cases.put("tipo_desconocido", broken("tipo_desconocido",
                f -> f.put("chunks", chunkWith("{\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"PIEDRA\"}"))));
        cases.put("tipo_aire", broken("tipo_aire",
                f -> f.put("chunks", chunkWith("{\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"AIR\"}"))));
        cases.put("coordenada_repetida", broken("coordenada_repetida",
                f -> f.put("chunks", chunkWith(
                        "{\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"STONE\"},"
                                + " {\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"DIRT\"}"))));

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Files.writeString(directory.resolve(entry.getKey() + ".json"), entry.getValue(),
                    StandardCharsets.UTF_8);
            String message = expectInvalid(storage, entry.getKey());
            System.out.println("  " + String.format("%-26s", entry.getKey()) + " -> " + message);
        }
        System.out.println("CT-09 archivos inválidos: OK (" + cases.size() + " casos, ninguno se cargó)");
    }

    // --------------------------------------------------------------- utilidades

    /** Mundo de prueba con dos chunks, uno de ellos negativo, y varios tipos de bloque. */
    private static World sampleWorld(String id) {
        Player player = new Player(8.5, 32.25, -3.75);
        player.setYaw(123.5f);
        player.setPitch(-45.25f);
        World world = new World(id, id, 48_271L, CREATED_AT, player);

        Chunk origin = new Chunk(0, 0);
        origin.addBlock(FACTORY.create(BlockType.STONE, new Position(0, 0, 0)));
        origin.addBlock(FACTORY.create(BlockType.GRASS, new Position(15, 63, 15)));
        origin.addBlock(FACTORY.create(BlockType.WOOD, new Position(7, 20, 9)));
        world.addChunk(origin);

        // Chunk con coordenadas negativas: aquí es donde floorDiv/floorMod se ganan el sueldo.
        Chunk negative = new Chunk(-1, -1);
        negative.addBlock(FACTORY.create(BlockType.SAND, new Position(-1, 10, -1)));      // local (15, 10, 15)
        negative.addBlock(FACTORY.create(BlockType.LEAVES, new Position(-16, 5, -16)));   // local (0, 5, 0)
        negative.addBlock(FACTORY.create(BlockType.GRAVEL, new Position(-8, 30, -3)));    // local (8, 30, 13)
        world.addChunk(negative);

        return world;
    }

    private static Map<Position, BlockType> blocksOf(World world) {
        Map<Position, BlockType> blocks = new HashMap<>();
        for (Chunk chunk : world.getChunks()) {
            for (Block block : chunk.getBlocks()) {
                blocks.put(block.getPosition(), block.getType());
            }
        }
        return blocks;
    }

    private static void checkPlayer(Player loaded, Player expected) {
        check(loaded.getX() == expected.getX(), "jugador x");
        check(loaded.getY() == expected.getY(), "jugador y");
        check(loaded.getZ() == expected.getZ(), "jugador z");
        check(loaded.getYaw() == expected.getYaw(), "jugador yaw");
        check(loaded.getPitch() == expected.getPitch(), "jugador pitch");
    }

    private static Map<String, String> validFields(String id) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("schemaVersion", "1");
        fields.put("id", "\"" + id + "\"");
        fields.put("name", "\"" + id + "\"");
        fields.put("seed", "48271");
        fields.put("createdAt", "\"2026-08-20T20:30:00Z\"");
        fields.put("player", "{ \"x\": 8.5, \"y\": 32.0, \"z\": 8.5, \"yaw\": 0.0, \"pitch\": 0.0 }");
        fields.put("chunks", "[ " + SAMPLE_CHUNK + " ]");
        return fields;
    }

    /** Parte de un documento válido y le aplica un único defecto. */
    private static String broken(String id, Consumer<Map<String, String>> defect) {
        Map<String, String> fields = validFields(id);
        defect.accept(fields);
        return render(fields);
    }

    private static String render(Map<String, String> fields) {
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("  \"").append(field.getKey()).append("\": ").append(field.getValue());
        }
        return json.append("\n}\n").toString();
    }

    private static String chunkWith(String blocks) {
        return "[ { \"x\": 0, \"z\": 0, \"blocks\": [ " + blocks + " ] } ]";
    }

    private static String expectInvalid(WorldStorage storage, String id) throws IOException {
        try {
            storage.read(id);
        } catch (InvalidWorldFileException expected) {
            String message = expected.getMessage();
            if (message == null || message.isBlank()) {
                throw new AssertionError("El error de '" + id + "' no trae mensaje legible");
            }
            return message;
        }
        throw new AssertionError("El archivo inválido '" + id + "' se cargó sin error");
    }

    private static void expectFailure(Action action, String description) {
        try {
            action.run();
        } catch (IOException | IllegalArgumentException | IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Debería haber fallado: " + description);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FALLO al comparar: " + description);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws IOException;
    }
}
