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
    @Test void clientsReceiveSameTerrainAndAuthoritativeBlockRemoval() throws Exception {
        var world=new domain.world.World("server",42,java.time.Instant.EPOCH);
        var chunk=new domain.world.Chunk(0,0);var factory=new patterns.factory.BlockFactory();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)chunk.addBlock(factory.create(domain.block.BlockType.DIRT,new domain.Position(x,1,z)));
        var target=new domain.Position(8,3,10);chunk.addBlock(factory.create(domain.block.BlockType.STONE,target));world.addChunk(chunk);
        world.getPlayer().setX(8.5);world.getPlayer().setY(2);world.getPlayer().setZ(8.5);
        java.nio.file.Files.writeString(directory.resolve("server.json"),persistence.WorldJsonCodec.write(world));
        try(GameServer server=new GameServer(new ServerConfig(0,0,20,0,directory));
            NetworkClient a=new NetworkClient("127.0.0.1",server.gamePort(),"a",0);
            NetworkClient b=new NetworkClient("127.0.0.1",server.gamePort(),"b",1)) {
            await(()->ready(a,2)&&ready(b,2));
            var ra=new presentation.game.RemoteWorldReplica(a.welcome().world());
            var rb=new presentation.game.RemoteWorldReplica(b.welcome().world());
            await(()->{drain(a,ra);drain(b,rb);return ra.ready()&&rb.ready();});
            assertEquals(domain.block.BlockType.STONE,ra.world().findChunk(0,0).orElseThrow().getBlock(target).orElseThrow().getType());
            assertEquals(ra.world().findChunk(0,0).orElseThrow().getBlocks().size(),rb.world().findChunk(0,0).orElseThrow().getBlocks().size());
            assertTrue(a.send(new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(),false,0,0,true,false,false,null)));
            await(()->{drain(a,ra);drain(b,rb);return a.latestState().ack()==1 &&
                    ra.world().findChunk(0,0).orElseThrow().getBlock(target).isEmpty()&&rb.world().findChunk(0,0).orElseThrow().getBlock(target).isEmpty();});
            assertEquals(0,b.latestState().ack());
        }
    }
    private static void drain(NetworkClient client,presentation.game.RemoteWorldReplica replica) {
        ClientProtocol.State next;while((next=client.pollTerrain())!=null)replica.apply(next);
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
