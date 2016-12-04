import model.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CastProjectileTacticBuilders {

    private static final double E = 1e-6;
    private static final Map<ActionType, Integer> manaPriorities;

    static {
        manaPriorities = new EnumMap<>(ActionType.class);
        manaPriorities.put(ActionType.FIREBALL, 2);
        manaPriorities.put(ActionType.MAGIC_MISSILE, 1);
    }

    private CastProjectileTacticBuilders() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static double undodgeableRadiusPessimistic(double selfCastRange, double enemySpeed, Game game) {
        return game.getWizardRadius() + game.getMagicMissileRadius() -
                (int) Math.ceil(selfCastRange / game.getMagicMissileSpeed()) * enemySpeed;
    }

    public static double undodgeableRadiusOptimistic(double selfCastRange, double enemySpeed, Game game) {
        return game.getWizardRadius() + game.getMagicMissileRadius() -
                (int) Math.ceil(selfCastRange / game.getMagicMissileSpeed() - 1) * enemySpeed;
    }

    public static double effectiveCastRangeToWizard(WizardProxy self,
                                                    WizardProxy wizard,
                                                    Game game,
                                                    boolean optimistic) {
        double r1 = 0;
        double r2 = self.getCastRange();
        while (r2 - r1 > E) {
            double r = (r1 + r2) / 2;
            double radius = optimistic ?
                    undodgeableRadiusOptimistic(r, wizard.getWizardBackwardSpeed(game), game) :
                    undodgeableRadiusPessimistic(r, wizard.getWizardBackwardSpeed(game), game);
            if (radius > 0) {
                r1 = r;
            } else {
                r2 = r;
            }
        }
        return r1;
    }

    private static double castRangeToWizard(WizardProxy self, WizardProxy wizard, Game game, boolean optimistic) {
        double r = effectiveCastRangeToWizard(self, wizard, game, optimistic);
        return r + (optimistic ?
                undodgeableRadiusOptimistic(r, wizard.getWizardBackwardSpeed(game), game) :
                undodgeableRadiusPessimistic(r, wizard.getWizardBackwardSpeed(game), game));
    }

    public static double castRangeToWizardPessimistic(WizardProxy self, WizardProxy wizard, Game game) {
        return castRangeToWizard(self, wizard, game, false);
    }

    public static double castRangeToWizardOptimistic(WizardProxy self, WizardProxy wizard, Game game) {
        return castRangeToWizard(self, wizard, game, true);
    }

    public static double castRangeToBuilding(WizardProxy self, Building building, Game game) {
        return self.getCastRange() + building.getRadius() + game.getMagicMissileRadius();
    }

    public static double castRangeToMinion(WizardProxy self, Minion minion, Game game) {
        return self.getCastRange();
    }

    public static boolean inCastSector(TurnContainer turnContainer, Point point) {
        WizardProxy self = turnContainer.getSelf();
        double angle = self.getAngleTo(point.getX(), point.getY());
        return WizardProxy.getWizardCastSector(turnContainer.getGame()) > Math.abs(angle);
    }

    public static int untilProjectileCast(WizardProxy wizard, ActionType actionType) {
        return Math.max(wizard.getRemainingActionCooldownTicks(),
                wizard.getRemainingCooldownTicksByAction()[actionType.ordinal()]);
    }

    public static boolean shouldSaveUpMana(TurnContainer turnContainer, ActionType actionType) {
        if (actionType != ActionType.MAGIC_MISSILE) {
            throw new RuntimeException("Not supported action type");
        }
        Game game = turnContainer.getGame();
        WizardProxy self = turnContainer.getSelf();
        if (!game.isSkillsEnabled()) {
            return false;
        }
        int priority = manaPriorities.get(actionType);
        int manaCost = game.getMagicMissileManacost();
        //noinspection RedundantIfStatement
        if (turnContainer.isSkillLearned(self, SkillType.FIREBALL) &&
                manaPriorities.get(ActionType.FIREBALL) > priority &&
                self.getMana() + untilProjectileCast(self, ActionType.FIREBALL) * self.getWizardManaPerTurn(game) -
                        manaCost < game.getFireballManacost()) {
            return true;
        }
        return false;
    }

    public static Optional<Unit> bestFocusTarget(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        WizardProxy self = turnContainer.getSelf();

        Unit bestUnit = null;
        int lowestLife = Integer.MAX_VALUE;
        for (WizardProxy wizard : world.getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard)) {
                continue;
            }
            double dist = wizard.getDistanceTo(self);
            if (dist <=
                    CastProjectileTacticBuilders.castRangeToWizardPessimistic(self, wizard, turnContainer.getGame()) &&
                    lowestLife > wizard.getLife()) {
                lowestLife = wizard.getLife();
                bestUnit = wizard;
            }
        }
        if (bestUnit != null) {
            return Optional.of(bestUnit);
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= turnContainer.getGame().getStaffRange() + minion.getRadius()) {
                return Optional.of(minion);
            }
        }

        for (Building building : world.getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(building)) {
                continue;
            }
            double dist = building.getDistanceTo(self);
            if (dist <= CastProjectileTacticBuilders.castRangeToBuilding(self, building, turnContainer.getGame())) {
                return Optional.of(building);
            }
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= CastProjectileTacticBuilders.castRangeToMinion(self, minion, turnContainer.getGame()) &&
                    lowestLife > minion.getLife()) {
                lowestLife = minion.getLife();
                bestUnit = minion;
            }
        }
        return Optional.ofNullable(bestUnit);
    }
}
