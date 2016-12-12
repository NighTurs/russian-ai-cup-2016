import model.ActionType;
import model.Game;
import model.SkillType;
import model.StatusType;

import java.util.Optional;

public class ApplyShieldTacticBuilder extends ApplyStatusTacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        if (!self.isSkillLearned(SkillType.SHIELD)) {
            return Optional.empty();
        }
        Optional<WizardProxy> targetOpt = bestTarget(turnContainer, x -> (double) x.getLife(), StatusType.SHIELDED);
        if (!targetOpt.isPresent()) {
            return Optional.empty();
        }
        WizardProxy target = targetOpt.get();
        int untilCast = untilNextStatus(self, StatusType.SHIELDED, game);
        Point targetPoint = new Point(target.getX(), target.getY());
        if (untilCast == 0 && CastProjectileTacticBuilders.inCastSector(turnContainer, targetPoint)) {
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setAction(ActionType.SHIELD);
            moveBuilder.setStatusTargetId(target.getId());
            return assembleTactic(moveBuilder);
        }
        Optional<Double> turn = CastProjectileTacticBuilders.justInTimeTurn(self, targetPoint, untilCast, game);
        if (turn.isPresent()) {
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setTurn(turn.get());
            return assembleTactic(moveBuilder);
        }
        return Optional.empty();
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("ApplyShield", moveBuilder, Tactics.APPLY_SHIELD_TACTIC_BUILDER));
    }
}
