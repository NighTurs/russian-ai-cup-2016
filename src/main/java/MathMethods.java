import static java.lang.StrictMath.hypot;

public final class MathMethods {
    private static final double E = 1e-9;

    private MathMethods() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static Point distPoint(double fromX, double fromY, double toX, double toY, double dist) {
        double curDist = hypot(fromX - toX, fromY - toY);
        if (curDist < E) {
            return new Point(fromX, fromY);
        }
        double proportion = dist / curDist;
        double deltaX = (toX - fromX) * proportion;
        double deltaY = (toY - fromY) * proportion;
        return new Point(fromX + deltaX, fromY + deltaY);
    }

    public static Point lineCircleIntersection(double x1, double x2, double y1, double y2, double cx, double cy) {
        double a = y1 - y2;
        double b = x2 - x1;
        if (Math.abs(a) + Math.abs(b) < E) {
            return new Point(x1, y1);
        }
        double c = (x1 - x2) * y1 + (y2 - y1) * x1;
        double xi = (b * (b * cx - a * cy) - a * c) / (a * a + b * b);
        double yi = (a * (-b * cx + a * cy) - b * c) / (a * a + b * b);
        return new Point(xi, yi);
    }

    public static boolean isLineIntersectsCircle(double x1, double x2, double y1, double y2, double cx, double cy, double r) {
        double a = y1 - y2;
        double b = x2 - x1;
        if (Math.abs(a) + Math.abs(b) < E) {
            return hypot(x1 - cx, y1 - cy) <= r;
        }
        double c = (x1 - x2) * y1 + (y2 - y1) * x1;
        double xi = (b * (b * cx - a * cy) - a * c) / (a * a + b * b);
        double yi = (a * (-b * cx + a * cy) - b * c) / (a * a + b * b);
        return isBetween(xi, x1, x2) && isBetween(yi, y1, y2) && hypot(cx - xi, cy - yi) <= r;
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    public static boolean isBetween(double v, double v1, double v2) {
        if (v1 > v2) {
            double z = v1;
            v1 = v2;
            v2 = z;
        }
        return v >= v1 && v <= v2;
    }

}
