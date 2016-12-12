import model.*;

import java.util.Optional;

public class StaffHitTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();

        boolean isStaffReady = self.getRemainingCooldownTicksByAction()[ActionType.STAFF.ordinal()] == 0 &&
                self.getRemainingActionCooldownTicks() == 0;
        if (isStaffReady) {
            for (Tree tree : turnContainer.getWorldProxy().getTrees()) {
                if (isInStaffRange(self, tree, game) && isInStaffCastSector(self, tree, game)) {
                    MoveBuilder moveBuilder = new MoveBuilder();
                    moveBuilder.setAction(ActionType.STAFF);
                    return createTactic(moveBuilder);
                }
            }
        }

        Optional<Unit> focusUnitOpt =
                CastProjectileTacticBuilders.bestFocusTarget(turnContainer, ProjectileType.MAGIC_MISSILE, 0);
        if (!focusUnitOpt.isPresent()) {
            return Optional.empty();
        }

        Unit focusUnit = focusUnitOpt.get();
        if (isInStaffRange(self, focusUnit, game)) {
            MoveBuilder moveBuilder = new MoveBuilder();
            if (isInStaffCastSector(self, focusUnit, game)) {
                if (isStaffReady) {
                    moveBuilder.setAction(ActionType.STAFF);
                } else {
                    moveBuilder.setTurn(0);
                }
            } else {
                moveBuilder.setTurn(self.getAngleTo(focusUnit));
            }
            return createTactic(moveBuilder);
        }

        return Optional.empty();
    }

    private boolean isInStaffRange(WizardProxy wizard, Unit unit, Game game) {
        return wizard.getDistanceTo(unit) <= game.getStaffRange() + ((CircularUnit) unit).getRadius();
    }

    private boolean isInStaffCastSector(WizardProxy wizard, Unit unit, Game game) {
        return WizardProxy.getWizardStaffSector(game) >= Math.abs(wizard.getAngleTo(unit));
    }

    private Optional<Tactic> createTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("StaffHit", moveBuilder, Tactics.STAFF_HIT_TACTIC_PRIORITY));
    }
}
