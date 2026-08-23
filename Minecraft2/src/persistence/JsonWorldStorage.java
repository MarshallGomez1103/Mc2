package persistence;

import domain.player.Player;
import domain.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Base de almacenamiento local en JSON.
 * La codificación y reconstrucción completas se dejan como trabajo del equipo.
 */
public final class JsonWorldStorage implements WorldStorage {
    private static final String JSON_EXTENSION = ".json";
    private final Path worldsDirectory;

    public JsonWorldStorage(Path worldsDirectory) {
        this.worldsDirectory = Objects.requireNonNull(worldsDirectory, "worldsDirectory no puede ser null");
    }

    @Override
    public void create(World world) throws IOException {
        Path file = worldFile(world.getName());
        ensureDirectory();
        if (Files.exists(file)) {
            throw new IllegalStateException("Ya existe un mundo con ese nombre");
        }
        Files.writeString(file, serializeSkeleton(world));
    }

    @Override
    public World read(String name) throws IOException {
        Path file = worldFile(name);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("El mundo no existe");
        }

        // TODO: reconstruir semilla, fecha, jugador, chunks y bloques leyendo el esquema completo.
        // ADVERTENCIA: mientras esto siga pendiente, el mundo devuelto pierde los chunks y recibe
        // una semilla y una fecha provisionales. Cargar y volver a guardar sobrescribe la metadata
        // real del archivo, así que no debe usarse cargar -> guardar sobre mundos que importen.
        String worldName = extractWorldName(Files.readString(file));
        return new World(worldName, worldName, 0L, Instant.EPOCH, new Player(World.DEFAULT_SPAWN));
    }

    @Override
    public void update(World world) throws IOException {
        Path file = worldFile(world.getName());
        ensureDirectory();
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("El mundo no existe");
        }
        Files.writeString(file, serializeSkeleton(world));
    }

    @Override
    public void delete(String name) throws IOException {
        Files.deleteIfExists(worldFile(name));
    }

    @Override
    public List<String> list() throws IOException {
        ensureDirectory();
        try (Stream<Path> files = Files.list(worldsDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(JSON_EXTENSION))
                    .map(fileName -> fileName.substring(0, fileName.length() - JSON_EXTENSION.length()))
                    .sorted()
                    .toList();
        }
    }

    private void ensureDirectory() throws IOException {
        Files.createDirectories(worldsDirectory);
    }

    private Path worldFile(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El nombre solo puede contener letras, números, guion y guion bajo"
            );
        }
        return worldsDirectory.resolve(name + JSON_EXTENSION);
    }

    private String serializeSkeleton(World world) {
        // TODO: incluir chunks y bloques cuando el equipo acuerde el esquema persistente.
        return "{\n  \"name\": \"" + world.getName() + "\",\n  \"chunks\": []\n}\n";
    }

    private String extractWorldName(String json) throws IOException {
        String marker = "\"name\"";
        int markerIndex = json.indexOf(marker);
        if (markerIndex < 0) {
            throw new IOException("El archivo JSON del mundo no tiene un nombre válido");
        }
        int colonIndex = json.indexOf(':', markerIndex + marker.length());
        if (colonIndex < 0) {
            throw new IOException("El archivo JSON del mundo no tiene un nombre válido");
        }
        int startQuote = json.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            throw new IOException("El archivo JSON del mundo no tiene un nombre válido");
        }
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            throw new IOException("El archivo JSON del mundo no tiene un nombre válido");
        }
        return json.substring(startQuote + 1, endQuote);
    }
}
