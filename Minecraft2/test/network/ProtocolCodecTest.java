package network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCodecTest {
    @Test void helloKeepsUnicodeNameAndSkin() throws Exception {
        var hello = (ProtocolCodec.Hello) ProtocolCodec.decode("{\"type\":\"hello\",\"name\":\"Jasub á\",\"skinId\":2}");
        assertEquals("Jasub á", hello.name());
        assertEquals(2, hello.skinId());
    }
    @Test void flatInputMatchesEthiansConsumer() throws Exception {
        var input = (ProtocolCodec.Input) ProtocolCodec.decode("""
                {"type":"input","seq":7,"forward":true,"backward":false,"left":false,"right":false,
                 "sprint":true,"jump":true,"deltaYaw":0.5,"deltaPitch":-1.0,
                 "primary":true,"secondary":false,"togglePistol":true}
                """);
        assertEquals(7, input.seq());
        assertTrue(input.frame().movement().isForward());
        assertTrue(input.frame().movement().isSprint());
        assertTrue(input.frame().jumpRequested());
        assertTrue(input.frame().primaryAction());
        assertTrue(input.frame().togglePistol());
        assertEquals(.5f, input.frame().deltaYaw());
        assertNull(input.frame().selectedType());
    }
    @Test void omittedActionsAreNeutralAndByeIsRecognized() throws Exception {
        var input = (ProtocolCodec.Input) ProtocolCodec.decode("{\"type\":\"input\",\"seq\":1}");
        assertFalse(input.frame().movement().isForward());
        assertFalse(input.frame().primaryAction());
        assertInstanceOf(ProtocolCodec.Bye.class, ProtocolCodec.decode("{\"type\":\"bye\"}"));
    }
    @Test void rejectsInvalidTypesIdentityAndNumbers() {
        for (String json : List.of("[]", "null", "{}", "{\"type\":\"state\"}",
                "{\"type\":\"hello\",\"name\":\"\",\"skinId\":0}",
                "{\"type\":\"hello\",\"name\":\"a\",\"skinId\":3}",
                "{\"type\":\"hello\",\"name\":\"a\",\"skinId\":1.5}",
                "{\"type\":\"input\",\"seq\":-1}", "{\"type\":\"input\",\"seq\":0}",
                "{\"type\":\"input\",\"seq\":1.5}", "{\"type\":\"input\",\"seq\":1,\"forward\":\"true\"}",
                "{\"type\":\"input\",\"seq\":1,\"deltaYaw\":1e309}",
                "{\"type\":\"input\",\"seq\":1,\"deltaYaw\":361}",
                "{\"type\":\"input\",\"seq\":1,\"playerId\":42}",
                "{\"type\":\"input\",\"seq\":1,\"selectedType\":\"AIR\"}",
                "{\"type\":\"bye\",\"type\":\"hello\"}")) {
            assertThrows(IOException.class, () -> ProtocolCodec.decode(json), json);
        }
    }
    @Test void limitsMessagesAndRequiresACompleteUtf8Line() throws Exception {
        assertEquals("{}", ProtocolCodec.readLine(bytes("{}\r\n")));
        assertNull(ProtocolCodec.readLine(bytes("")));
        assertThrows(IOException.class, () -> ProtocolCodec.readLine(bytes("{}")));
        assertThrows(IOException.class, () -> ProtocolCodec.readLine(bytes("x".repeat(4097) + "\n")));
        assertThrows(IOException.class, () -> ProtocolCodec.readLine(new ByteArrayInputStream(new byte[]{(byte)0xc3,10})));
    }
    @Test void outputEscapesTextAndIsReadableByExistingParser() throws Exception {
        String encoded = ProtocolCodec.encode(Map.of("type", "welcome", "text", "a\"\\\n", "tickHz", 20));
        var parsed = (Map<?, ?>) persistence.JsonParser.parse(encoded);
        assertEquals("a\"\\\n", parsed.get("text"));
        assertEquals(20L, parsed.get("tickHz"));
        assertThrows(IllegalArgumentException.class, () -> ProtocolCodec.encode(Map.of("bad", Double.NaN)));
    }
    private static ByteArrayInputStream bytes(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
