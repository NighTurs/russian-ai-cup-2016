import model.*;

import java.util.Optional;

import static java.lang.StrictMath.hypot;

public class DodgeProjectileTacticBuilder implements TacticBuilder {

    private static final int PROJECTILE_IGNORE_RANGE = 800;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        ProjectileControl projectileControl = turnContainer.getProjectileControl();

        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getType() == ProjectileType.DART ||
                    (projectile.getType() != ProjectileType.FIREBALL && projectile.getFaction() == self.getFaction())) {
                continue;
            }
            double dist = self.getDistanceTo(projectile);
            if (dist > PROJECTILE_IGNORE_RANGE) {
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

            final double projectileEffectiveRadius = projectileEffectiveRadius(game, projectile);
            if (!MathMethods.isLineIntersectsCircle(projectile.getX(),
                    travelsTo.getX(),
                    projectile.getY(),
                    travelsTo.getY(),
                    self.getX(),
                    self.getY(),
                    self.getRadius() + projectileEffectiveRadius + self.getWizardForwardSpeed(game)) &&
                    self.getDistanceTo(travelsTo.getX(), travelsTo.getY()) >
                            self.getRadius() + projectileEffectiveRadius + self.getWizardForwardSpeed(game)) {
                continue;
            }

            Optional<Movement> dodgeDirection = tryDodgeDirections(self, projectile, travelsTo, game, world);
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

    private double projectileEffectiveRadius(Game game, Projectile projectile) {
        return CastProjectileTacticBuilders.projectileEffectiveRadius(game, projectile.getType());
    }

    private double projectileMoveSpeed(Game game, Projectile projectile) {
        return CastProjectileTacticBuilders.projectileMoveSpeed(game, projectile.getType());
    }

    private Optional<Movement> tryDodgeDirections(WizardProxy wizard,
                                                  Projectile projectile,
                                                  Point travelsTo,
                                                  Game game,
                                                  WorldProxy world) {
        double projectileEffectiveRadius = projectile.getRadius();
        double projectileMoveSpeed = projectileMoveSpeed(game, projectile);

        double distLeft = projectile.getDistanceTo(travelsTo.getX(), travelsTo.getY());
        int maxTicks =
                (int) Math.ceil(distLeft / projectileMoveSpeed);

        double cos = Math.cos(wizard.getAngle());
        double sin = Math.sin(wizard.getAngle());

        double simProjX = projectile.getX();
        double simProjY = projectile.getY();
        int ticksPassed = 0;
        double maxWizardMoveSpeed = wizard.getWizardForwardSpeed(game);

        do {
            ticksPassed++;
            Point nextProjectilePoint =
                    MathMethods.distPoint(simProjX, simProjY, travelsTo.getX(), travelsTo.getY(), projectileMoveSpeed);
            simProjX = nextProjectilePoint.getX();
            simProjY = nextProjectilePoint.getY();
        } while (hypot(simProjX - wizard.getX(), simProjY - wizard.getY()) - maxWizardMoveSpeed * ticksPassed -
                wizard.getRadius() - projectileEffectiveRadius > 0);

        // first consider moving backwards as it is move safe
        for (double speedOffset = 1.0; speedOffset >= 0.0; speedOffset -= 0.1) {
            for (int speedSign = -1; speedSign <= 1; speedSign += 2) {
                for (int strafeSign = -1; strafeSign <= 1; strafeSign += 2) {
                    boolean collision = false;
                    double strafeOffset = Math.sqrt(1 - speedOffset * speedOffset);
                    double baseStrafe = strafeSign * wizard.getWizardStrafeSpeed(game) * strafeOffset;
                    double baseSpeed;
                    if (speedSign == -1) {
                        baseSpeed = -wizard.getWizardBackwardSpeed(game) * speedOffset;
                    } else {
                        baseSpeed = wizard.getWizardForwardSpeed(game) * speedOffset;
                    }

                    for (int ticksTotal = ticksPassed; ticksTotal <= maxTicks; ticksTotal++) {
                        Point nextProjectilePoint = MathMethods.distPoint(projectile.getX(),
                                projectile.getY(),
                                travelsTo.getX(),
                                travelsTo.getY(),
                                Math.min(distLeft, projectileMoveSpeed * ticksTotal));
                        simProjX = nextProjectilePoint.getX();
                        simProjY = nextProjectilePoint.getY();
                        double speed = baseSpeed * ticksTotal;
                        double strafe = baseStrafe * ticksTotal;
                        double resX = wizard.getX() + speed * cos - strafe * sin;
                        double resY = wizard.getY() + speed * sin + strafe * cos;

                        if (resX < wizard.getRadius() || resY < wizard.getRadius() ||
                                resX > world.getWidth() - wizard.getRadius() ||
                                resY > world.getHeight() - wizard.getRadius()) {
                            collision = true;
                            break;
                        }
                        if (MathMethods.isLineIntersectsCircle(projectile.getX(),
                                simProjX,
                                projectile.getY(),
                                simProjY,
                                resX,
                                resY,
                                wizard.getRadius() + projectileEffectiveRadius) ||
                                hypot(simProjX - resX, simProjY - resY) <=
                                        wizard.getRadius() + projectileEffectiveRadius) {
                            collision = true;
                            break;
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
                                collision = true;
                                break;
                            }
                            if (MathMethods.isLineIntersectsCircle(wizard.getX(),
                                    resX,
                                    wizard.getY(),
                                    resY,
                                    unit.getX(),
                                    unit.getY(),
                                    wizard.getRadius() + ((CircularUnit) unit).getRadius())) {
                                collision = true;
                                break;
                            }
                        }
                        if (collision) {
                            break;
                        }
                    }
                    if (!collision) {
                        return Optional.of(new Movement(baseSpeed, baseStrafe, 0));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
