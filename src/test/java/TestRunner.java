import model.Game;
import model.Move;
import model.PlayerContext;
import model.Wizard;

import java.io.IOException;

public final class TestRunner {
    private final RemoteProcessClient remoteProcessClient;
    private final String token;

    public static void main(String[] args) throws IOException {
        new TestRunner(args.length == 3 ? args : new String[]{"127.0.0.1", "31001", "0000000000000000"}).run();
    }

    private TestRunner(String[] args) throws IOException {
        remoteProcessClient = new RemoteProcessClient(args[0], Integer.parseInt(args[1]));
        token = args[2];
    }

    public void run() throws IOException {
        try {
            remoteProcessClient.writeToken(token);
            remoteProcessClient.writeProtocolVersion();
            int teamSize = remoteProcessClient.readTeamSize();
            Game game = remoteProcessClient.readGameContext();

            MyStrategy[] strategies = new MyStrategy[teamSize];

            for (int strategyIndex = 0; strategyIndex < teamSize; ++strategyIndex) {
                strategies[strategyIndex] = new MyStrategy();
            }

            PlayerContext playerContext;

            while ((playerContext = remoteProcessClient.readPlayerContext()) != null) {
                Wizard[] playerWizards = playerContext.getWizards();
                if (playerWizards == null || playerWizards.length != teamSize) {
                    break;
                }

                Move[] moves = new Move[teamSize];

                for (int wizardIndex = 0; wizardIndex < teamSize; ++wizardIndex) {
                    Wizard playerWizard = playerWizards[wizardIndex];

                    Move move = new Move();
                    moves[wizardIndex] = move;
                    strategies[wizardIndex].move(
                            playerWizard, playerContext.getWorld(), game, move
                    );
                    validate(strategies[wizardIndex]);
                }

                remoteProcessClient.writeMoves(moves);
            }
        } finally {
            remoteProcessClient.close();
        }
    }

    private void validate(MyStrategy strategy) {
        MapUtilsTest.test(strategy);
    }
}
