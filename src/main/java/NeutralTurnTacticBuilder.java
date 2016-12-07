import java.util.Optional;

public class NeutralTurnTacticBuilder implements TacticBuilder {

    private static final int IGNORE_WIZARD_RANGE = 800;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        MapUtils mapUtils = turnContainer.getMapUtils();
        WizardProxy self = turnContainer.getSelf();

        WizardProxy closestEnemy = null;
        double minDist = Double.MAX_VALUE;
        for (WizardProxy wizard : turnContainer.getWorldProxy().getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard)) {
                continue;
            }
            if (minDist > self.getDistanceTo(wizard)) {
                minDist = self.getDistanceTo(wizard);
                closestEnemy = wizard;
            }
        }

        if (closestEnemy == null || minDist > IGNORE_WIZARD_RANGE) {
            return Optional.empty();
        }

        MoveBuilder moveBuilder = new MoveBuilder();
        double currentAngle = self.getAngleTo(closestEnemy.getX(), closestEnemy.getY());
        if (Math.abs(currentAngle) < Math.PI / 2) {
            moveBuilder.setTurn(-self.getAngleTo(closestEnemy.getX(), closestEnemy.getY()));
            return Optional.of(new TacticImpl("NeutralTurn", moveBuilder, Tactics.NEUTRAL_TURN_TACTIC_BUILDER));
        } else {
            return Optional.empty();
        }
    }
}
