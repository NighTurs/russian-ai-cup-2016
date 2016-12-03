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

    public static double castRangeToWizardPessimistic(WizardProxy self, WizardProxy wizard, Game game) {
        double undodgebaleDistance = game.getWizardRadius() + game.getMagicMissileRadius() -
                (int) Math.ceil(self.getCastRange() / game.getMagicMissileSpeed()) *
                        wizard.getWizardBackwardSpeed(game);
        return self.getCastRange() + undodgebaleDistance;
    }

    public static double castRangeToWizardOptimistic(WizardProxy self, WizardProxy wizard, Game game) {
        double undodgebaleDistance = game.getWizardRadius() + game.getMagicMissileRadius() -
                (int) Math.ceil(self.getCastRange() / game.getMagicMissileSpeed() - 1) *
                        wizard.getWizardBackwardSpeed(game);
        return self.getCastRange() + undodgebaleDistance;
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
                manaPriorities.get(ActionType.FIREBALL) > priority && self.getMana() +
                untilProjectileCast(self, ActionType.FIREBALL) * self.getWizardManaPerTurn(game) -
                manaCost < game.getFireballManacost()) {
            return true;
        }
        return false;
    }
}
