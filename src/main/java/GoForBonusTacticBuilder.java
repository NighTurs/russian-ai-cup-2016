import model.*;

import java.util.Optional;

public class GoForBonusTacticBuilder implements TacticBuilder {

    private static final int ALLY_HALF_MANHATTAN_BASES = 9;
    private static final int ARRIVE_BEFORE_TICKS = 80;
    private static final int EXPECTED_TICKS_TO_BONUS_ERROR = 400;
    private static final int ACCEPTABLE_TICKS_TO_TAKE_BONUS_WITH_SKILLS = 1000;
    private static final int ACCEPTABLE_TICKS_TO_TAKE_BONUS_WITHOUT_SKILLS = 1300;
    private static final int KEEP_DISTANCE_TO_BONUS = 2;
    private final DirectionOptionalTacticBuilder directionOptional;

    public GoForBonusTacticBuilder(DirectionOptionalTacticBuilder directionOptional) {
        this.directionOptional = directionOptional;
    }

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        BonusControl bonusControl = turnContainer.getBonusControl();
        WizardProxy self = turnContainer.getSelf();
        PathFinder pathFinder = turnContainer.getPathFinder();
        WorldProxy world = turnContainer.getWorldProxy();
        Game game = turnContainer.getGame();

        if (self.getX() + world.getHeight() - self.getY() <=
                turnContainer.getWorldProxy().allyBase().getX() * ALLY_HALF_MANHATTAN_BASES) {
            turnContainer.getMemory().setWentForBonusPrevTurn(false);
            return Optional.empty();
        }

        Point topBonus = bonusControl.topBonusPosition();
        Point bottomBonus = bonusControl.bottomBonusPosition();
        Point goForBonus;
        double ticksUntilBonus;
        if (shouldGoForBottomBonus(turnContainer)) {
            goForBonus = bottomBonus;
            ticksUntilBonus = bonusControl.ticksUntilBottomBonus();
        } else {
            goForBonus = topBonus;
            ticksUntilBonus = bonusControl.ticksUntilTopBonus();
        }

        if (turnContainer.getGame().isRawMessagesEnabled() && (!isClosestToBonues(turnContainer, goForBonus) ||
                turnContainer.getLanePicker().myLane() != LocationType.MIDDLE_LANE)) {
            turnContainer.getMemory().setWentForBonusPrevTurn(false);
            return Optional.empty();
        }

        double ticksToBonus =
                roughDistToBonus(self, pathFinder, goForBonus) / self.getWizardForwardSpeed(turnContainer.getGame()) +
                        ARRIVE_BEFORE_TICKS;
        if (ticksToBonus * 2 > getAcceptableTicksToTakeBonus(turnContainer)) {
            turnContainer.getMemory().setWentForBonusPrevTurn(false);
            return Optional.empty();
        }
        if (ticksToBonus < ticksUntilBonus && (!turnContainer.getMemory().isWentForBonusPrevTurn() ||
                ticksToBonus + EXPECTED_TICKS_TO_BONUS_ERROR < ticksUntilBonus)) {
            turnContainer.getMemory().setWentForBonusPrevTurn(false);
            return Optional.empty();
        }
        turnContainer.getMemory().setWentForBonusPrevTurn(true);
        turnContainer.getMemory().setExpectedPushDuration(0);

