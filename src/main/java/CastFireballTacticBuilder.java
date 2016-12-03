import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class CastFireballTacticBuilder implements TacticBuilder {

    private static final int PREPARE_TO_CAST_THRESHOLD = 30;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        Game game = turnContainer.getGame();
        WizardProxy self = turnContainer.getSelf();
        boolean fireballLearned = turnContainer.isSkillLearned(self, SkillType.FIREBALL);
        int untilCast = CastProjectileTacticBuilders.untilProjectileCast(self, ActionType.FIREBALL);
        boolean haveEnoughMana = self.getMana() + untilCast * self.getWizardManaPerTurn(game) >=
                game.getFireballManacost();
        if (!game.isSkillsEnabled() || !fireballLearned || !haveEnoughMana || untilCast > PREPARE_TO_CAST_THRESHOLD) {
            return Optional.empty();
        }

        Optional<Point> clusterPointOpt = bestClusterCastPoint(turnContainer);
        Point targetPoint;
        if (!clusterPointOpt.isPresent()) {
            Optional<Point> singlePointOpt = bestSingleTarget(turnContainer);
            if (!singlePointOpt.isPresent()) {
                return Optional.empty();
            } else {
                targetPoint = singlePointOpt.get();
            }
        } else {
            targetPoint = clusterPointOpt.get();
        }
        MoveBuilder moveBuilder = new MoveBuilder();
        if (CastProjectileTacticBuilders.inCastSector(turnContainer, targetPoint)) {
            castWithMove(moveBuilder, targetPoint, turnContainer);
            return assembleTactic(moveBuilder);
        } else {
            moveBuilder.setTurn(self.getAngleTo(targetPoint.getX(), targetPoint.getY()));
            return assembleTactic(moveBuilder);
        }
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastFireball", moveBuilder, Tactics.CAST_FIREBALL_TACTIC_PRIORITY));
    }

    private void castWithMove(MoveBuilder moveBuilder, Point point, TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if (self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()] == 0 &&
                self.getRemainingActionCooldownTicks() == 0) {
            moveBuilder.setAction(ActionType.FIREBALL);
            moveBuilder.setCastAngle(self.getAngleTo(point.getX(), point.getY()));
            moveBuilder.setMinCastDistance(self.getDistanceTo(point.getX(), point.getY()));
            moveBuilder.setMaxCastDistance(self.getDistanceTo(point.getX(), point.getY()));
        }
    }

    private Optional<Point> bestSingleTarget(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        Point buildingPoint = null;
        Point wizardPoint = null;
        for (Unit unit : turnContainer.getWorldProxy().getAllUnitsNearby()) {
            if (!turnContainer.isOffensiveUnit(unit) || unit instanceof Minion) {
                continue;
            }
            double dist = self.getDistanceTo(unit);
            if (dist <
                    self.getRadius() + game.getFireballExplosionMinDamageRange() + ((CircularUnit) unit).getRadius()) {
                continue;
            }
            if (unit instanceof Building) {
                Building building = (Building) unit;
                if (dist < self.getWizardCastRange(game) + game.getFireballExplosionMinDamageRange() +
                        building.getRadius()) {
                    buildingPoint = new Point(building.getX(), building.getY());
                }
            } else if (unit instanceof WizardProxy) {
                WizardProxy wizard = (WizardProxy) unit;
                if (dist < self.getWizardCastRange(game) + game.getFireballExplosionMinDamageRange() +
                        wizard.getRadius() - wizard.getWizardForwardSpeed(game)) {
                    wizardPoint = new Point(wizard.getX(), wizard.getY());
                }
            } else {
                throw new RuntimeException("Unexpected type of target passed through " + unit.getClass());
            }
        }
        if (wizardPoint != null) {
            return Optional.of(wizardPoint);
        } else if (buildingPoint != null) {
            return Optional.of(buildingPoint);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Point> bestClusterCastPoint(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        Double distThresholdMax =
                self.getWizardCastRange(game) + game.getFireballExplosionMinDamageRange();
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
        return Optional.of(centroid(clusterTargets));
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
