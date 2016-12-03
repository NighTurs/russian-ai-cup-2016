import model.ActionType;
import model.CircularUnit;
import model.Tree;
import model.Unit;

import java.util.Optional;

public class StaffHitTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if (self.getRemainingCooldownTicksByAction()[ActionType.STAFF.ordinal()] != 0 ||
                self.getRemainingActionCooldownTicks() != 0) {
            return Optional.empty();
        }
        for (Unit unit : turnContainer.getWorldProxy().allUnits()) {
            double dist = self.getDistanceTo(unit);
            double angle = self.getAngleTo(unit);
            if (dist <= turnContainer.getGame().getStaffRange() + ((CircularUnit) unit).getRadius() &&
                    WizardProxy.getWizardStaffSector(turnContainer.getGame()) >= Math.abs(angle) &&
                    (turnContainer.isOffensiveUnit(unit) || unit instanceof Tree)) {
                MoveBuilder moveBuilder = new MoveBuilder();
                moveBuilder.setAction(ActionType.STAFF);
                return Optional.of(new TacticImpl("StaffHit", moveBuilder, Tactics.STAFF_HIT_TACTIC_PRIORITY));
            }
        }
        return Optional.empty();
    }
}
