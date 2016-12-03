import model.*;

import java.util.Optional;

public class CastMagicMissileTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();

        if (CastProjectileTacticBuilders.shouldSaveUpMana(turnContainer, ActionType.MAGIC_MISSILE)) {
            return Optional.empty();
        }
        Optional<Unit> bestTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer);
        if (!bestTargetOpt.isPresent()) {
            return Optional.empty();
        }
        Unit bestTarget = bestTargetOpt.get();
        MoveBuilder moveBuilder = new MoveBuilder();
        if (CastProjectileTacticBuilders.inCastSector(turnContainer, new Point(bestTarget.getX(), bestTarget.getY()))) {
            castWithMove(moveBuilder, bestTarget, turnContainer);
            return assembleTactic(moveBuilder);
        } else {
            moveBuilder.setTurn(self.getAngleTo(bestTarget));
            return assembleTactic(moveBuilder);
        }
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastMagicMissile", moveBuilder, Tactics.CAST_MAGIC_MISSILE_TACTIC_PRIORITY));
    }

    private void castWithMove(MoveBuilder moveBuilder, Unit unit, TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0 &&
                self.getRemainingActionCooldownTicks() == 0) {
            moveBuilder.setAction(ActionType.MAGIC_MISSILE);
            moveBuilder.setCastAngle(self.getAngleTo(unit));
            moveBuilder.setMinCastDistance(self.getDistanceTo(unit) - ((CircularUnit) unit).getRadius());
        }
    }
}
