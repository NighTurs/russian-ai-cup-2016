import model.ActionType;
import model.Game;
import model.SkillType;
import model.StatusType;

import java.util.Optional;

public class ApplyHasteTacticBuilder extends ApplyStatusTacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        if (!self.isSkillLearned(SkillType.HASTE)) {
            return Optional.empty();
        }
        Optional<WizardProxy> targetOpt = bestTarget(turnContainer, x -> (double) x.getXp(), StatusType.HASTENED);
        if (!targetOpt.isPresent()) {
            return Optional.empty();
        }
        WizardProxy target = targetOpt.get();
        int untilCast = untilNextStatus(self, StatusType.HASTENED, game);
        Point targetPoint = new Point(target.getX(), target.getY());
        if (untilCast == 0 && CastProjectileTacticBuilders.inCastSector(turnContainer, self, targetPoint)) {
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setAction(ActionType.HASTE);
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
        return Optional.of(new TacticImpl("ApplyHaste", moveBuilder, Tactics.APPLY_HASTE_TACTIC_BUILDER));
    }
}
