import model.Game;
import model.Projectile;
import model.ProjectileType;
import model.Wizard;

import java.util.Optional;

public class DodgeProjectileTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        Wizard self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        ProjectileControl projectileControl = turnContainer.getProjectileControl();

        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getType() != ProjectileType.MAGIC_MISSILE || projectile.getFaction() == self.getFaction()) {
                continue;
            }
            double dist = self.getDistanceTo(projectile);
            if (dist > game.getWizardVisionRange()) {
                continue;
            }
            Optional<ProjectileControl.ProjectileMeta> metaOpt = projectileControl.projectileMeta(projectile);
            if (!metaOpt.isPresent()) {
                continue;
            }
            ProjectileControl.ProjectileMeta meta = metaOpt.get();
            Point travelsTo = MathMethods.distPoint(meta.getInitialPoint().getX(),
                    meta.getInitialPoint().getY(),
                    projectile.getX(),
                    projectile.getY(),
                    meta.getRange());

            if (!MathMethods.isLineIntersectsCircle(projectile.getX(),
                    travelsTo.getX(),
                    projectile.getY(),
                    travelsTo.getY(),
                    self.getX(),
                    self.getY(),
                    self.getRadius() + projectile.getRadius()) &&
                    self.getDistanceTo(travelsTo.getX(), travelsTo.getY()) >
                            self.getRadius() + projectile.getRadius()) {
                continue;
            }

            int ticksLeft = (int) Math.ceil(
                    projectile.getDistanceTo(travelsTo.getX(), travelsTo.getY()) / game.getMagicMissileSpeed());
            if (meta.getRange() + self.getRadius() + projectile.getRadius() <
                    self.getDistanceTo(meta.getInitialPoint().getX(), meta.getInitialPoint().getY()) +
                            ticksLeft * WizardTraits.getWizardBackwardSpeed(self, game)) {
                Point retreatTo = MathMethods.distPoint(meta.getInitialPoint().getX(),
                        meta.getInitialPoint().getY(),
                        projectile.getX(),
                        projectile.getY(),
                        meta.getRange() + self.getRadius() + projectile.getRadius() + 1);
                Movement mov = turnContainer.getPathFinder()
                        .findOptimalMovement(self, retreatTo.getX(), retreatTo.getY());
                MoveBuilder moveBuilder = new MoveBuilder();
                moveBuilder.setSpeed(mov.getSpeed());
                moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
                return Optional.of(new TacticImpl("DodgeProjectile",
                        moveBuilder,
                        Tactics.DODGE_PROJECTILE_TACTIC_PRIORITY));
            }
        }
        return Optional.empty();
    }
}
