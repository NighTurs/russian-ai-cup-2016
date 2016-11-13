import model.Move;

import java.util.EnumSet;

public interface Tactic {
    String name();
    EnumSet<TacticAction> actions();
    void applyOnMove(Move move);
    int priority();
}
