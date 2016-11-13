import java.util.Optional;

public class PickLaneTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        Movement move = turnContainer.getPathFinder().findPath(turnContainer.getSelf(), 2000, 2000);
        MoveBuilder moveBuilder = new MoveBuilder();
        moveBuilder.setSpeed(move.getSpeed());
        moveBuilder.setStrafeSpeed(move.getStrafeSpeed());
        moveBuilder.setTurn(move.getTurn());
        return Optional.of(new TacticImpl("PickLane", moveBuilder, Tactics.PICK_LANE_TACTIC_PRIORITY));
    }
}
