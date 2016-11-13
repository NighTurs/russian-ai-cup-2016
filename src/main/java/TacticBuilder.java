import java.util.Optional;

public interface TacticBuilder {
    Optional<Tactic> build(TurnContainer turnContainer);
}
