package application;

import domain.enemy.AStarPathfinder;
import domain.enemy.NavigationGrid;
import domain.enemy.NavigationNode;
import domain.enemy.Path;
import domain.enemy.Zombie;
import domain.enemy.ZombieMovement;
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

    private final World world;
    private final ZombieParameters parameters;
    private final NavigationGrid grid;
    private final ZombieStateMachine stateMachine;
    private final AStarPathfinder pathfinder;
    private final ZombieMovement movement = new ZombieMovement();
    private final List<Zombie> zombies = new ArrayList<>();
    private final Map<Zombie, Navigation> navigation = new HashMap<>();
    private boolean enabled = true;

    public EnemyUpdateService(World world, ZombieParameters parameters) {
        this(world, parameters, new AStarPathfinder());
    }

    public EnemyUpdateService(World world, ZombieParameters parameters, AStarPathfinder pathfinder) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.parameters = Objects.requireNonNull(parameters, "parameters no puede ser null");
        this.pathfinder = Objects.requireNonNull(pathfinder, "pathfinder no puede ser null");
        this.grid = new NavigationGrid(world);
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
        return grid.nodeUnder(x, Chunk.HEIGHT - 1, z).map(node -> {
            Zombie zombie = new Zombie(x, node.feetY(), z, parameters.maxHealth());
            register(zombie);
            return zombie;
        });
    }

    /** Un fotograma de IA. Con enemigos desactivados o jugador muerto nada se mueve ni ataca. */
    public void update(PlayerLife life, double deltaSeconds) {
        Objects.requireNonNull(life, "life no puede ser null");
        if (!enabled || life.isDead() || deltaSeconds <= 0) {
            return;
        }
        Player player = world.getPlayer();
        Iterator<Zombie> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            Zombie zombie = iterator.next();
            ZombiePerception perception = new ZombiePerception(zombie.getHealth(),
                    zombie.distanceTo(player.getX(), player.getY(), player.getZ()), !life.isDead());
            zombie.setState(stateMachine.next(zombie.getState(), perception));
            Navigation nav = navigation.get(zombie);

            switch (zombie.getState()) {
                case IDLE -> nav.clear();
                case CHASE -> chase(zombie, nav, player, deltaSeconds);
                case ATTACK -> attack(zombie, nav, life, deltaSeconds);
                case DEAD -> {
                    nav.clear();
                    zombie.addDeadTime(deltaSeconds);
                    if (zombie.getDeadSeconds() >= DESPAWN_SECONDS) {
                        iterator.remove();
                        navigation.remove(zombie);
                    }
                }
            }
        }
    }

    private void chase(Zombie zombie, Navigation nav, Player player, double deltaSeconds) {
        nav.sinceRepath += deltaSeconds;
        nav.noRouteWait -= deltaSeconds;
        if (nav.needsRepath(player, parameters) && nav.noRouteWait <= 0) {
            replan(zombie, nav, player);
        }
        if (nav.path != null && !movement.follow(zombie, nav.path, grid, parameters.moveSpeed(), deltaSeconds)) {
            // Un bloque nuevo cerró el paso: la ruta ya no vale y el próximo fotograma replanifica.
            nav.path = null;
        }
    }

    private void replan(Zombie zombie, Navigation nav, Player player) {
        nav.sinceRepath = 0;
        nav.path = null;
        Optional<NavigationNode> start = grid.nodeUnder(zombie.getX(), zombie.getY(), zombie.getZ());
        Optional<NavigationNode> goal = grid.nodeUnder(player.getX(), player.getY(), player.getZ());
        Optional<Path> path = start.isPresent() && goal.isPresent()
                ? pathfinder.findPath(grid, start.get(), goal.get())
                : Optional.empty();
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
        nav.clear();
        zombie.addAttackTime(deltaSeconds);
        if (zombie.getAttackTimer() >= parameters.attackCooldownSeconds()) {
            zombie.resetAttackTimer();
            life.die(PlayerLife.DeathCause.ENEMY);
        }
    }

    /** Estado de navegación por zombi; vive solo mientras el zombi está registrado. */
    private static final class Navigation {
        private Path path;
        private NavigationNode goal;
        private double sinceRepath;
        private double noRouteWait;

        void clear() {
            path = null;
            goal = null;
            sinceRepath = 0;
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
