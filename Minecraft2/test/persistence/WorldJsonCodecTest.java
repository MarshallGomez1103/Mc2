package persistence;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CT-08 — guardar y cargar conserva metadatos, jugador, chunks y bloques no-AIR.
 * Comparación campo a campo sobre el texto JSON, sin pasar por el sistema de archivos.
 */
@DisplayName("CT-08 · códec JSON del mundo")
class WorldJsonCodecTest {

    @Test
    @DisplayName("conserva metadatos, jugador y bloques en el ida y vuelta")
    void conservaTodoElMundo() throws Exception {
        World original = WorldSamples.twoChunkWorld("mundo_ct08");

        World loaded = WorldJsonCodec.read(WorldJsonCodec.write(original), WorldSamples.FACTORY);

        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getSeed(), loaded.getSeed());
        assertEquals(original.getCreatedAt(), loaded.getCreatedAt());
        assertEquals(original.getChunks().size(), loaded.getChunks().size());
        assertEquals(blocksOf(original), blocksOf(loaded),
                "cada posición absoluta debe volver con el mismo tipo");
    }

    @Test
    @DisplayName("conserva la posición continua y la orientación del jugador")
    void conservaElJugador() throws Exception {
        World original = WorldSamples.twoChunkWorld("mundo_jugador");
        Player expected = original.getPlayer();

        Player loaded = WorldJsonCodec
                .read(WorldJsonCodec.write(original), WorldSamples.FACTORY)
                .getPlayer();

        assertEquals(expected.getX(), loaded.getX());
        assertEquals(expected.getY(), loaded.getY());
        assertEquals(expected.getZ(), loaded.getZ());
        assertEquals(expected.getYaw(), loaded.getYaw());
        assertEquals(expected.getPitch(), loaded.getPitch());
    }

    @Test
    @DisplayName("un chunk negativo vuelve con sus coordenadas absolutas intactas")
    void conservaCoordenadasNegativas() throws Exception {
        World loaded = WorldJsonCodec.read(
                WorldJsonCodec.write(WorldSamples.twoChunkWorld("mundo_negativo")),
                WorldSamples.FACTORY);

        Chunk negative = loaded.findChunk(-1, -1).orElseThrow();
        assertEquals(BlockType.SAND, typeAt(negative, new Position(-1, 10, -1)));
        assertEquals(BlockType.LEAVES, typeAt(negative, new Position(-16, 5, -16)));
        assertEquals(BlockType.GRAVEL, typeAt(negative, new Position(-8, 30, -3)));
    }

    @Test
    @DisplayName("los bloques AIR no se escriben")
    void omiteBloquesDeAire() throws Exception {
        World world = WorldSamples.twoChunkWorld("mundo_aire");
        Position airPosition = new Position(4, 40, 4);
        world.findChunk(0, 0).orElseThrow()
                .addBlock(WorldSamples.FACTORY.create(BlockType.AIR, airPosition));

        String json = WorldJsonCodec.write(world);
        assertFalse(json.contains("AIR"), "el archivo no debe mencionar bloques de aire");

        World loaded = WorldJsonCodec.read(json, WorldSamples.FACTORY);
        assertTrue(loaded.findChunk(0, 0).orElseThrow().getBlock(airPosition).isEmpty(),
                "una coordenada ausente ya significa aire");
    }

    @Test
    @DisplayName("soporta nanosegundos, decimales, semilla extrema y cero chunks")
    void conservaLosValoresLimite() throws Exception {
        Instant precise = Instant.parse("2026-08-20T20:30:00.123456789Z");
        Player player = new Player(-0.125, 63.9375, 1024.5);
        player.setYaw(359.5f);
        player.setPitch(-88.75f);
        World world = new World("mundo_limite", "mundo_limite", Long.MIN_VALUE, precise, player);

        World loaded = WorldJsonCodec.read(WorldJsonCodec.write(world), WorldSamples.FACTORY);

        assertEquals(precise, loaded.getCreatedAt());
        assertEquals(Long.MIN_VALUE, loaded.getSeed());
        assertTrue(loaded.getChunks().isEmpty());
        assertEquals(-0.125, loaded.getPlayer().getX());
        assertEquals(63.9375, loaded.getPlayer().getY());
        assertEquals(359.5f, loaded.getPlayer().getYaw());
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

    private static BlockType typeAt(Chunk chunk, Position position) {
        return chunk.getBlock(position).map(Block::getType).orElse(null);
    }
}
