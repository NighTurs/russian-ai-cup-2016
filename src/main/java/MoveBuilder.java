import model.ActionType;
import model.Message;
import model.Move;
import model.SkillType;

import java.util.EnumSet;
import java.util.Objects;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MoveBuilder {
    private Double speed;
    private Double strafeSpeed;
    private Double turn;
    private ActionType action;
    private Double castAngle;
    private Double minCastDistance;
    private Double maxCastDistance;
    private Long statusTargetId;
    private SkillType skillToLearn;
    private Message[] messages;

    private final EnumSet<TacticAction> actions;

    public MoveBuilder() {
        this.actions = EnumSet.noneOf(TacticAction.class);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        actions.add(TacticAction.MODIFY_SPEED);
    }

    public void setStrafeSpeed(double strafeSpeed) {
        this.strafeSpeed = strafeSpeed;
        actions.add(TacticAction.MODIFY_STRAFE);
    }

    public void setTurn(double turn) {
        this.turn = turn;
        actions.add(TacticAction.MODIFY_TURN);
    }

    public void setAction(ActionType action) {
        Objects.nonNull(action);
        this.action = action;
    }

    public void setCastAngle(double castAngle) {
        this.castAngle = castAngle;
    }

    public void setMinCastDistance(double minCastDistance) {
        this.minCastDistance = minCastDistance;
    }

    public void setMaxCastDistance(double maxCastDistance) {
        this.maxCastDistance = maxCastDistance;
    }

    public void setStatusTargetId(long statusTargetId) {
        this.statusTargetId = statusTargetId;
    }

    public void setSkillToLearn(SkillType skillToLearn) {
        Objects.nonNull(skillToLearn);
        this.skillToLearn = skillToLearn;
    }

    public void setMessages(Message[] messages) {
        Objects.nonNull(messages);
        this.messages = messages;
    }

    public EnumSet<TacticAction> getActions() {
        return EnumSet.copyOf(actions);
    }

    public void apply(Move move) {
        if (speed != null) {
            move.setSpeed(speed);
        }
        if (strafeSpeed != null) {
            move.setStrafeSpeed(strafeSpeed);
        }
        if (turn != null) {
            move.setTurn(turn);
        }
        if (action != null) {
            move.setAction(action);
        }
        if (castAngle != null) {
            move.setCastAngle(castAngle);
        }
        if (minCastDistance != null) {
            move.setMinCastDistance(minCastDistance);
        }
        if (maxCastDistance != null) {
            move.setMaxCastDistance(maxCastDistance);
        }
        if (statusTargetId != null) {
            move.setStatusTargetId(statusTargetId);
        }
        if (skillToLearn != null) {
            move.setSkillToLearn(skillToLearn);
        }
        if (messages != null) {
            move.setMessages(messages);
        }
    }
}
