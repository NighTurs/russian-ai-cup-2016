import model.Move;

import java.util.EnumSet;

public class TacticImpl implements Tactic {

    private final String name;
    private final MoveBuilder moveBuilder;
    private final int priority;

    public TacticImpl(String name, MoveBuilder moveBuilder, int priority) {
        this.name = name;
        this.moveBuilder = moveBuilder;
        this.priority = priority;
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public EnumSet<TacticAction> actions() {
        return moveBuilder.getActions();
    }

    @Override
    public void applyOnMove(Move move) {
        moveBuilder.apply(move);
    }

    @Override
    public int priority() {
        return priority;
    }
}
