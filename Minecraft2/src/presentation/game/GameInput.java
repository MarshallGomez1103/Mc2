package presentation.game;

import application.PlayerFrameInput;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import domain.block.BlockType;
import domain.player.MovementInput;
import java.util.Objects;

/** Traduce teclado y ratón a datos; aplicación decide cómo avanza la partida. */
public final class GameInput {
    private static final float MOUSE_SENSITIVITY = 0.20f;
    private static final BlockType[] PLACEABLE = {
            BlockType.STONE, BlockType.DIRT, BlockType.GRASS, BlockType.SAND,
            BlockType.GRAVEL, BlockType.WOOD, BlockType.LEAVES
    };

    public PlayerFrameInput read() { return read(Objects.requireNonNull(Gdx.input)); }

    /** Recibe el dispositivo para comprobar la traducción sin abrir una ventana. */
    PlayerFrameInput read(Input device) {
        boolean captured = device.isCursorCatched();
        MovementInput movement = new MovementInput(
                device.isKeyPressed(Input.Keys.W), device.isKeyPressed(Input.Keys.S),
                device.isKeyPressed(Input.Keys.A), device.isKeyPressed(Input.Keys.D),
                device.isKeyPressed(Input.Keys.SHIFT_LEFT) || device.isKeyPressed(Input.Keys.SHIFT_RIGHT));
        BlockType selection = null;
        for (int slot = 0; slot < PLACEABLE.length; slot++) {
            if (device.isKeyJustPressed(Input.Keys.NUM_1 + slot)) {
                selection = PLACEABLE[slot];
                break;
            }
        }
        return new PlayerFrameInput(movement, device.isKeyJustPressed(Input.Keys.SPACE),
                captured ? device.getDeltaX() * MOUSE_SENSITIVITY : 0,
                captured ? -device.getDeltaY() * MOUSE_SENSITIVITY : 0,
                captured && device.isButtonJustPressed(Input.Buttons.LEFT),
                captured && device.isButtonJustPressed(Input.Buttons.RIGHT),
                device.isKeyJustPressed(Input.Keys.Q), selection);
    }
}
