package presentation.game;

import application.PlayerFrameInput;
import com.badlogic.gdx.Input;
import domain.block.BlockType;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GameInputMappingTest {
    private Input device(boolean captured, Set<Integer> held, Set<Integer> pressed, Set<Integer> buttons) {
        return (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isCursorCatched" -> captured;
                    case "isKeyPressed" -> held.contains((Integer) args[0]);
                    case "isKeyJustPressed" -> pressed.contains((Integer) args[0]);
                    case "isButtonJustPressed" -> buttons.contains((Integer) args[0]);
                    case "getDeltaX" -> 10;
                    case "getDeltaY" -> -5;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
    @Test void translatesDevicesWithoutWorldPhysicsOrMutableControlState() {
        // Arrange: un doble de entrada sin ventana ni estado global de Gdx.
        Input device = device(true, Set.of(Input.Keys.W, Input.Keys.D, Input.Keys.SHIFT_RIGHT),
                Set.of(Input.Keys.SPACE, Input.Keys.Q, Input.Keys.NUM_6),
                Set.of(Input.Buttons.LEFT, Input.Buttons.RIGHT));
        // Act
        PlayerFrameInput input = new GameInput().read(device);
        // Assert
        assertTrue(input.movement().isForward());
        assertTrue(input.movement().isRight());
        assertTrue(input.movement().isSprint());
        assertFalse(input.movement().isBackward());
        assertFalse(input.movement().isLeft());
        assertTrue(input.jumpRequested());
        assertTrue(input.togglePistol());
        assertTrue(input.primaryAction());
        assertTrue(input.secondaryAction());
        assertEquals(2f, input.deltaYaw());
        assertEquals(1f, input.deltaPitch());
        assertEquals(BlockType.WOOD, input.selectedType());
    }
    @Test void uncapturedCursorSuppressesLookAndClicksButKeepsKeyboardIntent() {
        var input = new GameInput().read(device(false, Set.of(Input.Keys.S, Input.Keys.A, Input.Keys.SHIFT_LEFT),
                Set.of(Input.Keys.Q), Set.of(Input.Buttons.LEFT, Input.Buttons.RIGHT)));
        assertEquals(0, input.deltaYaw());
        assertEquals(0, input.deltaPitch());
        assertFalse(input.primaryAction());
        assertFalse(input.secondaryAction());
        assertTrue(input.togglePistol());
        assertTrue(input.movement().isBackward());
        assertTrue(input.movement().isLeft());
        assertTrue(input.movement().isSprint());
        assertNull(input.selectedType());
    }
    @Test void everyMaterialMapsInOrderAndSimultaneousKeysKeepTheFirstSlot() {
        BlockType[] expected = {BlockType.STONE, BlockType.DIRT, BlockType.GRASS, BlockType.SAND,
                BlockType.GRAVEL, BlockType.WOOD, BlockType.LEAVES};
        GameInput translator = new GameInput();
        for (int slot = 0; slot < expected.length; slot++) {
            assertEquals(expected[slot], translator.read(device(true, Set.of(),
                    Set.of(Input.Keys.NUM_1 + slot), Set.of())).selectedType());
        }
        assertEquals(BlockType.STONE, translator.read(device(true, Set.of(),
                Set.of(Input.Keys.NUM_1, Input.Keys.NUM_7), Set.of())).selectedType());
        assertNull(translator.read(device(true, Set.of(), Set.of(), Set.of())).selectedType());
    }
}
