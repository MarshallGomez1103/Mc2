package network;

import application.PlayerFrameInput;
import domain.player.MovementInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiplayerServerIT {
    @TempDir Path directory;
    @Test void threeClientsSharePlayersAndKeepActionsAndAcksIsolated() throws Exception {
        try (GameServer server = new GameServer(new ServerConfig(0,0,20,0,directory));
             NetworkClient a = new NetworkClient("127.0.0.1",server.gamePort(),"Jasub",0);
             NetworkClient b = new NetworkClient("127.0.0.1",server.gamePort(),"Thomas",1);
             NetworkClient c = new NetworkClient("127.0.0.1",server.gamePort(),"Ethian",2)) {
            await(() -> ready(a,3) && ready(b,3) && ready(c,3));
            Set<Long> ids = Set.of(a.welcome().playerId(),b.welcome().playerId(),c.welcome().playerId());
            assertEquals(3,ids.size());
            double ay = own(a).yaw(), by = own(b).yaw(), cy = own(c).yaw();
            assertTrue(a.send(new PlayerFrameInput(new MovementInput(false,false,false,false),false,20,0,false,false,false,null)));
            await(() -> a.latestState().ack() == 1 && player(b,a.welcome().playerId()).yaw() == (ay+20)%360);
            assertEquals(0,b.latestState().ack()); assertEquals(0,c.latestState().ack());
            assertEquals(by,own(b).yaw()); assertEquals(cy,own(c).yaw());
            assertEquals(Set.of(0,1,2),new HashSet<>(a.latestState().players().stream().map(ClientProtocol.PlayerView::skinId).toList()));
            b.close(); await(() -> ready(a,2) && ready(c,2));
            assertFalse(a.latestState().players().stream().anyMatch(p -> p.playerId()==b.welcome().playerId()));
        }
    }
    @Test void lastDisconnectLeavesNoGhostOnReconnect() throws Exception {
        try (GameServer server = new GameServer(new ServerConfig(0,0,20,0,directory))) {
            long id;
            try (NetworkClient a = new NetworkClient("127.0.0.1",server.gamePort(),"first",0)) {
                await(() -> ready(a,1)); id = a.welcome().playerId();
            }
            Thread.sleep(150);
            try (NetworkClient b = new NetworkClient("127.0.0.1",server.gamePort(),"second",2)) {
                await(() -> ready(b,1)); assertEquals(id,b.welcome().playerId());
                assertEquals("second",own(b).name()); assertEquals(2,own(b).skinId());
            }
        }
    }
    private static ClientProtocol.PlayerView own(NetworkClient c) { return player(c,c.welcome().playerId()); }
    private static ClientProtocol.PlayerView player(NetworkClient c,long id) { return c.latestState().players().stream().filter(p->p.playerId()==id).findFirst().orElseThrow(); }
    private static boolean ready(NetworkClient c,int size) { return c.latestState()!=null && c.latestState().players().size()==size; }
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long end = System.nanoTime()+Duration.ofSeconds(8).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime()<end) Thread.sleep(15);
        assertTrue(condition.getAsBoolean(),"Timed out waiting for shared state");
    }
}
