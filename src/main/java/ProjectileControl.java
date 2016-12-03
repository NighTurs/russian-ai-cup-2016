import model.Game;
import model.Projectile;
import model.ProjectileType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ProjectileControl {

    private final Memory memory;

    public ProjectileControl(WorldProxy world, Game game, Memory memory) {
        this.memory = memory;
        Set<Long> dissapearedProjectiles = new HashSet<>(memory.getProjectileMeta().keySet());
        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getType() != ProjectileType.MAGIC_MISSILE) {
                continue;
            }
            dissapearedProjectiles.remove(projectile.getId());
            if (memory.getProjectileMeta().containsKey(projectile.getId())) {
                continue;
            }
            for (WizardProxy wizard : world.getWizards()) {
                if (wizard.getOwnerPlayerId() == projectile.getOwnerPlayerId()) {
                    Point p = memory.getWizardPreviousPosition().get(wizard.getId());
                    if (p != null) {
                        memory.getProjectileMeta()
                                .put(projectile.getId(), new ProjectileMeta(p, wizard.getCastRange()));
                    }
                }
            }
        }
        for (WizardProxy wizard : world.getWizards()) {
            memory.getWizardPreviousPosition().put(wizard.getId(), new Point(wizard.getX(), wizard.getY()));
        }
        dissapearedProjectiles.forEach(x -> memory.getProjectileMeta().remove(x));
    }

    public Optional<ProjectileMeta> projectileMeta(Projectile projectile) {
        return Optional.ofNullable(memory.getProjectileMeta().get(projectile.getId()));
    }

    public static class ProjectileMeta {

        private final Point initialPoint;
        private final double range;

        public ProjectileMeta(Point initialPoint, double range) {
            this.initialPoint = initialPoint;
            this.range = range;
        }

        public Point getInitialPoint() {
            return initialPoint;
        }

        public double getRange() {
            return range;
        }
    }
}