        boolean haveBonusInVisibilityRange = false;
        for (Bonus bonus : world.getBonuses()) {
            if (bonus.getDistanceTo(self) <= self.getVisionRange()) {
                haveBonusInVisibilityRange = true;
            }
        }
        if (haveBonusInVisibilityRange || self.getDistanceTo(goForBonus.getX(), goForBonus.getY()) >
                game.getBonusRadius() + self.getRadius() + KEEP_DISTANCE_TO_BONUS) {
            Movement mov =
                    pathFinder.findPath(self, goForBonus.getX(), goForBonus.getY(), game.getBonusRadius(), false);
            MoveBuilder moveBuilder = new MoveBuilder();

            double leftDistance = self.getDistanceTo(goForBonus.getX(), goForBonus.getY()) -
                    (game.getBonusRadius() + self.getRadius() + KEEP_DISTANCE_TO_BONUS);

            if (leftDistance >= self.getWizardForwardSpeed(game) || haveBonusInVisibilityRange) {
                moveBuilder.setSpeed(mov.getSpeed());
                moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
            } else {
                double maxSpeed = mov.getSpeed();
                double maxStrafe = mov.getStrafeSpeed();
                double ratio = leftDistance / Math.hypot(maxSpeed, maxStrafe);
                moveBuilder.setSpeed(maxSpeed * ratio);
                moveBuilder.setStrafeSpeed(maxStrafe * ratio);
            }
            if (!PushLaneTacticBuilder.hasEnemyInPotentialAttackRange(turnContainer)) {
                moveBuilder.setTurn(mov.getTurn());
            } else {
                directionOptional.addTurn(world.getTickIndex(), mov.getTurn());
            }
            return tactic(moveBuilder);
        } else {
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setSpeed(0);
            moveBuilder.setStrafeSpeed(0);
            return tactic(moveBuilder);
        }
    }

    private Optional<Tactic> tactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("GoForBonus", moveBuilder, Tactics.GO_FOR_BONUS_TACTIC_PRIORITY));
    }

    private double roughDistToBonus(WizardProxy self, PathFinder pathFinder, Point goForBonus) {
        return pathFinder.roughDistanceTo(self, goForBonus.getX(), goForBonus.getY());
    }

    private boolean shouldGoForBottomBonus(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Point topBonus = turnContainer.getBonusControl().topBonusPosition();
        Point bottomBonus = turnContainer.getBonusControl().bottomBonusPosition();
        // Going for top bonus on middle is dangerous if middle enemy tower is up
        if (turnContainer.getLanePicker().myLane() == LocationType.MIDDLE_LANE) {
            double unit = turnContainer.getWorldProxy().allyBase().getX();
            for (Building building : turnContainer.getWorldProxy().getBuildings()) {
                if (building.getType() == BuildingType.GUARDIAN_TOWER &&
                        building.getFaction() == turnContainer.opposingFaction() && building.getX() >= unit * 4 &&
                        building.getX() <= unit * 6 && building.getY() >= unit * 3 && building.getY() <= unit * 5) {
                    return true;
                }
            }
        }
        return self.getDistanceTo(topBonus.getX(), topBonus.getY()) >
                self.getDistanceTo(bottomBonus.getX(), bottomBonus.getY());
    }

    private int getAcceptableTicksToTakeBonus(TurnContainer turnContainer) {
        if (turnContainer.getGame().isSkillsEnabled()) {
            if (turnContainer.getSelf().isSkillLearned(SkillType.FIREBALL)) {
                return ACCEPTABLE_TICKS_TO_TAKE_BONUS_WITHOUT_SKILLS / 2;
            } else {
                return ACCEPTABLE_TICKS_TO_TAKE_BONUS_WITH_SKILLS;
            }
        } else {
            return ACCEPTABLE_TICKS_TO_TAKE_BONUS_WITHOUT_SKILLS;
        }
    }

    private boolean isClosestToBonues(TurnContainer turnContainer, Point bonusPoint) {
        WizardProxy self = turnContainer.getSelf();
        for (WizardProxy wizard : turnContainer.getWorldProxy().getWizards()) {
            LocationType locationType = turnContainer.getMapUtils().getLocationType(wizard.getId());
            if (!turnContainer.isAllyWizard(wizard) ||
                    locationType == LocationType.BOTTOM_LANE || locationType == LocationType.TOP_LANE) {
                continue;
            }
            if (wizard.getDistanceTo(bonusPoint.getX(), bonusPoint.getY()) <
                    self.getDistanceTo(bonusPoint.getX(), bonusPoint.getY())) {
                return false;
            }
        }
        return true;
    }
}
