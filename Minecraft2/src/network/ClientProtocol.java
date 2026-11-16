package network;

import application.PlayerFrameInput;
import persistence.JsonParser;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/** Client-side wire values. Snapshots are immutable and contain no live simulation objects. */
public final class ClientProtocol {
    public static final int MAX_STATE_BYTES = 1024 * 1024;
    public sealed interface Message permits Welcome, State { }
    public record WorldInfo(String name, long seed, int chunkCount) { }
    public record Welcome(long playerId, int tickHz, WorldInfo world) implements Message { }
    public record PickupView(double x, double y, double z) { }
    public record PlayerView(long playerId, String name, int skinId, double x, double y, double z,
                             float yaw, float pitch, double health, boolean dead, boolean pistolOwned, boolean pistolEquipped, PickupView pickup) {
        public PlayerView(long id, String name, int skin, double x, double y, double z, float yaw, float pitch, double health, boolean dead) {
            this(id,name,skin,x,y,z,yaw,pitch,health,dead,false,false,null);
        }
    }
    public record ZombieView(double x, double y, double z, double health, String state) { }
    public record State(long tick, long ack, List<PlayerView> players, List<ZombieView> zombies,
                        List<TerrainCodec.ChunkView> chunks, List<TerrainCodec.BlockView> blocks) implements Message {
        public State { players = List.copyOf(players); zombies = List.copyOf(zombies);
            chunks = List.copyOf(chunks); blocks = List.copyOf(blocks); }
    }
    private ClientProtocol() { }
    public static String hello(String name, int skinId) throws IOException {
        String json = ProtocolCodec.encode(Map.of("type", "hello", "name", name, "skinId", skinId));
        ProtocolCodec.decode(json);
        return json;
    }
    public static String input(long seq, PlayerFrameInput frame) throws IOException { return input(seq, frame, false); }
    public static String input(long seq, PlayerFrameInput frame, boolean respawn) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "input"); data.put("seq", seq); data.put("respawn", respawn);
        var movement = frame.movement();
        data.put("forward", movement.isForward()); data.put("backward", movement.isBackward());
        data.put("left", movement.isLeft()); data.put("right", movement.isRight()); data.put("sprint", movement.isSprint());
        data.put("jump", frame.jumpRequested()); data.put("deltaYaw", frame.deltaYaw()); data.put("deltaPitch", frame.deltaPitch());
        data.put("primary", frame.primaryAction()); data.put("secondary", frame.secondaryAction());
        data.put("togglePistol", frame.togglePistol());
        if (frame.selectedType() != null) data.put("selectedType", frame.selectedType().name());
        String json = ProtocolCodec.encode(data);
        ProtocolCodec.decode(json);
        return json;
    }
    /** Server snapshots exceed the much smaller bound for untrusted client intentions. */
    public static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') {
                try {
                    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
                } catch (CharacterCodingException e) { throw new IOException("Invalid server UTF-8", e); }
            }
            if (bytes.size() >= MAX_STATE_BYTES) throw new IOException("Server frame too large");
            bytes.write(value);
        }
        if (bytes.size() != 0) throw new IOException("Incomplete server frame");
        return null;
    }
    public static Message decode(String json) throws IOException {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_STATE_BYTES) throw new IOException("Invalid server frame");
        Map<?, ?> data = object(JsonParser.parse(json));
        if ("welcome".equals(data.get("type"))) {
            WorldInfo info = null;
            if (data.containsKey("world")) {
                Map<?,?> world = object(data.get("world"));
                if (!(world.get("seed") instanceof Long seed)) throw new IOException("Invalid world seed");
                info = new WorldInfo(text(world, "name", 64), seed, (int) boundedInteger(world, "chunkCount", 1, 4096));
            }
            return new Welcome(integer(data, "playerId"), (int) boundedInteger(data, "tickHz", 1, 240), info);
        }
        if (!"state".equals(data.get("type"))) throw new IOException("Unknown server message");
        List<PlayerView> players = new ArrayList<>(); Set<Long> ids = new HashSet<>();
        for (Object item : list(data, "players", 64)) {
            var p = object(item); long id = integer(p, "playerId");
            if (!ids.add(id)) throw new IOException("Duplicate player id");
            players.add(new PlayerView(id, text(p, "name", 32), (int) boundedInteger(p, "skinId", 0, 2),
                    number(p, "x"), number(p, "y"), number(p, "z"), angle(p, "yaw"), angle(p, "pitch"),
                    number(p, "health"), bool(p, "dead"), p.containsKey("pistolOwned") && bool(p,"pistolOwned"),
                    p.containsKey("pistolEquipped") && bool(p,"pistolEquipped"), pickup(p.get("pickup"))));
        }
        List<ZombieView> zombies = new ArrayList<>();
        for (Object item : list(data, "zombies", 4096)) {
            var z = object(item);
            zombies.add(new ZombieView(number(z, "x"), number(z, "y"), number(z, "z"), number(z, "health"), text(z, "state", 32)));
        }
        List<TerrainCodec.ChunkView> chunks = new ArrayList<>();
        if (data.containsKey("chunks")) for (Object chunk : list(data, "chunks", 4)) chunks.add(TerrainCodec.decode(chunk));
        List<TerrainCodec.BlockView> blocks = new ArrayList<>();
        if (data.containsKey("blocks")) for (Object block : list(data, "blocks", 8192)) blocks.add(TerrainCodec.block(block));
        return new State(integer(data, "tick"), integer(data, "ack"), players, zombies, chunks, blocks);
    }
    private static PickupView pickup(Object value) throws IOException {
        if(value==null)return null;
        var p=object(value);return new PickupView(number(p,"x"),number(p,"y"),number(p,"z"));
    }
    private static Map<?, ?> object(Object value) throws IOException {
        if (!(value instanceof Map<?, ?> map)) throw new IOException("Expected object"); return map;
    }
    private static List<?> list(Map<?, ?> map, String key, int max) throws IOException {
        if (!(map.get(key) instanceof List<?> list) || list.size() > max) throw new IOException("Invalid " + key); return list;
    }
    private static long integer(Map<?, ?> map, String key) throws IOException { return boundedInteger(map, key, 0, 9_007_199_254_740_991L); }
    private static long boundedInteger(Map<?, ?> map, String key, long min, long max) throws IOException {
        if (!(map.get(key) instanceof Long n) || n < min || n > max) throw new IOException("Invalid " + key); return n;
    }
    private static double number(Map<?, ?> map, String key) throws IOException {
        if (!(map.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue())) throw new IOException("Invalid " + key); return n.doubleValue();
    }
    private static float angle(Map<?, ?> map, String key) throws IOException {
        double n = number(map, key); if (Math.abs(n) > Float.MAX_VALUE) throw new IOException("Invalid angle"); return (float)n;
    }
    private static String text(Map<?, ?> map, String key, int max) throws IOException {
        if (!(map.get(key) instanceof String s) || s.isBlank() || s.length() > max || s.chars().anyMatch(Character::isISOControl)) throw new IOException("Invalid " + key); return s;
    }
    private static boolean bool(Map<?, ?> map, String key) throws IOException {
        if (!(map.get(key) instanceof Boolean b)) throw new IOException("Invalid " + key); return b;
    }
}
