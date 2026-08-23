package persistence;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Traduce entre {@link World} y el esquema JSON acordado en docs/decisiones-compartidas.md.
 * Las dos direcciones viven juntas a propósito: cualquier cambio de formato se revisa de un vistazo.
 *
 * <p>Las posiciones de {@link Block} son absolutas del mundo, mientras que el archivo guarda
 * coordenadas locales al chunk. La conversión en ambos sentidos ocurre aquí.
 */
public final class WorldJsonCodec {
    /** Única versión de esquema admitida. Es un detalle del formato, no del dominio. */
    public static final int SCHEMA_VERSION = 1;

    private static final String ID_PATTERN = "[a-zA-Z0-9_-]+";
    private static final int ESTIMATED_BYTES_PER_BLOCK = 48;

    private WorldJsonCodec() {
    }

    // ----------------------------------------------------------------- escritura

    /** Convierte un mundo completo en el texto JSON que se guardará en disco. */
    public static String write(World world) {
        Objects.requireNonNull(world, "world no puede ser null");

        int blockCount = 0;
        for (Chunk chunk : world.getChunks()) {
            blockCount += chunk.getBlocks().size();
        }
        StringBuilder json = new StringBuilder(blockCount * ESTIMATED_BYTES_PER_BLOCK + 512);

        json.append("{\n");
        json.append("  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n");
        json.append("  \"id\": ").append(quote(world.getId())).append(",\n");
        json.append("  \"name\": ").append(quote(world.getName())).append(",\n");
        json.append("  \"seed\": ").append(world.getSeed()).append(",\n");
        json.append("  \"createdAt\": ").append(quote(world.getCreatedAt().toString())).append(",\n");

        // El jugador usa coordenadas continuas y orientación; la velocidad vertical y onGround
        // son estado transitorio de la física y se recalculan al cargar.
        Player player = world.getPlayer();
        json.append("  \"player\": { \"x\": ").append(number(player.getX()))
                .append(", \"y\": ").append(number(player.getY()))
                .append(", \"z\": ").append(number(player.getZ()))
                .append(", \"yaw\": ").append(number(player.getYaw()))
                .append(", \"pitch\": ").append(number(player.getPitch()))
                .append(" },\n");

        if (world.getChunks().isEmpty()) {
            json.append("  \"chunks\": []\n");
        } else {
            json.append("  \"chunks\": [\n");
            boolean firstChunk = true;
            for (Chunk chunk : world.getChunks()) {
                if (!firstChunk) {
                    json.append(",\n");
                }
                firstChunk = false;
                appendChunk(json, chunk);
            }
            json.append("\n  ]\n");
        }

        json.append("}\n");
        return json.toString();
    }

    private static void appendChunk(StringBuilder json, Chunk chunk) {
        json.append("    {\n");
        json.append("      \"x\": ").append(chunk.getChunkX()).append(",\n");
        json.append("      \"z\": ").append(chunk.getChunkZ()).append(",\n");
        json.append("      \"blocks\": [");

        boolean firstBlock = true;
        for (Block block : chunk.getBlocks()) {
            // El esquema no guarda aire: una coordenada ausente ya significa AIR.
            if (block.getType() == BlockType.AIR) {
                continue;
            }
            json.append(firstBlock ? "\n" : ",\n");
            firstBlock = false;

            Position position = block.getPosition();
            json.append("        {\"x\": ").append(Math.floorMod(position.x(), Chunk.WIDTH))
                    .append(", \"y\": ").append(position.y())
                    .append(", \"z\": ").append(Math.floorMod(position.z(), Chunk.DEPTH))
                    .append(", \"type\": ").append(quote(block.getType().name()))
                    .append("}");
        }

        json.append(firstBlock ? "]\n" : "\n      ]\n");
        json.append("    }");
    }

