package persistence;

import domain.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Acceso a disco: duplicados, inexistentes, identificadores inválidos y reescritura. */
@DisplayName("Almacenamiento de mundos en archivos")
class JsonWorldStorageTest {

    private WorldStorage storageIn(Path directory) {
        return new JsonWorldStorage(directory, WorldSamples.FACTORY);
    }

    @Test
    @DisplayName("no permite crear dos mundos con el mismo identificador")
    void rechazaIdentificadoresDuplicados(@TempDir Path directory) throws Exception {
        WorldStorage storage = storageIn(directory);
        storage.create(WorldSamples.twoChunkWorld("mundo_repetido"));

        assertThrows(IllegalStateException.class,
                () -> storage.create(WorldSamples.twoChunkWorld("mundo_repetido")));
    }

    @Test
    @DisplayName("avisa cuando el mundo no existe, también al eliminar")
    void avisaCuandoElMundoNoExiste(@TempDir Path directory) {
        WorldStorage storage = storageIn(directory);

        assertThrows(IllegalArgumentException.class, () -> storage.read("no_existe"));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("no_existe"));
        assertThrows(IllegalArgumentException.class,
                () -> storage.update(WorldSamples.twoChunkWorld("no_existe")));
    }

    @Test
    @DisplayName("rechaza identificadores que no sirven como nombre de archivo")
    void rechazaIdentificadoresInvalidos(@TempDir Path directory) {
        WorldStorage storage = storageIn(directory);

        assertThrows(IllegalArgumentException.class, () -> storage.read("con espacios"));
        assertThrows(IllegalArgumentException.class, () -> storage.read("../fuera"));
        assertThrows(IllegalArgumentException.class, () -> storage.read(""));
    }

    @Test
    @DisplayName("cargar y volver a guardar deja el archivo equivalente")
    void reescrituraConservaElContenido(@TempDir Path directory) throws Exception {
        WorldStorage storage = storageIn(directory);
        storage.create(WorldSamples.twoChunkWorld("mundo_reescrito"));
        String afterCreate = Files.readString(directory.resolve("mundo_reescrito.json"),
                StandardCharsets.UTF_8);

        storage.update(storage.read("mundo_reescrito"));

        assertEquals(afterCreate,
                Files.readString(directory.resolve("mundo_reescrito.json"), StandardCharsets.UTF_8),
                "el ciclo cargar → guardar no debe alterar el archivo");
    }

    @Test
    @DisplayName("el listado refleja las altas y las bajas")
    void listadoRefleja(@TempDir Path directory) throws Exception {
        WorldStorage storage = storageIn(directory);
        assertTrue(storage.list().isEmpty());

        storage.create(WorldSamples.twoChunkWorld("mundo_uno"));
        storage.create(WorldSamples.twoChunkWorld("mundo_dos"));
        assertEquals(java.util.List.of("mundo_dos", "mundo_uno"), storage.list(),
                "el listado va ordenado alfabéticamente");

        storage.delete("mundo_uno");
        assertFalse(storage.list().contains("mundo_uno"));
    }

    @Test
    @DisplayName("detecta un archivo renombrado a mano")
    void detectaIdentificadorQueNoCoincide(@TempDir Path directory) throws Exception {
        WorldStorage storage = storageIn(directory);
        World world = WorldSamples.twoChunkWorld("nombre_original");
        storage.create(world);

        Files.move(directory.resolve("nombre_original.json"), directory.resolve("nombre_cambiado.json"));

        assertThrows(InvalidWorldFileException.class, () -> storage.read("nombre_cambiado"));
    }
}
