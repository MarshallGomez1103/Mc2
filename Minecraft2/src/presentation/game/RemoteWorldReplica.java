package presentation.game;

import domain.Position;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import network.ClientProtocol;
import network.TerrainCodec;
import patterns.factory.BlockFactory;
import java.time.Instant;
import java.util.*;

/** Display copy, owned by the render thread. No local GameSession or physics is created. */
public final class RemoteWorldReplica {
    private final World world;
    private final int expectedChunks;
    private final Set<String> received = new HashSet<>();
    public RemoteWorldReplica(ClientProtocol.WorldInfo info) {
        world = new World("remote", info.seed(), Instant.EPOCH);
        expectedChunks = info.chunkCount();
    }
    public World world() { return world; }
    public boolean ready() { return received.size() == expectedChunks; }
    public int receivedChunks() { return received.size(); }
    public void apply(ClientProtocol.State update) {
        for (var chunk : update.chunks()) {
            String key = chunk.x() + ":" + chunk.z();
            if (!received.add(key)) throw new IllegalArgumentException("Duplicate initial chunk");
            if (received.size() > expectedChunks) throw new IllegalArgumentException("Too many chunks");
            world.addChunk(TerrainCodec.materialize(chunk));
        }
        for (var block : update.blocks()) {
            Position position = new Position(block.x(), block.y(), block.z());
            int x = Chunk.chunkXFor(position), z = Chunk.chunkZFor(position);
            // A not-yet-sent chunk will be transmitted with its current authoritative contents.
            if (world.findChunk(x,z).isEmpty()) continue;
            if (block.type() == BlockType.AIR) world.removeBlock(x,z,position);
            else world.placeBlock(x,z,new BlockFactory().create(block.type(),position));
        }
    }
    public Player local(ClientProtocol.PlayerView state) {
        Player player = world.getPlayer();
        player.setX(state.x()); player.setY(state.y()); player.setZ(state.z());
        player.setYaw(state.yaw()); player.setPitch(state.pitch()); player.setSkinId(state.skinId());
        return player;
    }
}
