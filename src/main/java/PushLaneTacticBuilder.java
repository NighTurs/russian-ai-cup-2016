import model.Faction;
import model.Minion;
import model.Unit;
import model.Wizard;

import java.util.Optional;

public class PushLaneTacticBuilder implements TacticBuilder {
    private static final int SAFE_DISTANCE_MIN = 200;
    private static final int SAFE_DISTANCE_MAX = 400;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        LocationType lane = turnContainer.getLanePicker().myLane();
        Wizard self = turnContainer.getSelf();

        Optional<Unit> offensiveOpt = findClosestOffensiveUnit(lane, turnContainer);
        Point offensivePoint = offensiveOpt.isPresent() ?
                new Point(offensiveOpt.get().getX(), offensiveOpt.get().getY()) :
                new Point(world.getWidth(), 0);
        Optional<Unit> allyOpt = findClosestAllyUnit(offensivePoint, lane, turnContainer);
        if (!allyOpt.isPresent()) {
            return Optional.empty();
        }
        Point allyPoint = new Point(allyOpt.get().getX(), allyOpt.get().getY());

        double retreatX = world.allyBase().getX();
        double retreatY = world.allyBase().getY();

        double distWizard = dist(self.getX(), self.getY(), retreatX, retreatY);
        double distEnemy = dist(offensivePoint.getX(), offensivePoint.getY(), retreatX, retreatY);
        double distAlly = dist(allyPoint.getX(), allyPoint.getY(), retreatX, retreatY);
        double distWizAlly = dist(self.getX(), self.getY(), allyPoint.getX(), allyPoint.getY());
        double distWizEnemy = dist(self.getX(), self.getY(), offensivePoint.getX(), offensivePoint.getY());
        double distAllyEnemy =
                dist(allyPoint.getX(), allyPoint.getY(), offensivePoint.getX(), offensivePoint.getY());

        Movement mov;
        if (distWizard <= distAlly && distAllyEnemy <= distEnemy) {
            double distSafety = distWizAlly / (distWizAlly + distAllyEnemy) * distWizEnemy;
            if (distSafety > SAFE_DISTANCE_MAX) {
                mov = turnContainer.getPathFinder().findPath(self, allyPoint.getX(), allyPoint.getY());
            } else if (distSafety < SAFE_DISTANCE_MIN) {
                mov = turnContainer.getPathFinder().findPath(self, retreatX, retreatY);
            } else {
                return Optional.empty();
            }
        } else {
            mov = turnContainer.getPathFinder().findPath(self, retreatX, retreatY);
        }

        MoveBuilder moveBuilder = new MoveBuilder();
        moveBuilder.setSpeed(mov.getSpeed());
        moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
        if (!hasEnemyInAttackRange(turnContainer)) {
            moveBuilder.setTurn(mov.getTurn());
        }

        return Optional.of(new TacticImpl("PushLane", moveBuilder, Tactics.PUSH_LANE_TACTIC_PRIORITY));
    }

    private Optional<Unit> findClosestOffensiveUnit(LocationType lane, TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        Wizard self = turnContainer.getSelf();
        UnitLocationType unitLocationType = turnContainer.getUnitLocationType();
        Faction opposingFaction = turnContainer.opposingFaction();

        double bestDist = Double.MAX_VALUE;
        Unit bestUnit = null;
        for (Unit unit : world.allUnits()) {
            LocationType curLane = unitLocationType.getLocationType(unit.getId());
            if (curLane != lane && curLane != LocationType.ALLY_BASE && curLane != LocationType.ENEMY_BASE) {
                continue;
            }
            if (unit.getFaction() != opposingFaction &&
                    !(unit instanceof Minion && turnContainer.isOffensiveMinion((Minion) unit))) {
                continue;
            }

            double dist = unit.getDistanceTo(self);
            if (dist < bestDist) {
                bestDist = dist;
                bestUnit = unit;
            }
        }
        return Optional.ofNullable(bestUnit);
    }

    private Optional<Unit> findClosestAllyUnit(Point toPoint, LocationType lane, TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        Wizard self = turnContainer.getSelf();
        UnitLocationType unitLocationType = turnContainer.getUnitLocationType();
        Faction allyFaction = self.getFaction();

        double bestDist = Double.MAX_VALUE;
        Unit bestUnit = null;
        for (Unit unit : world.allUnits()) {
            LocationType curLane = unitLocationType.getLocationType(unit.getId());
            if (curLane != lane && curLane != LocationType.ALLY_BASE && curLane != LocationType.ENEMY_BASE) {
                continue;
            }
            if (unit.getFaction() != allyFaction) {
                continue;
            }

            double dist = unit.getDistanceTo(toPoint.getX(), toPoint.getY());
            if (dist < bestDist) {
                bestDist = dist;
                bestUnit = unit;
            }
        }
        return Optional.ofNullable(bestUnit);
    }

    private boolean hasEnemyInAttackRange(TurnContainer turnContainer) {
        Wizard self = turnContainer.getSelf();
        Faction opposingFaction = turnContainer.opposingFaction();
        for (Unit unit : turnContainer.getWorldProxy().allUnits()) {
            if (unit.getFaction() != opposingFaction) {
                continue;
            }
            double dist = unit.getDistanceTo(self);
            if (dist < WizardTraits.getWizardCastRange(self, turnContainer.getGame())) {
                return true;
            }
        }
        return false;
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}
