package network;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import patterns.factory.BlockFactory;
import java.io.IOException;
import java.util.*;

/** RLE of all cells in x,z,y order. The palette travels by name; enum ordinals never cross the wire. */
public final class TerrainCodec {
    public static final int CELLS = Chunk.WIDTH * Chunk.DEPTH * Chunk.HEIGHT;
    public record ChunkView(int x, int z, List<String> palette, List<Integer> runs) {
        public ChunkView { palette = List.copyOf(palette); runs = List.copyOf(runs); }
    }
    public record BlockView(int x, int y, int z, BlockType type) { }
    private TerrainCodec() { }
    public static Map<String,Object> encode(Chunk chunk) {
        String[] cells = new String[CELLS]; Arrays.fill(cells, "AIR");
        for (var block : chunk.getBlocks()) {
            Position p = block.getPosition();
            int index = ((Math.floorMod(p.x(), Chunk.WIDTH) * Chunk.DEPTH) + Math.floorMod(p.z(), Chunk.DEPTH)) * Chunk.HEIGHT + p.y();
            cells[index] = block.getType().name();
        }
        List<String> palette = new ArrayList<>(); List<Integer> runs = new ArrayList<>();
        for (int i = 0; i < cells.length;) {
            String value = cells[i]; int end = i + 1;
            while (end < cells.length && value.equals(cells[end])) end++;
            if (!palette.contains(value)) palette.add(value);
            runs.add(end - i); runs.add(palette.indexOf(value)); i = end;
        }
        return Map.of("x", chunk.getChunkX(), "z", chunk.getChunkZ(), "palette", palette, "runs", runs);
    }
    public static ChunkView decode(Object value) throws IOException {
        if (!(value instanceof Map<?,?> map) || !(map.get("palette") instanceof List<?> colors)
                || colors.isEmpty() || colors.size() > BlockType.values().length || !(map.get("runs") instanceof List<?> raw)
                || raw.size() % 2 != 0 || raw.size() > CELLS * 2) throw new IOException("Invalid chunk");
        List<String> palette = new ArrayList<>();
        for (Object color : colors) {
            if (!(color instanceof String name)) throw new IOException("Invalid palette");
            try { BlockType.valueOf(name); } catch (IllegalArgumentException e) { throw new IOException("Invalid material", e); }
            if (palette.contains(name)) throw new IOException("Duplicate material"); palette.add(name);
        }
        List<Integer> runs = new ArrayList<>(); int sum = 0;
        for (int i = 0; i < raw.size(); i += 2) {
            int count = integer(raw.get(i), 1, CELLS), color = integer(raw.get(i + 1), 0, palette.size() - 1);
            sum += count; if (sum > CELLS) throw new IOException("Too many cells");
            runs.add(count); runs.add(color);
        }
        if (sum != CELLS) throw new IOException("Incomplete chunk");
        return new ChunkView(integer(map.get("x"), -10000, 10000), integer(map.get("z"), -10000, 10000), palette, runs);
    }
    public static BlockView block(Object value) throws IOException {
        if (!(value instanceof Map<?,?> map) || !(map.get("type") instanceof String name)) throw new IOException("Invalid block");
        try { return new BlockView(integer(map.get("x"), -160000, 160015), integer(map.get("y"), 0, Chunk.HEIGHT - 1),
                integer(map.get("z"), -160000, 160015), BlockType.valueOf(name)); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid material", e); }
    }
    private static int integer(Object value, int min, int max) throws IOException {
        if (!(value instanceof Long n) || n < min || n > max) throw new IOException("Invalid terrain coordinate/run"); return n.intValue();
    }
    /** Construct a display copy only; this method never starts physics or simulation. */
    public static Chunk materialize(ChunkView view) {
        Chunk chunk = new Chunk(view.x(), view.z()); BlockFactory factory = new BlockFactory(); int cursor = 0;
        for (int i = 0; i < view.runs().size(); i += 2) {
            int count = view.runs().get(i); BlockType type = BlockType.valueOf(view.palette().get(view.runs().get(i + 1)));
            for (int end = cursor + count; cursor < end; cursor++) {
                if (type == BlockType.AIR) continue;
                int y = cursor % Chunk.HEIGHT, column = cursor / Chunk.HEIGHT;
                Position pos = new Position(view.x() * Chunk.WIDTH + column / Chunk.DEPTH, y,
                        view.z() * Chunk.DEPTH + column % Chunk.DEPTH);
                chunk.addBlock(factory.create(type, pos));
            }
        }
        return chunk;
    }
}
