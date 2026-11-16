package network;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import patterns.factory.BlockFactory;
import persistence.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TerrainCodecTest {
    @Test void runLengthRoundTripPreservesNegativeCoordinatesAndHoles() throws Exception {
        Chunk chunk=new Chunk(-1,2);Position p=new Position(-2,20,35);
        chunk.addBlock(new BlockFactory().create(BlockType.WOOD,p));
        var decoded=TerrainCodec.materialize(TerrainCodec.decode(JsonParser.parse(ProtocolCodec.encode(TerrainCodec.encode(chunk)))));
        assertEquals(BlockType.WOOD,decoded.getBlock(p).orElseThrow().getType());
        assertEquals(1,decoded.getBlocks().size());
    }
    @Test void rejectsInvalidOrUnboundedRuns() {
        for(String runs:new String[]{"[]","[16385,0]","[1,99]","[-1,0]","[1,0,1]"}) {
            assertThrows(IOException.class,()->TerrainCodec.decode(JsonParser.parse("{\"x\":0,\"z\":0,\"palette\":[\"AIR\"],\"runs\":"+runs+"}")));
        }
    }
}
