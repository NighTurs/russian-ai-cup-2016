import model.*;

import java.util.EnumMap;
import java.util.Map;

public final class CastProjectileTacticBuilders {

    private static final Map<ActionType, Integer> manaPriorities;

    static {
        manaPriorities = new EnumMap<>(ActionType.class);
        manaPriorities.put(ActionType.FIREBALL, 2);
        manaPriorities.put(ActionType.MAGIC_MISSILE, 1);
    }

    private CastProjectileTacticBuilders() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static double castRangeToWizardPessimistic(Wizard self, Wizard wizard, Game game) {
        double undodgebaleDistance = game.getWizardRadius() + game.getMagicMissileRadius() -
                (int) Math.ceil(self.getCastRange() / game.getMagicMissileSpeed()) *
                        WizardTraits.getWizardBackwardSpeed(wizard, game);
        return WizardTraits.getWizardCastRange(self, game) + undodgebaleDistance;
    }

    public static double castRangeToWizardOptimistic(Wizard self, Wizard wizard, Game game) {
        double undodgebaleDistance = game.getWizardRadius() + game.getMagicMissileRadius() -
                (int) Math.ceil(self.getCastRange() / game.getMagicMissileSpeed() - 1) *
                        WizardTraits.getWizardBackwardSpeed(wizard, game);
        return WizardTraits.getWizardCastRange(self, game) + undodgebaleDistance;
    }

    public static double castRangeToBuilding(Wizard self, Building building, Game game) {
        return WizardTraits.getWizardCastRange(self, game) + building.getRadius() + game.getMagicMissileRadius();
    }

    public static double castRangeToMinion(Wizard self, Minion minion, Game game) {
        return WizardTraits.getWizardCastRange(self, game);
    }

    public static boolean inCastSector(TurnContainer turnContainer, Point point) {
        Wizard self = turnContainer.getSelf();
        double angle = self.getAngleTo(point.getX(), point.getY());
        return WizardTraits.getWizardCastSector(turnContainer.getGame()) > Math.abs(angle);
    }

    public static int untilProjectileCast(Wizard wizard, ActionType actionType) {
        return Math.max(wizard.getRemainingActionCooldownTicks(),
                wizard.getRemainingCooldownTicksByAction()[actionType.ordinal()]);
    }

    public static boolean shouldSaveUpMana(TurnContainer turnContainer, ActionType actionType) {
        if (actionType != ActionType.MAGIC_MISSILE) {
            throw new RuntimeException("Not supported action type");
        }
        Game game = turnContainer.getGame();
        Wizard self = turnContainer.getSelf();
        if (!game.isSkillsEnabled()) {
            return false;
        }
        int priority = manaPriorities.get(actionType);
        int manaCost = game.getMagicMissileManacost();
        //noinspection RedundantIfStatement
        if (turnContainer.isSkillLearned(self, SkillType.FIREBALL) &&
                manaPriorities.get(ActionType.FIREBALL) > priority && self.getMana() +
                untilProjectileCast(self, ActionType.FIREBALL) * WizardTraits.getWizardManaPerTurn(self, game) -
                manaCost < game.getFireballManacost()) {
            return true;
        }
        return false;
    }
}
