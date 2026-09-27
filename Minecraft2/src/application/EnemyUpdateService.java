package application;

import domain.enemy.AStarPathfinder;
import domain.enemy.NavigationGrid;
import domain.enemy.NavigationNode;
import domain.enemy.Path;
import domain.enemy.Zombie;
import domain.enemy.ZombieMovement;
import domain.enemy.ZombiePhysics;
import domain.enemy.KamikazePolicy;
import domain.enemy.KamikazeDescent;
import domain.enemy.ZombieSeparation;
import domain.enemy.ZombieParameters;
import domain.enemy.ZombiePerception;
import domain.enemy.ZombieState;
import domain.enemy.ZombieStateMachine;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.Chunk;
import domain.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * Coordina por fotograma la IA de todos los zombis de la sesión: percepción, FSM, navegación,
 * movimiento, ataque y despawn. La FSM decide qué hacer y A* decide por dónde; este servicio es
 * el único que llama a ambos. No dibuja ni lee teclado.
 *
 * <p>Política de repath: solo se recalcula una ruta cuando no hay ruta, la ruta terminó o quedó
 * bloqueada, el jugador se alejó del destino planificado o venció el intervalo. Tras un A* sin
 * ruta se espera {@code noRouteRetrySeconds} antes de reintentar.
 *
 * <p>Contrato propuesto para Estudiante 3: {@link #register(Zombie)} y {@link #spawnAt(double, double)}
 * son la entrada de HordeManager; {@link #setEnabled(boolean)} es el interruptor Enemigos ON/OFF.
 * Los zombis desaparecen de {@link #zombies()} {@link #DESPAWN_SECONDS} después de morir.
 */
public final class EnemyUpdateService {
    public static final double DESPAWN_SECONDS = 0.8;
    public static final int ATTACK_DAMAGE = 25;

    private final World world;
    private final ZombieParameters parameters;
    private final NavigationGrid grid;
    private final ZombieStateMachine stateMachine;
    private final AStarPathfinder pathfinder;
    private final ZombieMovement movement;
    private final NavigationGrid crawlerGrid;
    private final KamikazeDescent descent = new KamikazeDescent();
    private final Map<String, DropGate> dropGates = new HashMap<>();
    private final ZombieSeparation separation = new ZombieSeparation();
    private final List<Zombie> zombies = new ArrayList<>();
    private final Map<Zombie, Navigation> navigation = new HashMap<>();
    private boolean enabled = true;

    public EnemyUpdateService(World world, ZombieParameters parameters) {
        this(world, parameters, new AStarPathfinder());
    }

    public EnemyUpdateService(World world, ZombieParameters parameters, AStarPathfinder pathfinder) {
        this(world, parameters, pathfinder, new Random(world.getSeed() ^ 0x4b414d494b415a45L)::nextDouble);
    }

    /** Injectable landing RNG for deterministic fall/crawler regression tests. */
    public EnemyUpdateService(World world, ZombieParameters parameters, AStarPathfinder pathfinder,
                              DoubleSupplier landingRandom) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.parameters = Objects.requireNonNull(parameters, "parameters no puede ser null");
        this.pathfinder = Objects.requireNonNull(pathfinder, "pathfinder no puede ser null");
        this.grid = new NavigationGrid(world);
        this.crawlerGrid = new NavigationGrid(world, 1, 0);
        this.movement = new ZombieMovement(new ZombiePhysics(new KamikazePolicy(landingRandom)));
        this.stateMachine = ZombieStateMachine.from(parameters);
    }

    public ZombieParameters parameters() {
        return parameters;
    }

    public AStarPathfinder pathfinder() {
        return pathfinder;
    }

    public NavigationGrid grid() {
        return grid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Zombie> zombies() {
        return Collections.unmodifiableList(zombies);
    }

    public void register(Zombie zombie) {
        zombies.add(Objects.requireNonNull(zombie, "zombie no puede ser null"));
        navigation.put(zombie, new Navigation());
    }

    /** Crea un zombi con los pies sobre el primer apoyo caminable de la columna, si existe. */
    public Optional<Zombie> spawnAt(double x, double z) {
        return grid.nodeUnder(x, Chunk.HEIGHT - 1, z).flatMap(node -> {
            Zombie zombie = new Zombie(x, node.feetY(), z, parameters.maxHealth());
            if (!movement.tryMove(zombie, grid, x, z) || separation.isOccupied(zombie, zombies)) {
                return Optional.empty();
            }
            register(zombie);
            return Optional.of(zombie);
        });
    }

    /** Un fotograma de IA. Con enemigos desactivados o jugador muerto nada se mueve ni ataca. */
    public void update(PlayerLife life, double deltaSeconds) {
        Objects.requireNonNull(life, "life no puede ser null");
        if (life.isDead() || deltaSeconds <= 0) return;
        zombies.forEach(zombie -> zombie.advanceFlash(deltaSeconds));
        if (!enabled) return;
        Player player = world.getPlayer();
        Iterator<Zombie> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            Zombie zombie = iterator.next();
            double previousY = zombie.getY();
            ZombiePerception perception = new ZombiePerception(zombie.getHealth(),
                    perceptionDistance(zombie, player), !life.isDead());
            zombie.setState(stateMachine.next(zombie.getState(), perception));
            Navigation nav = navigation.get(zombie);
            nav.attackCooldownRemaining = Math.max(0, nav.attackCooldownRemaining - deltaSeconds);

            switch (zombie.getState()) {
                case IDLE -> { nav.clear(); movement.advancePhysics(zombie, grid, deltaSeconds); }
                case CHASE -> chase(zombie, nav, player, deltaSeconds);
                case ATTACK -> attack(zombie, nav, life, deltaSeconds);
                case DEAD -> {
                    nav.clear();
                    zombie.advanceMotionTimers(deltaSeconds);
                    zombie.addDeadTime(deltaSeconds);
                    if (zombie.getDeadSeconds() >= DESPAWN_SECONDS) {
                        iterator.remove();
                        navigation.remove(zombie);
                    }
                }
            }
            resolveFallingContact(zombie, previousY);
        }
        separation.resolve(zombies, grid);
        dropGates.values().forEach(gate -> gate.remaining = Math.max(0, gate.remaining - deltaSeconds));
        dropGates.values().removeIf(gate -> gate.remaining <= 0);
    }

    private void resolveFallingContact(Zombie falling, double previousY) {
        if (!falling.isAlive() || falling.getY() >= previousY - 1e-6) return;
        for (Zombie other : zombies) {
            if (other == falling || !other.isAlive()) continue;
            double top = other.getY() + other.height();
            if (previousY >= top - 1e-6 && falling.getY() < top
                    && Math.abs(falling.getX() - other.getX()) < Zombie.WIDTH
                    && Math.abs(falling.getZ() - other.getZ()) < Zombie.WIDTH) {
                // The queue normally prevents this contact; an impact is never a walkable head.
                falling.setPosition(falling.getX(), top, falling.getZ());
                falling.shatter();
                return;
            }
        }
    }

    private void chase(Zombie zombie, Navigation nav, Player player, double deltaSeconds) {
        NavigationGrid bodyGrid = zombie.isCrawler() ? crawlerGrid : grid;
        nav.sinceRepath += deltaSeconds;
        nav.noRouteWait -= deltaSeconds;
        if (nav.dropTarget != null && nav.path != null && nav.path.isFinished()) {
            enterDrop(zombie, nav, bodyGrid, deltaSeconds);
            return;
        }
        // Remember the entrance while approaching it, even if the player moves deeper in the cave.
        if ((nav.dropTarget == null || nav.path == null)
                && nav.needsRepath(player, parameters) && nav.noRouteWait <= 0) {
            replan(zombie, nav, player);
        }
        if (nav.path != null) {
            double x = zombie.getX(), y = zombie.getY(), z = zombie.getZ();
            if (!movement.follow(zombie, nav.path, bodyGrid, parameters.moveSpeed(), deltaSeconds)) {
                nav.path = null;
                nav.noRouteWait = parameters.noRouteRetrySeconds();
            } else if (separation.isOccupied(zombie, zombies)) {
                // Crowds wait locally; they do not trigger A* retries or pass through each other.
                zombie.setPosition(x, zombie.getY(), z);
            }
        } else {
            movement.advancePhysics(zombie, bodyGrid, deltaSeconds);
        }
    }

    private void enterDrop(Zombie zombie, Navigation nav, NavigationGrid bodyGrid, double seconds) {
        String key = nav.dropTarget.x() + ":" + nav.dropTarget.z();
        DropGate gate = dropGates.get(key);
        boolean entranceOccupied = zombies.stream().anyMatch(other -> other != zombie && other.isAlive()
                && other.getY() < nav.dropEntryY - 0.1
                && other.getY() + other.height() > nav.dropEntryY - 2.5
                && Math.hypot(other.getX() - nav.dropTarget.centerX(),
                other.getZ() - nav.dropTarget.centerZ()) < Zombie.WIDTH + 0.1);
        if (!nav.dropCommitted && (entranceOccupied || (gate != null && gate.remaining > 0))) {
            movement.advancePhysics(zombie, bodyGrid, seconds);
            return;
        }
        if (!nav.dropCommitted) {
            nav.dropCommitted = true;
            zombie.markKamikaze();
            dropGates.put(key, new DropGate(0.65));
        }
        movement.toward(zombie, bodyGrid, nav.dropTarget.centerX(), nav.dropTarget.centerZ(),
                parameters.moveSpeed(), seconds);
        if (!zombie.isAlive() || (zombie.isOnGround() && zombie.getY() < nav.dropEntryY - 0.5)) {
            nav.clear();
            nav.noRouteWait = 0;
        }
    }

    private void replan(Zombie zombie, Navigation nav, Player player) {
        nav.sinceRepath = 0;
        nav.path = null;
        nav.dropTarget = null;
        nav.dropCommitted = false;
        NavigationGrid bodyGrid = zombie.isCrawler() ? crawlerGrid : grid;
        Optional<NavigationNode> start = bodyGrid.nodeUnder(zombie.getX(), zombie.getY(), zombie.getZ());
        Optional<NavigationNode> goal = bodyGrid.nodeUnder(player.getX(), player.getY(), player.getZ());
        Optional<Path> path = start.isPresent() && goal.isPresent()
                ? pathfinder.findPath(bodyGrid, start.get(), goal.get())
                : Optional.empty();
        if (path.isEmpty() && start.isPresent() && goal.isPresent()) {
            Optional<KamikazeDescent.Descent> drop = descent.find(bodyGrid, start.get(), goal.get());
            if (drop.isPresent()) {
                path = Optional.of(drop.get().approach());
                nav.dropTarget = drop.get().bottom();
                nav.dropEntryY = drop.get().entryY();
            }
        }
        if (path.isEmpty()) {
            nav.noRouteWait = parameters.noRouteRetrySeconds();
            return;
        }
        nav.path = path.get();
        nav.goal = goal.get();
        if (nav.path.size() > 1) {
            nav.path.advance(); // el primer nodo es la columna donde ya está parado
        }
    }

    private void attack(Zombie zombie, Navigation nav, PlayerLife life, double deltaSeconds) {
        if (!hasClearMeleeReach(zombie, world.getPlayer())) {
            // Close distance through a wall still requires a walkable route around it.
            chase(zombie, nav, world.getPlayer(), deltaSeconds);
            return;
        }
        movement.advancePhysics(zombie, zombie.isCrawler() ? crawlerGrid : grid, deltaSeconds);
        if (!zombie.isAlive() || !hasClearMeleeReach(zombie, world.getPlayer())) return;
        nav.clear();
        if (nav.attackCooldownRemaining <= 1e-9) {
            life.takeDamage(ATTACK_DAMAGE, zombie.isKamikaze() ? PlayerLife.DeathCause.KAMIKAZE : PlayerLife.DeathCause.ENEMY);
            nav.attackCooldownRemaining = parameters.attackCooldownSeconds();
        }
    }

    /** Melee uses overlapping body heights rather than the distance between feet. */
    private double perceptionDistance(Zombie zombie, Player player) {
        double horizontal = Math.hypot(player.getX() - zombie.getX(), player.getZ() - zombie.getZ());
        double overlap = Math.min(zombie.getY() + zombie.height(), player.getY() + Player.HEIGHT)
                - Math.max(zombie.getY(), player.getY());
        double distance = overlap > 0 && horizontal <= parameters.attackRange()
                ? horizontal : zombie.distanceTo(player.getX(), player.getY(), player.getZ());
        return distance;
    }

    /** Arms reach above a low block, while tall solid walls still interrupt melee. */
    private boolean hasClearMeleeReach(Zombie zombie, Player player) {
        double dx = player.getX() - zombie.getX();
        double reachHeight = Math.min(zombie.getY() + zombie.height(),
                player.getY() + Player.HEIGHT) - 0.2;
        if (reachHeight <= Math.max(zombie.getY(), player.getY())) return false;
        double dz = player.getZ() - zombie.getZ();
        int samples = Math.max(1, (int) Math.ceil(Math.hypot(dx, dz) / 0.05));
        for (int step = 0; step <= samples; step++) {
            double fraction = (double) step / samples;
            int x = (int) Math.floor(zombie.getX() + dx * fraction);
            int y = (int) Math.floor(reachHeight);
            int z = (int) Math.floor(zombie.getZ() + dz * fraction);
            if (!grid.chunkExists(x, z) || grid.isSolid(x, y, z)) return false;
        }
        return true;
    }

    private static final class DropGate {
        private double remaining;
        DropGate(double remaining) { this.remaining = remaining; }
    }

    /** Estado de navegación por zombi; vive solo mientras el zombi está registrado. */
    private static final class Navigation {
        private Path path;
        private NavigationNode goal;
        private double sinceRepath;
        private double noRouteWait;
        private double attackCooldownRemaining;
        private NavigationNode dropTarget;
        private double dropEntryY;
        private boolean dropCommitted;

        void clear() {
            path = null;
            goal = null;
            sinceRepath = 0;
            dropTarget = null;
            dropCommitted = false;
        }

        boolean needsRepath(Player player, ZombieParameters parameters) {
            if (path == null || path.isFinished()) {
                return true;
            }
            if (sinceRepath >= parameters.repathIntervalSeconds()) {
                return true;
            }
            double movedX = player.getX() - goal.centerX();
            double movedZ = player.getZ() - goal.centerZ();
            return Math.hypot(movedX, movedZ) > parameters.repathDistance();
        }
    }
}
