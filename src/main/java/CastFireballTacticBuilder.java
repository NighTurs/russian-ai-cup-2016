import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class CastFireballTacticBuilder implements TacticBuilder {
    private static final int MIN_CLUSTER_OF_MINIONS = 3;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        Game game = turnContainer.getGame();
        WizardProxy self = turnContainer.getSelf();
        boolean fireballLearned = self.isSkillLearned(SkillType.FIREBALL);
        int untilCast = CastProjectileTacticBuilders.untilNextProjectile(self, ProjectileType.FIREBALL, game);

        if (!game.isSkillsEnabled() || !fireballLearned) {
            return Optional.empty();
        }

        Point targetPoint;
        Optional<Unit> singlePointOpt = bestSingleTarget(turnContainer);
        Optional<Point> clusterPointOpt = bestClusterCastPoint(turnContainer);
        if (singlePointOpt.isPresent() &&
                (singlePointOpt.get() instanceof WizardProxy || !clusterPointOpt.isPresent())) {
            Unit unit = singlePointOpt.get();
            if (unit instanceof WizardProxy) {
                WizardProxy wizard = (WizardProxy) unit;
                targetPoint = wizard.faceOffsetPoint(CastProjectileTacticBuilders.castMeta(turnContainer,
                        self,
                        wizard,
                        ProjectileType.FIREBALL).getCenterOffset());
            } else if (unit instanceof Building) {
                Building building = (Building) unit;
                targetPoint = MathMethods.distPoint(self.getX(),
                        self.getY(),
                        building.getX(),
                        building.getY(),
                        self.getDistanceTo(building) + building.getRadius());
            } else {
                targetPoint = new Point(unit.getX(), unit.getY());
            }
        } else if (clusterPointOpt.isPresent()) {
            targetPoint = clusterPointOpt.get();
        } else {
            return Optional.empty();
        }

        MoveBuilder moveBuilder = new MoveBuilder();
        if (CastProjectileTacticBuilders.inCastSector(turnContainer, self, targetPoint) && untilCast == 0) {
            moveBuilder.setAction(ActionType.FIREBALL);
            moveBuilder.setCastAngle(self.getAngleTo(targetPoint.getX(), targetPoint.getY()));
            moveBuilder.setMinCastDistance(self.getDistanceTo(targetPoint.getX(), targetPoint.getY()));
            moveBuilder.setMaxCastDistance(self.getDistanceTo(targetPoint.getX(), targetPoint.getY()));
            return assembleTactic(moveBuilder);
        }
        Optional<Double> turn = CastProjectileTacticBuilders.justInTimeTurn(self, targetPoint, untilCast, game);
        if (turn.isPresent()) {
            moveBuilder.setTurn(turn.get());
            return assembleTactic(moveBuilder);
        }
        return Optional.empty();
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastFireball", moveBuilder, Tactics.CAST_FIREBALL_TACTIC_PRIORITY));
    }

    private Optional<Unit> bestSingleTarget(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        Building bestBuilding = null;
        WizardProxy bestWizard = null;
        for (Unit unit : turnContainer.getWorldProxy().getAllUnitsNearby()) {
            if (!turnContainer.isOffensiveUnit(unit) || unit instanceof Minion) {
                continue;
            }
            double dist = self.getDistanceTo(unit);
            if (unit instanceof Building) {
                Building building = (Building) unit;
                if (!turnContainer.getMapUtils().isIgnorableBuilding(self, building) &&
                        dist < self.getCastRange() + game.getFireballExplosionMinDamageRange() + building.getRadius()) {
                    bestBuilding = building;
                }
            } else if (unit instanceof WizardProxy) {
                WizardProxy wizard = (WizardProxy) unit;
                if (!wizard.isRealOrFreshShadow()) {
                    continue;
                }
                if (CastProjectileTacticBuilders.isInCastRange(turnContainer, self, wizard, ProjectileType.FIREBALL)) {
                    bestWizard = wizard;
                }
            } else {
                throw new RuntimeException("Unexpected type of target passed through " + unit.getClass());
            }
        }
        if (bestWizard != null) {
            return Optional.of(bestWizard);
        } else if (bestBuilding != null) {
            return Optional.of(bestBuilding);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Point> bestClusterCastPoint(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        Double distThresholdMax = self.getCastRange() + game.getFireballExplosionMinDamageRange();
        Double distThresholdMin = game.getFireballExplosionMinDamageRange();
        List<CircularUnit> potentialTargets = new ArrayList<>();
        for (Unit unit : turnContainer.getWorldProxy().getAllUnitsNearby()) {
            if (!turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = self.getDistanceTo(unit);
            if (dist >= distThresholdMin && dist <= distThresholdMax) {
                potentialTargets.add((CircularUnit) unit);
            }
        }

        if (potentialTargets.size() < 2) {
            return Optional.empty();
        }

        List<CircularUnit> clusterTargets = new ArrayList<>();
        double pairMinDist = Double.MAX_VALUE;
        for (CircularUnit unitA : potentialTargets) {
            for (CircularUnit unitB : potentialTargets) {
                if (unitA.getId() == unitB.getId()) {
                    continue;
                }
                if (pairMinDist > unitA.getDistanceTo(unitB)) {
                    pairMinDist = unitA.getDistanceTo(unitB);
                    clusterTargets.clear();
                    clusterTargets.add(unitA);
                    clusterTargets.add(unitB);
                }
            }
        }

        if (!areUnitsInDamageRange(self, clusterTargets, centroid(clusterTargets), game)) {
            return Optional.empty();
        }

        Set<Long> evaluatedTargets = new HashSet<>();
        evaluatedTargets.addAll(clusterTargets.stream().map(Unit::getId).collect(Collectors.toList()));
        Point curCentroid = centroid(clusterTargets);

        while (true) {
            final Point valCentroid = curCentroid;
            Optional<CircularUnit> closestUnitOpt = potentialTargets.stream()
                    .filter(x -> !evaluatedTargets.contains(x.getId()))
                    .min(Comparator.comparingDouble(x -> x.getDistanceTo(valCentroid.getX(), valCentroid.getY())));
            if (!closestUnitOpt.isPresent()) {
                break;
            }
            CircularUnit closestUnit = closestUnitOpt.get();
            clusterTargets.add(closestUnit);
            Point newCentroid = centroid(clusterTargets);
            if (areUnitsInDamageRange(self, clusterTargets, newCentroid, game)) {
                curCentroid = newCentroid;
            } else {
                clusterTargets.remove(clusterTargets.size() - 1);
            }
            evaluatedTargets.add(closestUnit.getId());
        }

        int minionCount = 0;
        for (CircularUnit unit : clusterTargets) {
            if (unit instanceof Building ||
                    (unit instanceof WizardProxy && ((WizardProxy) unit).isRealOrFreshShadow())) {
                return Optional.of(centroid(clusterTargets));
            } else {
                minionCount++;
            }
        }
        if (MIN_CLUSTER_OF_MINIONS <= minionCount) {
            return Optional.of(centroid(clusterTargets));
        } else {
            return Optional.empty();
        }
    }

    private Point centroid(List<CircularUnit> units) {
        double sumX = 0;
        double sumY = 0;
        for (Unit unit : units) {
            sumX += unit.getX();
            sumY += unit.getY();
        }
        return new Point(sumX / units.size(), sumY / units.size());
    }

    private boolean areUnitsInDamageRange(WizardProxy self, List<CircularUnit> units, Point point, Game game) {
        int untilImpact = (int) Math.ceil(self.getDistanceTo(point.getX(), point.getY()) / game.getFireballSpeed());
        for (CircularUnit unit : units) {
            double dist = unit.getDistanceTo(point.getX(), point.getY());
            double speed = unit instanceof Building ? 0 : game.getWizardBackwardSpeed();
            if (dist > game.getFireballExplosionMinDamageRange() + unit.getRadius() - untilImpact * speed) {
                return false;
            }
        }
        return true;
    }
}
