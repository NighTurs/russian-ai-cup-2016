import model.Game;
import model.ProjectileType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CastRangeService {

    private static final String extremeCastPropsLine = null;
    private static final int ANGLES_TO_TEST = 40;
    private static final int MAX_DIST_TO_CENTER = 700;
    private static final int MAX_CENTER_OFFSET = 100;
    private static final double DIST_PRECISION = 1e-2;
    // no way to get 5 amplification
    private static final List<Integer> SPEED_AMPLIFIERS = Arrays.asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10);
    private static final List<Integer> RANGE_AMPLIFIERS = Arrays.asList(0, 1, 2, 3, 4);
    private static final Point dummyWizardPoint = new Point(1000, 1000);
    // projectile type, target speed amplifier, caster range amplifier
    private final Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps;

    public CastRangeService(WorldProxy worldProxy, Game game) {
        //noinspection ConstantConditions
        if (extremeCastPropsLine == null) {
            this.extremeCastProps = new EnumMap<>(ProjectileType.class);
            for (ProjectileType projectileType : Arrays.asList(ProjectileType.MAGIC_MISSILE,
                    ProjectileType.FIREBALL,
                    ProjectileType.FROST_BOLT)) {
                simulateCastsFixedProjectile(projectileType, extremeCastProps, worldProxy, game);
            }
            try (FileWriter writer = new FileWriter("extremeCastProps.txt")) {
                writer.append(extremeCastPropsToLine(extremeCastProps));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.extremeCastProps = parseExtremeCastProps();
        }
    }

    private static String extremeCastPropsToLine(Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps) {
        StringBuilder sb = new StringBuilder();
        //noinspection KeySetIterationMayUseEntrySet
        for (ProjectileType projectileType : extremeCastProps.keySet()) {
            for (Integer speedAmpl : extremeCastProps.get(projectileType).keySet()) {
                for (Integer rangeAmpl : extremeCastProps.get(projectileType).get(speedAmpl).keySet()) {
                    for (Double angle : extremeCastProps.get(projectileType).get(speedAmpl).get(rangeAmpl).keySet()) {
                        CastMeta castMeta =
                                extremeCastProps.get(projectileType).get(speedAmpl).get(rangeAmpl).get(angle);
                        sb.append(String.format("%s,%s,%s,%.3f,%.3f,%.3f#",
                                projectileCode(projectileType),
                                rangeAmpl,
                                speedAmpl,
                                angle,
                                castMeta.getDistToCenter(),
                                castMeta.getCenterOffset()));
                    }
                }
            }
        }
        return sb.toString();
    }

    private static Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> parseExtremeCastProps() {
        Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps =
                new EnumMap<>(ProjectileType.class);
        //noinspection ConstantConditions
        for (String record : extremeCastPropsLine.split("#")) {
            String[] fields = record.split(",");
            putToExtremeCastProps(projectileFromCode(fields[0]),
                    Integer.parseInt(fields[1]),
                    Integer.parseInt(fields[2]),
                    Double.parseDouble(fields[3]),
                    Double.parseDouble(fields[4]),
                    Double.parseDouble(fields[5]),
                    extremeCastProps);
        }
        return extremeCastProps;
    }

    private static void putToExtremeCastProps(ProjectileType projectileType,
                                              Integer rangeAmpl,
                                              Integer speedAmpl,
                                              Double angle,
                                              Double distToCenter,
                                              Double centerOffset,
                                              Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps) {

        if (!extremeCastProps.containsKey(projectileType)) {
            extremeCastProps.put(projectileType, new HashMap<>());
        }
        if (!extremeCastProps.get(projectileType).containsKey(rangeAmpl)) {
            extremeCastProps.get(projectileType).put(rangeAmpl, new HashMap<>());
        }
        if (!extremeCastProps.get(projectileType).get(rangeAmpl).containsKey(speedAmpl)) {
            extremeCastProps.get(projectileType).get(rangeAmpl).put(speedAmpl, new HashMap<>());
        }
        extremeCastProps.get(projectileType)
                .get(rangeAmpl)
                .get(speedAmpl)
                .put(angle, new CastMeta(distToCenter, centerOffset));
    }

    private static void simulateCastsFixedProjectile(ProjectileType projectileType,
                                                     Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps,
                                                     WorldProxy worldProxy,
                                                     Game game) {
        for (int speedAmpl : SPEED_AMPLIFIERS) {
            simulateCastsFixedSpeedAmpl(speedAmpl, projectileType, extremeCastProps, worldProxy, game);
        }
    }

    private static void simulateCastsFixedSpeedAmpl(int speedAmpl,
                                                    ProjectileType projectileType,
                                                    Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps,
                                                    WorldProxy worldProxy,
                                                    Game game) {
        for (int rangeAmpl : RANGE_AMPLIFIERS) {
            simulateCastsFixedRangeAmpl(rangeAmpl, speedAmpl, projectileType, extremeCastProps, worldProxy, game);
        }
    }

    private static void simulateCastsFixedRangeAmpl(int rangeAmpl,
                                                    int speedAmpl,
                                                    ProjectileType projectileType,
                                                    Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps,
                                                    WorldProxy worldProxy,
                                                    Game game) {
        for (double angle = -Math.PI; angle <= Math.PI; angle += Math.PI * 2 / ANGLES_TO_TEST) {
            simulateCastsFixedAngle(angle, rangeAmpl, speedAmpl, projectileType, extremeCastProps, worldProxy, game);
        }
    }

    private static void simulateCastsFixedAngle(double angle,
                                                int rangeAmpl,
                                                int speedAmpl,
                                                ProjectileType projectileType,
                                                Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps,
                                                WorldProxy worldProxy,
                                                Game game) {
        double d1 = 0;
        double d2 = MAX_DIST_TO_CENTER;
        while (d2 - d1 > DIST_PRECISION) {
            double d = (d1 + d2) / 2;
            Optional<Double> centerOffset = simulateCastsFixedDist(d,
                    angle,
                    rangeAmpl,
                    speedAmpl,
                    projectileType,
                    extremeCastProps,
                    worldProxy,
                    game);
            if (centerOffset.isPresent()) {
                d2 = d;
            } else {
                d1 = d;
            }
        }
        Optional<Double> centerOffsetOpt = simulateCastsFixedDist(d2,
                angle,
                rangeAmpl,
                speedAmpl,
                projectileType,
                extremeCastProps,
                worldProxy,
                game);
        Double centerOffset = centerOffsetOpt.orElseThrow(() -> new RuntimeException(
                "Can't happen. Some minimal cast distance is always undodgeable"));
        if (!extremeCastProps.containsKey(projectileType)) {
            extremeCastProps.put(projectileType, new HashMap<>());
        }
        if (!extremeCastProps.get(projectileType).containsKey(rangeAmpl)) {
            extremeCastProps.get(projectileType).put(rangeAmpl, new HashMap<>());
        }
        if (!extremeCastProps.get(projectileType).get(rangeAmpl).containsKey(speedAmpl)) {
            extremeCastProps.get(projectileType).get(rangeAmpl).put(speedAmpl, new HashMap<>());
        }
        extremeCastProps.get(projectileType).get(rangeAmpl).get(speedAmpl).put(angle, new CastMeta(d2, centerOffset));
        System.out.print(String.format("%s,%s,%s,%.3f,%.3f,%.3f#",
                projectileCode(projectileType),
                rangeAmpl,
                speedAmpl,
                angle,
                d2,
                centerOffset));
    }

    private static Optional<Double> simulateCastsFixedDist(double distToCenter,
                                                           double angle,
                                                           int rangeAmpl,
                                                           int speedAmpl,
                                                           ProjectileType projectileType,
                                                           Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps,
                                                           WorldProxy worldProxy,
                                                           Game game) {
        double d1 = 0;
        double d2 = MAX_CENTER_OFFSET;
        while (d2 - d1 > DIST_PRECISION) {
            double m1 = d1 + (d2 - d1) / 3;
            double m2 = d2 - (d2 - d1) / 3;
            double maxDodgeM1 = simulateCastsFixedOffset(m1,
                    distToCenter,
                    angle,
                    rangeAmpl,
                    speedAmpl,
                    projectileType,
                    extremeCastProps,
                    worldProxy,
                    game);
            double maxDodgeM2 = simulateCastsFixedOffset(m2,
                    distToCenter,
                    angle,
                    rangeAmpl,
                    speedAmpl,
                    projectileType,
                    extremeCastProps,
                    worldProxy,
                    game);

            if (maxDodgeM1 <= maxDodgeM2) {
                d2 = m2;
            } else {
                d1 = m1;
            }
        }
        double maxDodge = simulateCastsFixedOffset(d1,
                distToCenter,
                angle,
                rangeAmpl,
                speedAmpl,
                projectileType,
                extremeCastProps,
                worldProxy,
                game);
        if (maxDodge >
                game.getWizardRadius() + CastProjectileTacticBuilders.projectileEffectiveRadius(game, projectileType)) {
            return Optional.of(d1);
        } else {
            return Optional.empty();
        }
    }

    private static double simulateCastsFixedOffset(double centerOffset,
                                                   double distToCenter,
                                                   double angle,
                                                   int rangeAmpl,
                                                   int speedAmpl,
                                                   ProjectileType projectileType,
                                                   Map<ProjectileType, Map<Integer, Map<Integer, Map<Double, CastMeta>>>> extremeCastProps,
                                                   WorldProxy worldProxy,
                                                   Game game) {
        Point projectilePoint = new Point(dummyWizardPoint.getX() + distToCenter, dummyWizardPoint.getY());
        double aimPointX = dummyWizardPoint.getX() + centerOffset * Math.cos(angle);
        double aimPointY = dummyWizardPoint.getY() + centerOffset * Math.sin(angle);
        Point projectileTravelsTo = MathMethods.distPoint(projectilePoint.getX(),
                projectilePoint.getY(),
                aimPointX,
                aimPointY,
                amplifyRange(game.getWizardCastRange(), rangeAmpl, game));

        List<DodgeProjectileTacticBuilder.DodgeOption> options = DodgeProjectileTacticBuilder.tryDodgeDirections(
                dummyWizardPoint,
                1,
                angle,
                amplifySpeed(game.getWizardForwardSpeed(), speedAmpl, game),
                amplifySpeed(game.getWizardBackwardSpeed(), speedAmpl, game),
                amplifySpeed(game.getWizardStrafeSpeed(), speedAmpl, game),
                amplifyAngle(game.getWizardMaxTurnAngle(), speedAmpl, game),
                projectilePoint,
                projectileType,
                projectileTravelsTo,
                false,
                null,
                game,
                worldProxy);
        options.sort(Comparator.comparingDouble(DodgeProjectileTacticBuilder.DodgeOption::getDistToProjectile));
        if (options.isEmpty()) {
            throw new RuntimeException("Can't happen, tryDodgeDirections will return result for every direction");
        }
        return options.get(options.size() - 1).getDistToProjectile();
    }

    private static double amplifyRange(double range, int rangeAmpl, Game game) {
        return range + game.getRangeBonusPerSkillLevel() * rangeAmpl;
    }

    private static double amplifySpeed(double speed, int speedAmpl, Game game) {
        return speed * (1 + game.getMovementBonusFactorPerSkillLevel() * speedAmpl);
    }

    private static double amplifyAngle(double angle, int speedAmpl, Game game) {
        return angle * (speedAmpl >= 6 ? 1 + game.getHastenedRotationBonusFactor() : 1);
    }

    private static String projectileCode(ProjectileType type) {
        switch (type) {
            case FROST_BOLT:
                return "F";
            case MAGIC_MISSILE:
                return "M";
            case FIREBALL:
                return "E";
            default:
                throw new RuntimeException("Unexpected projectile type " + type);
        }
    }

    private static ProjectileType projectileFromCode(String code) {
        switch (code) {
            case "F":
                return ProjectileType.FROST_BOLT;
            case "M":
                return ProjectileType.MAGIC_MISSILE;
            case "E":
                return ProjectileType.FIREBALL;
            default:
                throw new RuntimeException("Unexpected projectile code " + code);
        }
    }

    private static class CastMeta {

        private final double distToCenter;
        private final double centerOffset;

        public CastMeta(double distToCenter, double centerOffset) {
            this.distToCenter = distToCenter;
            this.centerOffset = centerOffset;
        }

        public double getDistToCenter() {
            return distToCenter;
        }

        public double getCenterOffset() {
            return centerOffset;
        }
    }
}
