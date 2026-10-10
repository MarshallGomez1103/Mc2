package application;

import domain.block.BlockType;
import domain.player.CollisionResolver;
import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerMovementService;
import domain.player.PlayerPhysics;
import domain.player.Stamina;
import domain.world.World;
import java.util.Objects;

/** Coordina control, física e interacción; las reglas permanecen en los servicios existentes. */
public final class PlayerControlService {
    public static final double MAX_DELTA_SECONDS = 0.05;
    private final World world;
    private final PlayerMovementService movementService = new PlayerMovementService();
    private final PlayerPhysics physics = new PlayerPhysics();
    private final CollisionResolver collisionResolver = new CollisionResolver();
    private final Stamina stamina = new Stamina();
    private final PlayerInteractionService interactionService;
    private final ZombieMeleeService meleeService;
    private final EnemyUpdateService enemyService;
    private final PistolService pistol;
    private boolean sprinting;
    private boolean moving;
    private BlockType selectedType = BlockType.STONE;

    public PlayerControlService(World world, PlayerInteractionService interactionService,
                                EnemyUpdateService enemyService, PistolService pistol) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.interactionService = Objects.requireNonNull(interactionService);
        this.enemyService = Objects.requireNonNull(enemyService);
        this.pistol = Objects.requireNonNull(pistol);
        meleeService = new ZombieMeleeService(world);
    }

    /** Orden conservado: mirar, mover, alternar arma, seleccionar material y actuar. */
    public void update(double rawDeltaSeconds, PlayerFrameInput input) {
        double delta = checkedDelta(rawDeltaSeconds);
        Objects.requireNonNull(input, "input no puede ser null");
        Player player = world.getPlayer();
        movementService.look(player, input.deltaYaw(), input.deltaPitch());
        updateMovement(player, world, input.movement(), input.jumpRequested(), delta);
        if (input.togglePistol()) pistol.toggleEquipped();
        if (input.selectedType() != null) {
            selectedType = input.selectedType();
            pistol.unequip();
        }
        if (input.primaryAction()) {
            // El disparo nunca pica bloques; melee tiene prioridad sobre el bloque apuntado.
            if (pistol.equipped()) pistol.shoot(player, enemyService.zombies());
            else if (!meleeService.strike(player, enemyService.zombies())) {
                interactionService.removeTargetedBlock(player, world);
            }
        }
        if (input.secondaryAction()) interactionService.placeBlockOfType(player, world, selectedType);
    }

    /** El sprint consume energía según el desplazamiento realmente permitido por colisión. */
    void updateMovement(Player player, World world, MovementInput intent,
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

    public PlayerControlState state() {
        return new PlayerControlState(stamina.current(), stamina.maximum(), stamina.fraction(),
                stamina.exhausted(), sprinting, moving, selectedType);
    }

    /** Conserva el material seleccionado, igual que antes de la separación de capas. */
    public void resetAfterRespawn() {
        stamina.reset();
        sprinting = false;
        moving = false;
    }
}
