import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DirectionOptionalTacticBuilder implements TacticBuilder {

    private final Map<Integer, Double> turnsByTick = new HashMap<>();

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        Double turn = turnsByTick.get(turnContainer.getWorldProxy().getTickIndex());
        turnsByTick.clear();
        if (turn != null) {
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setTurn(turn);
            return Optional.of(new TacticImpl("DirectionOptional",
                    moveBuilder,
                    Tactics.DIRECTION_OPTIONAL_TACTIC_BUILDER));
        } else {
            return Optional.empty();
        }
    }

    public void addTurn(int tick, double turn) {
        turnsByTick.put(tick, turn);
    }
}
