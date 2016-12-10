import model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.StrictMath.hypot;

public class DodgeProjectileTacticBuilder implements TacticBuilder {

    private static final int ANGLES_TO_TEST = 40;
    private static final double ANGLE_STEP = Math.PI * 2 / ANGLES_TO_TEST;
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

            List<DodgeOption> dodgeOptions = tryDodgeDirections(new Point(self.getX(), self.getY()),
                    self.getId(),
                    self.getAngle(),
                    self.getWizardForwardSpeed(game),
                    self.getWizardBackwardSpeed(game),
                    self.getWizardStrafeSpeed(game),
                    self.getWizardMaxTurnAngle(game),
                    new Point(projectile.getX(), projectile.getY()),
                    projectile.getType(),
                    travelsTo,
                    true,
                    self.getRadius() + projectile.getRadius(),
                    game,
                    world);
            if (!dodgeOptions.isEmpty()) {
                dodgeOptions.sort(Comparator.comparingDouble(projectile.getType() == ProjectileType.FIREBALL ?
                        DodgeOption::getDistToProjectile :
                        DodgeOption::getDistToProjectileInit));
                Movement mov = dodgeOptions.get(dodgeOptions.size() - 1).getMove();
                MoveBuilder moveBuilder = new MoveBuilder();
                moveBuilder.setSpeed(mov.getSpeed());
                moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
                moveBuilder.setTurn(mov.getTurn());
                return Optional.of(new TacticImpl("DodgeProjectile",
                        moveBuilder,
                        Tactics.DODGE_PROJECTILE_TACTIC_PRIORITY));
            }
        }
        return Optional.empty();
    }

    private static double projectileEffectiveRadius(Game game, Projectile projectile) {
        return CastProjectileTacticBuilders.projectileEffectiveRadius(game, projectile.getType());
    }

    public static List<DodgeOption> tryDodgeDirections(Point wizardPoint,
                                                       long wizardId,
                                                       double wizardAngle,
                                                       double wizardForwardSpeed,
                                                       double wizardBackwardSpeed,
                                                       double wizardStrafeSpeed,
                                                       double wizardMaxTurnAngle,
                                                       Point projectilePoint,
                                                       ProjectileType projectileType,
                                                       Point travelsTo,
                                                       boolean considerOutsideWorld,
                                                       // nullable
                                                       Double exclusionRadius,
                                                       Game game,
                                                       WorldProxy world) {
        double projectileEffectiveRadius = CastProjectileTacticBuilders.projectileRadius(game, projectileType);
        double projectileMoveSpeed = CastProjectileTacticBuilders.projectileMoveSpeed(game, projectileType);

        double distLeft = hypot(projectilePoint.getX() - travelsTo.getX(), projectilePoint.getY() - travelsTo.getY());
        int maxTicks = (int) Math.ceil(distLeft / projectileMoveSpeed);

        double simProjX = projectilePoint.getX();
        double simProjY = projectilePoint.getY();
        int ticksPassed = 0;
        @SuppressWarnings("UnnecessaryLocalVariable")
        double maxWizardMoveSpeed = wizardForwardSpeed;

        do {
            ticksPassed++;
            Point nextProjectilePoint =
                    MathMethods.distPoint(simProjX, simProjY, travelsTo.getX(), travelsTo.getY(), projectileMoveSpeed);
            simProjX = nextProjectilePoint.getX();
            simProjY = nextProjectilePoint.getY();
        } while (
                hypot(simProjX - wizardPoint.getX(), simProjY - wizardPoint.getY()) - maxWizardMoveSpeed * ticksPassed -
                        game.getWizardRadius() - projectileEffectiveRadius > 0);

        List<DodgeOption> results = new ArrayList<>();

        int directionId = 0;
        for (double angle = -Math.PI; angle <= Math.PI; angle += ANGLE_STEP) {
            directionId++;
            boolean collision = false;
            double distToProjectile = Double.MAX_VALUE;
            double distToProjectileInit = Double.MAX_VALUE;
            double resX = wizardPoint.getX();
            double resY = wizardPoint.getY();
            double resAngle = wizardAngle;
            double baseSpeed = 0;
            double baseStrafe = 0;
            double baseTurn = 0;

            for (int ticksTotal = 1; ticksTotal <= maxTicks; ticksTotal++) {

                double diffAngle = angle - resAngle;
                if (Math.abs(diffAngle) > Math.PI) {
                    diffAngle = (diffAngle > 0 ? -1 : 1) * (2 * Math.PI - Math.abs(diffAngle));
                }
                boolean backwards = Math.abs(diffAngle) > Math.PI / 2;
                double maxSpeed = backwards ? wizardBackwardSpeed : wizardForwardSpeed;
                @SuppressWarnings("UnnecessaryLocalVariable")
                double maxStrafe = wizardStrafeSpeed;
                double speed = (backwards ? -1 : 1) * maxSpeed * maxStrafe / Math.sqrt(
                        maxStrafe * maxStrafe + maxSpeed * maxSpeed * Math.tan(diffAngle) * Math.tan(diffAngle));
                double strafe = (diffAngle > 0 ? 1 : -1) * maxSpeed * maxStrafe / Math.sqrt(
                        maxSpeed * maxSpeed + maxStrafe * maxStrafe / (Math.tan(diffAngle) * Math.tan(diffAngle)));
                resX += speed * Math.cos(resAngle) - strafe * Math.sin(resAngle);
                resY += speed * Math.sin(resAngle) + strafe * Math.cos(resAngle);
                double turn = (diffAngle > 0 ? 1 : -1) * Math.min(Math.abs(diffAngle), wizardMaxTurnAngle);
                resAngle += turn;
                if (ticksTotal == 1) {
                    baseSpeed = speed;
                    baseStrafe = strafe;
                    baseTurn = turn;
                }

                Point nextProjectilePoint = MathMethods.distPoint(projectilePoint.getX(),
                        projectilePoint.getY(),
                        travelsTo.getX(),
                        travelsTo.getY(),
                        Math.min(distLeft, projectileMoveSpeed * ticksTotal));
                simProjX = nextProjectilePoint.getX();
                simProjY = nextProjectilePoint.getY();

                if (resX < game.getWizardRadius() || resY < game.getWizardRadius() ||
                        resX > world.getWidth() - game.getWizardRadius() ||
                        resY > world.getHeight() - game.getWizardRadius()) {
                    collision = true;
                    break;
                }

                Point trajInter = MathMethods.lineCircleIntersection(projectilePoint.getX(),
                        simProjX,
                        projectilePoint.getY(),
                        simProjY,
                        resX,
                        resY);
                if (MathMethods.isBetween(trajInter.getX(), projectilePoint.getX(), simProjX) &&
                        MathMethods.isBetween(trajInter.getY(), projectilePoint.getY(), simProjY)) {
                    double distToInter = hypot(resX - trajInter.getX(), resY - trajInter.getY());
                    if (exclusionRadius != null && distToInter <= exclusionRadius) {
                        collision = true;
                        break;
                    }
                    if (distToProjectile > distToInter) {
                        distToProjectile = distToInter;
                    }
                }

                double distToTrajEnd = hypot(simProjX - resX, simProjY - resY);

                if (distToProjectile > distToTrajEnd) {
                    distToProjectile = distToTrajEnd;
                }

                double distToTrajStart = hypot(projectilePoint.getX() - resX, projectilePoint.getY() - resY);
                if (distToProjectileInit > distToTrajStart) {
                    distToProjectileInit = distToTrajStart;
                }

                if (exclusionRadius != null && distToTrajEnd <= exclusionRadius) {
                    collision = true;
                    break;
                }
                if (considerOutsideWorld) {
                    for (Unit unit : world.getAllUnitsNearby()) {
                        if (unit.getId() == wizardId) {
                            continue;
                        }
                        if (unit instanceof Projectile || unit instanceof Bonus) {
                            continue;
                        }
                        double unitR = ((CircularUnit) unit).getRadius();
                        if (unit.getDistanceTo(resX, resY) < game.getWizardRadius() + unitR) {
                            collision = true;
                            break;
                        }
                        if (MathMethods.isLineIntersectsCircle(wizardPoint.getX(),
                                resX,
                                wizardPoint.getY(),
                                resY,
                                unit.getX(),
                                unit.getY(),
                                game.getWizardRadius() + ((CircularUnit) unit).getRadius())) {
                            collision = true;
                            break;
                        }
                    }
                }
            }

            if (!collision) {
                results.add(new DodgeOption(directionId,
                        distToProjectile,
                        distToProjectileInit,
                        new Movement(baseSpeed, baseStrafe, baseTurn)));
            }
        }

        return results;
    }

    public static class DodgeOption {

        private final int id;
        private final double distToProjectile;
        private final double distToProjectileInit;
        private final Movement move;

        public DodgeOption(int id, double distToProjectile, double distToProjectileInit, Movement move) {
            this.id = id;
            this.distToProjectile = distToProjectile;
            this.distToProjectileInit = distToProjectileInit;
            this.move = move;
        }

        public int getId() {
            return id;
        }

        public double getDistToProjectile() {
            return distToProjectile;
        }

        public double getDistToProjectileInit() {
            return distToProjectileInit;
        }

        public Movement getMove() {
            return move;
        }
    }
}
