package presentation.game;
import application.PlayerFrameInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NetworkInputBufferTest {
    @Test void retainsOneShotActionsAndLookUntilNetworkTick() {
        var buffer=new NetworkInputBuffer();
        buffer.add(new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(),true,4,2,true,false,false,null));
        buffer.add(new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(),false,5,3,false,true,true,null));
        var input=buffer.take();assertTrue(input.jumpRequested());assertTrue(input.primaryAction());assertTrue(input.secondaryAction());
        assertTrue(input.togglePistol());assertEquals(9,input.deltaYaw());assertEquals(5,input.deltaPitch());
        var next=buffer.take();assertFalse(next.jumpRequested());assertFalse(next.primaryAction());assertEquals(0,next.deltaYaw());
    }
    @Test void pauseClearsUnsentIntentions() {
        var b=new NetworkInputBuffer();b.add(new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(),true,8,3,true,false,true,null));
        b.clear();assertEquals(PlayerFrameInput.NEUTRAL,b.take());
    }
}
