import java.util.Optional;

public class NeutralTurnTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        MapUtils mapUtils = turnContainer.getMapUtils();
        WizardProxy self = turnContainer.getSelf();
        LocationType lane = turnContainer.getLanePicker().myLane();
        Point retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane);
        MoveBuilder moveBuilder = new MoveBuilder();
        double currentAngle = self.getAngleTo(retreatWaypoint.getX(), retreatWaypoint.getY());
        if (Math.abs(currentAngle) > Math.PI / 2) {
            moveBuilder.setTurn(self.getAngleTo(retreatWaypoint.getX(), retreatWaypoint.getY()));
            return Optional.of(new TacticImpl("NeutralTurn", moveBuilder, Tactics.NEUTRAL_TURN_TACTIC_BUILDER));
        } else {
            return Optional.empty();
        }
    }
}
