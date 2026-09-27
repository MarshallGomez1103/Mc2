package presentation.game;

import application.EnemyUpdateService;
import application.PlayerInteractionService;
import application.PistolService;
import application.ZombieMeleeService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import domain.block.BlockType;
import domain.player.CollisionResolver;
import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerMovementService;
import domain.player.PlayerPhysics;
import domain.player.Stamina;
import domain.world.World;

import java.util.Objects;

/**
 * Traduce teclado y ratón en llamadas a los servicios de movimiento, física, colisión e
 * interacción. No decide reglas del juego: solo lee qué está pulsado y delega.
 *
 * <p>Controles: W/A/S/D mueven, Shift corre, espacio salta, el ratón mira, clic izquierdo golpea al zombi
 * apuntado o, si no hay ninguno en alcance, elimina el bloque apuntado; clic derecho coloca, y las
 * teclas 1 a 7 eligen el material a colocar.
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
    private final ZombieMeleeService meleeService;
    private final EnemyUpdateService enemyService;
    private final PistolService pistol;

    private final Stamina stamina = new Stamina();
    private boolean sprinting;
    private boolean moving;
    private BlockType selectedType = BlockType.STONE;

    public Stamina stamina() { return stamina; }
    public boolean isSprinting() { return sprinting; }
    public boolean isMoving() { return moving; }

    public void resetAfterRespawn() {
        stamina.reset();
        sprinting = false;
        moving = false;
    }

    public GameInput(PlayerInteractionService interactionService, ZombieMeleeService meleeService,
                     EnemyUpdateService enemyService) {
        this(interactionService, meleeService, enemyService, null);
    }

    public GameInput(PlayerInteractionService interactionService, ZombieMeleeService meleeService,
                     EnemyUpdateService enemyService, PistolService pistol) {
        this.pistol = pistol;
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
        this.meleeService = Objects.requireNonNull(meleeService, "meleeService no puede ser null");
        this.enemyService = Objects.requireNonNull(enemyService, "enemyService no puede ser null");
    }

    public BlockType getSelectedType() {
        return selectedType;
    }

    /** Procesa un fotograma completo de entrada y movimiento. */
    public void update(Player player, World world, float rawDeltaSeconds) {
        double deltaSeconds = checkedDelta(rawDeltaSeconds);

        applyLook(player);
        applyMovement(player, world, deltaSeconds);
        if (pistol != null && Gdx.input.isKeyJustPressed(Input.Keys.Q)) pistol.toggleEquipped();
        updateSelectedType();
        applyInteraction(player, world);
    }

    /**
     * Mirar con el ratón. En la convención de la cámara, yaw creciente gira hacia la derecha,
     * por lo que el desplazamiento horizontal se conserva. El eje Y de la pantalla crece hacia
     * abajo, así que el desplazamiento vertical sí se invierte para conservar la mirada natural.
     */
    private void applyLook(Player player) {
        if (!Gdx.input.isCursorCatched()) {
            return;
        }
        float deltaYaw = Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
        float deltaPitch = -Gdx.input.getDeltaY() * MOUSE_SENSITIVITY;
        movementService.look(player, deltaYaw, deltaPitch);
    }

    private void applyMovement(Player player, World world, double deltaSeconds) {
        MovementInput input = new MovementInput(
                Gdx.input.isKeyPressed(Input.Keys.W),
                Gdx.input.isKeyPressed(Input.Keys.S),
                Gdx.input.isKeyPressed(Input.Keys.A),
                Gdx.input.isKeyPressed(Input.Keys.D),
                Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        updateMovement(player, world, input,
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE), deltaSeconds);
    }

    /** Device-independent movement step; Shift is intent, stamina authorizes the speed. */
    public void updateMovement(Player player, World world, MovementInput intent,
                               boolean jumpRequested, double rawDeltaSeconds) {
        double deltaSeconds = checkedDelta(rawDeltaSeconds);
        MovementInput walk = new MovementInput(intent.isForward(), intent.isBackward(),
                intent.isLeft(), intent.isRight());
        double[] horizontal = movementService.computeIntendedDelta(player, walk, deltaSeconds);
        double sprintSeconds = intent.isSprint() ? stamina.sprintSeconds(deltaSeconds) : 0;
        if (sprintSeconds > 0) {
            double[] extra = movementService.computeIntendedDelta(player, walk,
                    sprintSeconds * (PlayerMovementService.SPRINT_MULTIPLIER - 1));
            horizontal[0] += extra[0];
            horizontal[1] += extra[1];
        }
        if (jumpRequested) {
            physics.jump(player);
        }
        physics.applyGravity(player, deltaSeconds);
        double verticalDelta = physics.computeIntendedDeltaY(player, deltaSeconds);
        double previousX = player.getX();
        double previousZ = player.getZ();
        collisionResolver.resolveAndApply(player, world, horizontal[0], verticalDelta, horizontal[1]);
        double displacement = Math.hypot(player.getX() - previousX, player.getZ() - previousZ);
        double intended = Math.hypot(horizontal[0], horizontal[1]);
        moving = displacement > 1e-9;
        double appliedFraction = intended > 1e-9 ? Math.min(1, displacement / intended) : 0;
        sprinting = moving && sprintSeconds > 0;
        stamina.advance(deltaSeconds, sprintSeconds * appliedFraction);
    }

    private static double checkedDelta(double rawDeltaSeconds) {
        if (!Double.isFinite(rawDeltaSeconds) || rawDeltaSeconds < 0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and nonnegative");
        }
        return Math.min(rawDeltaSeconds, MAX_DELTA_SECONDS);
    }

    private void applyInteraction(Player player, World world) {
        if (!Gdx.input.isCursorCatched()) {
            return;
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Primero el zombi bajo la mira y en alcance; si no hay ninguno, el clic sigue picando.
            if (pistol != null && pistol.equipped()) {
                pistol.shoot(player, enemyService.zombies());
            } else if (!meleeService.strike(player, enemyService.zombies())) {
                interactionService.removeTargetedBlock(player, world);
            }
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            interactionService.placeBlockOfType(player, world, selectedType);
        }
    }

    private void updateSelectedType() {
        for (int slot = 0; slot < PLACEABLE.length; slot++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + slot)) {
                selectedType = PLACEABLE[slot];
                if (pistol != null) pistol.unequip();
                return;
            }
        }
    }
}
