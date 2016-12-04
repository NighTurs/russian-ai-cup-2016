import model.*;

import java.util.Optional;

import static java.lang.StrictMath.hypot;

public class DodgeProjectileTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        WizardProxy self = turnContainer.getSelf();
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
                            self.getRadius() + projectile.getRadius() + self.getWizardForwardSpeed(game)) {
                continue;
            }

            int ticksLeft = (int) Math.ceil(
                    projectile.getDistanceTo(travelsTo.getX(), travelsTo.getY()) / game.getMagicMissileSpeed());
            Optional<Movement> dodgeDirection = tryDodgeDirections(self, projectile, travelsTo, ticksLeft, game, world);
            if (dodgeDirection.isPresent()) {
                Movement mov = dodgeDirection.get();
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

    private Optional<Movement> tryDodgeDirections(WizardProxy wizard,
                                                  Projectile projectile,
                                                  Point travelsTo,
                                                  int ticksLeft,
                                                  Game game,
                                                  WorldProxy world) {
        double cos = Math.cos(wizard.getAngle());
        double sin = Math.sin(wizard.getAngle());

        for (double speedOffset = 0.0; speedOffset <= 1.0; speedOffset += 0.1) {
            for (int speedSign = -1; speedSign <= 1; speedSign += 2) {
                for (int strafeSign = -1; strafeSign <= 1; strafeSign += 2) {
                    double strafeOffset = Math.sqrt(1 - speedOffset * speedOffset);
                    double speed;
                    if (speedSign == -1) {
                        speed = -wizard.getWizardBackwardSpeed(game) * ticksLeft * speedOffset;
                    } else {
                        speed = wizard.getWizardForwardSpeed(game) * ticksLeft * speedOffset;
                    }
                    double strafe = strafeSign * wizard.getWizardStrafeSpeed(game) * ticksLeft * strafeOffset;
                    double resX = wizard.getX() + speed * cos - strafe * sin;
                    double resY = wizard.getY() + speed * sin + strafe * cos;
                    boolean foundIntersection = false;
                    if (resX < wizard.getRadius() || resY < wizard.getRadius() ||
                            resX > world.getWidth() - wizard.getRadius() ||
                            resY > world.getHeight() - wizard.getRadius()) {
                        continue;
                    }
                    if (MathMethods.isLineIntersectsCircle(projectile.getX(),
                            travelsTo.getX(),
                            projectile.getY(),
                            travelsTo.getY(),
                            resX,
                            resY,
                            wizard.getRadius() + projectile.getRadius()) ||
                            hypot(travelsTo.getX() - resX, travelsTo.getY() - resY) <=
                                    wizard.getRadius() + projectile.getRadius()) {
                        continue;
                    }
                    for (Unit unit : world.getAllUnitsNearby()) {
                        if (unit.getId() == wizard.getId()) {
                            continue;
                        }
                        if (unit instanceof Projectile || unit instanceof Bonus) {
                            continue;
                        }
                        double unitR = ((CircularUnit) unit).getRadius();
                        if (unit.getDistanceTo(resX, resY) < wizard.getRadius() + unitR) {
                            foundIntersection = true;
                            break;
                        }
                        if (MathMethods.isLineIntersectsCircle(wizard.getX(),
                                resX,
                                wizard.getY(),
                                resY,
                                unit.getX(),
                                unit.getY(),
                                wizard.getRadius() + ((CircularUnit) unit).getRadius())) {
                            foundIntersection = true;
                            break;
                        }
                    }
                    if (foundIntersection) {
                        continue;
                    }
                    return Optional.of(new Movement(speed / ticksLeft, strafe / ticksLeft, 0));
                }
            }
        }
        return Optional.empty();
    }
}