    /** JSON no admite NaN ni infinito, así que un mundo con esos valores no se puede guardar. */
    private static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(
                    "El jugador tiene un valor no finito y no se puede guardar: " + value
            );
        }
        return Double.toString(value);
    }

    private static String number(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(
                    "El jugador tiene un valor no finito y no se puede guardar: " + value
            );
        }
        return Float.toString(value);
    }

    private static String quote(String text) {
        StringBuilder quoted = new StringBuilder(text.length() + 2);
        quoted.append('"');
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            switch (current) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\b' -> quoted.append("\\b");
                case '\f' -> quoted.append("\\f");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (current < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) current));
                    } else {
                        quoted.append(current);
                    }
                }
            }
        }
        quoted.append('"');
        return quoted.toString();
    }

    // ------------------------------------------------------------------ lectura

    /**
     * Reconstruye un mundo a partir del texto JSON.
     * Cualquier incumplimiento del esquema termina en {@link InvalidWorldFileException}:
     * un archivo inválido nunca se carga a medias ni en silencio.
     */
    public static World read(String json, BlockFactory blockFactory) throws InvalidWorldFileException {
        Objects.requireNonNull(blockFactory, "blockFactory no puede ser null");
        Map<String, Object> root = asObject(JsonParser.parse(json), "la raíz del documento");

        long schemaVersion = requireLong(root, "schemaVersion", "la raíz");
        if (schemaVersion != SCHEMA_VERSION) {
            throw new InvalidWorldFileException(
                    "Versión de esquema no compatible: " + schemaVersion
                            + " (se esperaba " + SCHEMA_VERSION + ")"
            );
        }

        String id = requireString(root, "id", "la raíz");
        if (!id.matches(ID_PATTERN)) {
            throw new InvalidWorldFileException(
                    "El campo 'id' solo puede contener letras, números, guion y guion bajo: '" + id + "'"
            );
        }
        String name = requireString(root, "name", "la raíz");
        if (name.isBlank()) {
            throw new InvalidWorldFileException("El campo 'name' no puede estar vacío");
        }
        long seed = requireLong(root, "seed", "la raíz");
        Instant createdAt = parseInstant(requireString(root, "createdAt", "la raíz"));
        Player player = readPlayer(asObject(require(root, "player", "la raíz"), "player"));

        World world = new World(id, name, seed, createdAt, player);

        List<Object> chunks = asArray(require(root, "chunks", "la raíz"), "chunks");
        Set<String> seenChunks = new HashSet<>();
        for (int i = 0; i < chunks.size(); i++) {
            String path = "chunks[" + i + "]";
            Chunk chunk = readChunk(asObject(chunks.get(i), path), path, blockFactory);
            if (!seenChunks.add(chunk.getChunkX() + ":" + chunk.getChunkZ())) {
                throw new InvalidWorldFileException(
                        "Chunk repetido en " + path + ": ("
                                + chunk.getChunkX() + ", " + chunk.getChunkZ() + ")"
                );
            }
            world.addChunk(chunk);
        }
        return world;
    }

    private static Player readPlayer(Map<String, Object> source) throws InvalidWorldFileException {
        double x = requireDouble(source, "x", "player");
        double y = requireDouble(source, "y", "player");
        double z = requireDouble(source, "z", "player");
        // El jugador ocupa coordenadas continuas: y = 64.0 significa de pie sobre el bloque más alto.
        if (y < 0 || y > Chunk.HEIGHT) {
            throw new InvalidWorldFileException(
                    "La altura del jugador debe estar entre 0 y " + Chunk.HEIGHT + ": " + y
            );
        }

        Player player = new Player(x, y, z);
        // setYaw normaliza y setPitch recorta: el propio Player es dueño de esos límites.
        player.setYaw((float) requireDouble(source, "yaw", "player"));
        player.setPitch((float) requireDouble(source, "pitch", "player"));
        return player;
    }

    private static Chunk readChunk(Map<String, Object> source, String path, BlockFactory blockFactory)
            throws InvalidWorldFileException {
        int chunkX = requireInt(source, "x", path);
        int chunkZ = requireInt(source, "z", path);
        Chunk chunk = new Chunk(chunkX, chunkZ);

        List<Object> blocks = asArray(require(source, "blocks", path), path + ".blocks");
        Set<Position> seenPositions = new HashSet<>();

        for (int i = 0; i < blocks.size(); i++) {
            String blockPath = path + ".blocks[" + i + "]";
            Map<String, Object> entry = asObject(blocks.get(i), blockPath);

            int localX = requireInt(entry, "x", blockPath);
            int localY = requireInt(entry, "y", blockPath);
            int localZ = requireInt(entry, "z", blockPath);
            validateLocalPosition(localX, localY, localZ, blockPath);

            BlockType type = readBlockType(requireString(entry, "type", blockPath), blockPath);

            if (!seenPositions.add(new Position(localX, localY, localZ))) {
                throw new InvalidWorldFileException(
                        "Coordenada local repetida en " + blockPath
                                + ": (" + localX + ", " + localY + ", " + localZ + ")"
                );
            }

            Position absolute = new Position(
                    chunkX * Chunk.WIDTH + localX,
                    localY,
                    chunkZ * Chunk.DEPTH + localZ
            );
            chunk.addBlock(blockFactory.create(type, absolute));
        }
        return chunk;
    }

    private static void validateLocalPosition(int x, int y, int z, String path)
            throws InvalidWorldFileException {
        if (x < 0 || x >= Chunk.WIDTH || z < 0 || z >= Chunk.DEPTH) {
            throw new InvalidWorldFileException(
                    "Coordenada local fuera de 0 a " + (Chunk.WIDTH - 1) + " en " + path
                            + ": (x=" + x + ", z=" + z + ")"
            );
        }
        if (y < 0 || y >= Chunk.HEIGHT) {
            throw new InvalidWorldFileException(
                    "Altura fuera de 0 a " + (Chunk.HEIGHT - 1) + " en " + path + ": y=" + y
            );
        }
    }

    private static BlockType readBlockType(String value, String path) throws InvalidWorldFileException {
        BlockType type;
        try {
            type = BlockType.valueOf(value);
        } catch (IllegalArgumentException cause) {
            throw new InvalidWorldFileException("Tipo de bloque desconocido en " + path + ": '" + value + "'");
        }
        if (type == BlockType.AIR) {
            throw new InvalidWorldFileException(
                    "El esquema no admite bloques AIR guardados; sobra la entrada en " + path
            );
        }
        return type;
    }

    private static Instant parseInstant(String value) throws InvalidWorldFileException {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException cause) {
            throw new InvalidWorldFileException(
                    "El campo 'createdAt' no es una fecha ISO-8601 válida: '" + value + "'"
            );
        }
    }

    // ------------------------------------------------- accesores con ruta del campo

    private static Object require(Map<String, Object> source, String field, String path)
            throws InvalidWorldFileException {
        if (!source.containsKey(field)) {
            throw new InvalidWorldFileException("Falta el campo obligatorio '" + field + "' en " + path);
        }
        Object value = source.get(field);
        if (value == null) {
            throw new InvalidWorldFileException("El campo '" + field + "' de " + path + " no puede ser nulo");
        }
        return value;
    }

    private static Map<String, Object> asObject(Object value, String path) throws InvalidWorldFileException {
        if (!(value instanceof Map<?, ?>)) {
            throw new InvalidWorldFileException("Se esperaba un objeto en " + path);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> members = (Map<String, Object>) value;
        return members;
    }

    private static List<Object> asArray(Object value, String path) throws InvalidWorldFileException {
        if (!(value instanceof List<?>)) {
            throw new InvalidWorldFileException("Se esperaba una lista en " + path);
        }
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) value;
        return items;
    }

    private static String requireString(Map<String, Object> source, String field, String path)
            throws InvalidWorldFileException {
        Object value = require(source, field, path);
        if (!(value instanceof String text)) {
            throw new InvalidWorldFileException(
                    "El campo '" + field + "' de " + path + " debe ser una cadena de texto"
            );
        }
        return text;
    }

    private static long requireLong(Map<String, Object> source, String field, String path)
            throws InvalidWorldFileException {
        Object value = require(source, field, path);
        if (!(value instanceof Long number)) {
            throw new InvalidWorldFileException(
                    "El campo '" + field + "' de " + path + " debe ser un número entero"
            );
        }
        return number;
    }

    /** Acepta tanto enteros como decimales: {@code "y": 32} y {@code "y": 32.5} son válidos. */
    private static double requireDouble(Map<String, Object> source, String field, String path)
            throws InvalidWorldFileException {
        Object value = require(source, field, path);
        if (value instanceof Long integer) {
            return integer.doubleValue();
        }
        if (value instanceof Double number) {
            if (number.isNaN() || number.isInfinite()) {
                throw new InvalidWorldFileException(
                        "El campo '" + field + "' de " + path + " debe ser un número finito"
                );
            }
            return number;
        }
        throw new InvalidWorldFileException(
                "El campo '" + field + "' de " + path + " debe ser un número"
        );
    }

    private static int requireInt(Map<String, Object> source, String field, String path)
            throws InvalidWorldFileException {
        long value = requireLong(source, field, path);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new InvalidWorldFileException(
                    "El campo '" + field + "' de " + path + " está fuera del rango de un entero: " + value
            );
        }
        return (int) value;
    }
}
