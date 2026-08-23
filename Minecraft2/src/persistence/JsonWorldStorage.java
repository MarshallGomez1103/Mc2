package persistence;

import domain.world.World;
import patterns.factory.BlockFactory;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Almacenamiento local de mundos, un archivo JSON por mundo dentro de {@code worlds/}.
 * Esta clase se ocupa solo del acceso a disco; el formato lo resuelve {@link WorldJsonCodec}.
 */
public final class JsonWorldStorage implements WorldStorage {
    private static final String JSON_EXTENSION = ".json";
    private static final String TEMPORARY_EXTENSION = ".tmp";

    private final Path worldsDirectory;
    private final BlockFactory blockFactory;

    public JsonWorldStorage(Path worldsDirectory, BlockFactory blockFactory) {
        this.worldsDirectory = Objects.requireNonNull(worldsDirectory, "worldsDirectory no puede ser null");
        this.blockFactory = Objects.requireNonNull(blockFactory, "blockFactory no puede ser null");
    }

    @Override
    public void create(World world) throws IOException {
        Path file = worldFile(world.getId());
        ensureDirectory();
        if (Files.exists(file)) {
            throw new IllegalStateException("Ya existe un mundo con el identificador: " + world.getId());
        }
        writeAtomically(file, WorldJsonCodec.write(world));
    }

    @Override
    public World read(String id) throws IOException {
        Path file = worldFile(id);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("El mundo no existe");
        }

        World world = WorldJsonCodec.read(readFile(file), blockFactory);
        if (!world.getId().equals(id)) {
            throw new InvalidWorldFileException(
                    "El identificador del archivo no coincide con su contenido: se esperaba '"
                            + id + "' y se encontró '" + world.getId() + "'"
            );
        }
        return world;
    }

    @Override
    public void update(World world) throws IOException {
        Path file = worldFile(world.getId());
        ensureDirectory();
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("El mundo no existe");
        }
        writeAtomically(file, WorldJsonCodec.write(world));
    }

    @Override
    public void delete(String id) throws IOException {
        if (!Files.deleteIfExists(worldFile(id))) {
            throw new IllegalArgumentException("El mundo no existe");
        }
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

    /**
     * Escribe primero en un archivo temporal y luego lo mueve sobre el definitivo.
     * Así un fallo a mitad de la escritura no deja el mundo guardado a medias.
     */
    private void writeAtomically(Path file, String json) throws IOException {
        Path temporary = file.resolveSibling(file.getFileName() + TEMPORARY_EXTENSION);
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException cause) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException cause) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Prevalece el error original de escritura.
            }
            throw cause;
        }
    }

    private String readFile(Path file) throws IOException {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (MalformedInputException cause) {
            throw new InvalidWorldFileException(
                    "El archivo del mundo está dañado: no contiene texto UTF-8 válido", cause
            );
        }
    }

    private void ensureDirectory() throws IOException {
        Files.createDirectories(worldsDirectory);
    }

    private Path worldFile(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El identificador solo puede contener letras, números, guion y guion bajo"
            );
        }
        return worldsDirectory.resolve(id + JSON_EXTENSION);
    }
}
