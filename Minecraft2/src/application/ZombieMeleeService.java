package application;

import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;
import java.util.Collection;
import java.util.Optional;

/** Melee and pistol share the same body ray and terrain occlusion rules. */
public final class ZombieMeleeService {
    public static final double REACH = 4.0;
    public static final int DAMAGE = 1;
    private final ZombieTargeting targeting;

    public ZombieMeleeService(World world) { targeting = new ZombieTargeting(world); }

    public boolean strike(Player player, Collection<Zombie> zombies) {
        Optional<Zombie> target = findTarget(player, zombies);
        target.ifPresent(zombie -> zombie.takeDamage(DAMAGE));
        return target.isPresent();
    }

    public Optional<Zombie> findTarget(Player player, Collection<Zombie> zombies) {
        return targeting.findTarget(player, zombies, REACH);
    }
}
