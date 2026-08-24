package presentation.game;

import application.PlayerInteractionService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import domain.block.BlockType;
import domain.player.CollisionResolver;
import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerMovementService;
import domain.player.PlayerPhysics;
import domain.world.World;

import java.util.Objects;

/**
 * Traduce teclado y ratón en llamadas a los servicios de movimiento, física, colisión e
 * interacción. No decide reglas del juego: solo lee qué está pulsado y delega.
 *
 * <p>Controles: W/A/S/D mueven, espacio salta, el ratón mira, clic izquierdo elimina el bloque
 * apuntado, clic derecho coloca, y las teclas 1 a 7 eligen el material a colocar.
 */
public final class GameInput {
    /**
     * Tope del paso de tiempo. Tras un tirón o al arrastrar la ventana, el delta real puede ser
     * de varios segundos; sin este límite el jugador avanzaría tanto en un solo fotograma que
     * atravesaría el suelo antes de que la colisión pudiera detectarlo.
     */
    private static final double MAX_DELTA_SECONDS = 0.05;

    private static final float MOUSE_SENSITIVITY = 0.20f;

    /** Materiales que el jugador puede colocar, en el orden de las teclas 1 a 7. */
    private static final BlockType[] PLACEABLE = {
            BlockType.STONE, BlockType.DIRT, BlockType.GRASS, BlockType.SAND,
            BlockType.GRAVEL, BlockType.WOOD, BlockType.LEAVES
    };

    private final PlayerMovementService movementService = new PlayerMovementService();
    private final PlayerPhysics physics = new PlayerPhysics();
    private final CollisionResolver collisionResolver = new CollisionResolver();
    private final PlayerInteractionService interactionService;

    private BlockType selectedType = BlockType.STONE;

    public GameInput(PlayerInteractionService interactionService) {
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
    }

    public BlockType getSelectedType() {
        return selectedType;
    }

    /** Procesa un fotograma completo de entrada y movimiento. */
    public void update(Player player, World world, float rawDeltaSeconds) {
        double deltaSeconds = Math.min(rawDeltaSeconds, MAX_DELTA_SECONDS);

        applyLook(player);
        applyMovement(player, world, deltaSeconds);
        applyInteraction(player, world);
        updateSelectedType();
    }

    /**
     * Mirar con el ratón. Los dos signos están invertidos a propósito: mover el ratón a la
     * derecha debe girar a la derecha, y como yaw crece hacia la izquierda hay que restarlo.
     * Lo mismo en vertical, porque el eje Y de la pantalla crece hacia abajo.
     */
    private void applyLook(Player player) {
        if (!Gdx.input.isCursorCatched()) {
            return;
        }
        float deltaYaw = -Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
        float deltaPitch = -Gdx.input.getDeltaY() * MOUSE_SENSITIVITY;
        movementService.look(player, deltaYaw, deltaPitch);
    }

    private void applyMovement(Player player, World world, double deltaSeconds) {
        MovementInput input = new MovementInput(
                Gdx.input.isKeyPressed(Input.Keys.W),
                Gdx.input.isKeyPressed(Input.Keys.S),
                Gdx.input.isKeyPressed(Input.Keys.A),
                Gdx.input.isKeyPressed(Input.Keys.D));

        double[] horizontal = movementService.computeIntendedDelta(player, input, deltaSeconds);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            physics.jump(player);
        }
        physics.applyGravity(player, deltaSeconds);
        double verticalDelta = physics.computeIntendedDeltaY(player, deltaSeconds);

        collisionResolver.resolveAndApply(player, world, horizontal[0], verticalDelta, horizontal[1]);
    }

    private void applyInteraction(Player player, World world) {
        if (!Gdx.input.isCursorCatched()) {
            return;
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            interactionService.removeTargetedBlock(player, world);
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            interactionService.placeBlockOfType(player, world, selectedType);
        }
    }

    private void updateSelectedType() {
        for (int slot = 0; slot < PLACEABLE.length; slot++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + slot)) {
                selectedType = PLACEABLE[slot];
                return;
            }
        }
    }
}
