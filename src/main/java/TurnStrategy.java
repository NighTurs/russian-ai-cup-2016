import model.Move;

import java.util.*;

public class TurnStrategy {

    private static final List<TacticBuilder> tacticBuilders = Arrays.asList(new PushLaneTacticBuilder(),
            new CastMagicMissileTacticBuilder(),
            new SurviveTacticBuilder(),
            new GoForBonusTacticBuilder(),
            new StaffHitTacticBuilder());
    private final TurnContainer turnContainer;
    private final Move move;

    public TurnStrategy(TurnContainer turnContainer, Move move) {
        this.turnContainer = turnContainer;
        this.move = move;
    }

    public Move findStrategy() {
        List<Tactic> tactics = new ArrayList<>();
        for (TacticBuilder builder : tacticBuilders) {
            Optional<Tactic> tacticOpt = builder.build(turnContainer);
            tacticOpt.ifPresent(tactics::add);
        }
        tactics.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        EnumSet<TacticAction> usedActions = EnumSet.noneOf(TacticAction.class);
        for (Tactic tactic : tactics) {
            if (tactic.actions().stream().map(usedActions::contains).anyMatch(Boolean::booleanValue)) {
                continue;
            }
            usedActions.addAll(tactic.actions());
            tactic.applyOnMove(move);
        }
        return move;
    }
}
