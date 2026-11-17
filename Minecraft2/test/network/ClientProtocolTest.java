package network;

import application.PlayerFrameInput;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientProtocolTest {
    @Test void intentionsRoundTripWithoutInventingAnIdentity() throws Exception {
        var message = (ProtocolCodec.Input) ProtocolCodec.decode(ClientProtocol.input(7, PlayerFrameInput.NEUTRAL));
        assertEquals(7, message.seq());
        assertFalse(message.frame().movement().isForward());
        assertEquals(new ProtocolCodec.Hello("Jasub", 2), ProtocolCodec.decode(ClientProtocol.hello("Jasub", 2)));
    }
    @Test void snapshotIsImmutableAndKeepsOtherPlayers() throws Exception {
        var state = (ClientProtocol.State) ClientProtocol.decode("""
            {"type":"state","tick":9,"ack":7,"players":[
             {"playerId":4,"name":"Thomas","skinId":1,"x":1,"y":2,"z":3,"yaw":45,"pitch":0,"health":100,"dead":false}],"zombies":[]}
            """);
        assertEquals(7, state.ack());
        assertEquals(4, state.players().get(0).playerId());
        assertEquals(1, state.players().get(0).skinId());
        assertThrows(UnsupportedOperationException.class, () -> state.players().clear());
    }
    @Test void rejectsMalformedServerMessages() {
        for (String json : List.of("{}", "{\"type\":\"hello\"}",
                "{\"type\":\"welcome\",\"playerId\":-1,\"tickHz\":20}",
                "{\"type\":\"welcome\",\"playerId\":0,\"tickHz\":0}",
                "{\"type\":\"state\",\"tick\":1,\"ack\":0,\"players\":[],\"zombies\":{}}")) {
            assertThrows(IOException.class, () -> ClientProtocol.decode(json), json);
        }
    }
    @Test void serverFramesHaveTheirOwnBound() throws Exception {
        String message = " ".repeat(5000) + "{}\n";
        assertEquals(5002, ClientProtocol.readLine(new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8))).length());
        assertThrows(IOException.class, () -> ClientProtocol.readLine(new ByteArrayInputStream(
                (" ".repeat(ClientProtocol.MAX_STATE_BYTES + 1) + "\n").getBytes(StandardCharsets.UTF_8))));
    }
}
