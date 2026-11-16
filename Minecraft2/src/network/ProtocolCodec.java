package network;

import application.PlayerFrameInput;
import domain.block.BlockType;
import domain.player.MovementInput;
import persistence.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/** Bounded UTF-8 JSON-lines protocol. Only client intentions can enter the simulation. */
public final class ProtocolCodec {
    public static final int MAX_LINE_BYTES = 4096;
    private static final Set<String> INPUT_FIELDS = Set.of("type", "seq", "forward", "backward", "left", "right",
            "sprint", "jump", "deltaYaw", "deltaPitch", "primary", "secondary", "togglePistol", "selectedType");
    public sealed interface Message permits Hello, Input, Bye { }
    public record Hello(String name, int skinId) implements Message { }
    public record Input(long seq, PlayerFrameInput frame) implements Message { }
    public record Bye() implements Message { }
    private ProtocolCodec() { }

    /** EOF between messages is normal; EOF inside a frame is a protocol error. */
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int value;
        while ((value = in.read()) != -1) {
            if (value == '\n') {
                byte[] data = bytes.toByteArray();
                int length = data.length > 0 && data[data.length - 1] == '\r' ? data.length - 1 : data.length;
                try {
                    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data, 0, length)).toString();
                } catch (CharacterCodingException e) { throw new IOException("Invalid UTF-8", e); }
            }
            if (bytes.size() >= MAX_LINE_BYTES) throw new IOException("Message too large");
            bytes.write(value);
        }
        if (bytes.size() != 0) throw new IOException("Incomplete message");
        return null;
    }

    public static Message decode(String line) throws IOException {
        if (line == null || line.getBytes(StandardCharsets.UTF_8).length > MAX_LINE_BYTES) {
            throw new IOException("Missing or oversized message");
        }
        Object parsed = JsonParser.parse(line);
        if (!(parsed instanceof Map<?, ?> map)) throw new IOException("Expected JSON object");
        Object type = map.get("type");
        if ("hello".equals(type)) {
            fields(map, Set.of("type", "name", "skinId"));
            if (!(map.get("name") instanceof String name) || name.isBlank() || name.length() > 32
                    || name.chars().anyMatch(Character::isISOControl)) throw new IOException("Invalid player name");
            long skin = integer(map.get("skinId"), 0, 2);
            return new Hello(name.strip(), (int) skin);
        }
        if ("bye".equals(type)) {
            fields(map, Set.of("type"));
            return new Bye();
        }
        if (!"input".equals(type)) throw new IOException("Unknown client message type");
        fields(map, INPUT_FIELDS);
        long seq = integer(map.get("seq"), 1, 9_007_199_254_740_991L);
        BlockType selected = null;
        if (map.containsKey("selectedType") && map.get("selectedType") != null) {
            try {
                if (!(map.get("selectedType") instanceof String value)) throw new IllegalArgumentException();
                selected = BlockType.valueOf(value);
                if (selected == BlockType.AIR) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) { throw new IOException("Invalid selected material"); }
        }
        return new Input(seq, new PlayerFrameInput(new MovementInput(flag(map, "forward"), flag(map, "backward"),
                flag(map, "left"), flag(map, "right"), flag(map, "sprint")), flag(map, "jump"),
                angle(map, "deltaYaw", 360), angle(map, "deltaPitch", 180), flag(map, "primary"),
                flag(map, "secondary"), flag(map, "togglePistol"), selected));
    }

    private static void fields(Map<?, ?> map, Set<String> allowed) throws IOException {
        for (Object key : map.keySet()) if (!allowed.contains(key)) throw new IOException("Unknown field");
    }
    private static long integer(Object value, long min, long max) throws IOException {
        if (!(value instanceof Long n) || n < min || n > max) throw new IOException("Invalid integer");
        return n;
    }
    private static boolean flag(Map<?, ?> map, String key) throws IOException {
        if (!map.containsKey(key)) return false;
        if (!(map.get(key) instanceof Boolean value)) throw new IOException("Invalid boolean: " + key);
        return value;
    }
    private static float angle(Map<?, ?> map, String key, float max) throws IOException {
        if (!map.containsKey(key)) return 0;
        if (!(map.get(key) instanceof Number value) || !Double.isFinite(value.doubleValue())
                || Math.abs(value.doubleValue()) > max) throw new IOException("Invalid angle: " + key);
        return value.floatValue();
    }

    /** Encode server snapshots without reflection or executable polymorphic deserialization. */
    public static String encode(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }
    private static void append(StringBuilder out, Object value) {
        if (value == null) out.append("null");
        else if (value instanceof String text) {
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 32) out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int)c));
                        else out.append(c);
                    }
                }
            }
            out.append('"');
        } else if (value instanceof Number n) {
            if (!Double.isFinite(n.doubleValue())) throw new IllegalArgumentException("Nonfinite JSON number");
            out.append(n);
        } else if (value instanceof Boolean b) out.append(b);
        else if (value instanceof Map<?, ?> map) {
            out.append('{'); boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) out.append(','); first = false;
                append(out, entry.getKey().toString()); out.append(':'); append(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Iterable<?> list) {
            out.append('['); boolean first = true;
            for (Object item : list) {
                if (!first) out.append(','); first = false; append(out, item);
            }
            out.append(']');
        } else throw new IllegalArgumentException("Unsupported JSON value");
    }
}
