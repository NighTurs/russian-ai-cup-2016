public class Movement {
    private final double speed;
    private final double strafeSpeed;
    private final double turn;

    public Movement(double speed, double strafeSpeed, double turn) {
        this.speed = speed;
        this.strafeSpeed = strafeSpeed;
        this.turn = turn;
    }

    public double getSpeed() {
        return speed;
    }

    public double getStrafeSpeed() {
        return strafeSpeed;
    }

    public double getTurn() {
        return turn;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Movement{");
        sb.append("speed=").append(speed);
        sb.append(", strafeSpeed=").append(strafeSpeed);
        sb.append(", turn=").append(turn);
        sb.append('}');
        return sb.toString();
    }
}
