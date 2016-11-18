import model.*;

import java.util.*;

import static java.lang.StrictMath.hypot;

public class PathFinder {

    private static final int DEFAULT_CELL_WIDTH = 100;
    private static final double UNSTATIC_OBJECTS_RADIUS_ADJUST = 2.9;
    private static final double E = 1e-9;
    private static final int SHORT_SEARCH_GRID_CELL = 10;
    private static final int SHORT_SEARCH_GRID_SPAN = 250;
    private static final int LONG_DISTANCE_MIN_FIRST_MOVE = 500;
    private static final int MAX_ANGLE_RADIUS = 500;
    private final WorldProxy world;
    private final Game game;
    private final Wizard self;
    private final boolean[][] longSearchGrid;
    private final int cellWidth;
    private final int gridN;
    private final int gridM;
    private boolean shortSearchGridInitialized;
    private PathPoint[][] shortSearchGrid;
    private int shortSearchGridDim;

    public PathFinder(Wizard self, WorldProxy world, Game game, MapUtils mapUtils) {
        this.world = world;
        this.game = game;
        this.self = self;
        this.cellWidth = DEFAULT_CELL_WIDTH;
        this.gridN = (int) (world.getWidth() / cellWidth + 1);
        this.gridM = (int) (world.getHeight() / cellWidth + 1);
        this.longSearchGrid = new boolean[gridN][gridM];
        shortSearchGridInitialized = false;

        double wizardRadius = game.getWizardRadius();

        for (int i = 0; i * cellWidth <= world.getWidth(); i++) {
            for (int h = 0; h * cellWidth <= world.getHeight(); h++) {
                longSearchGrid[i][h] = true;
                double x = toRealAxis(i);
                double y = toRealAxis(h);
                if (mapUtils.isForest(x, y)) {
                    longSearchGrid[i][h] = false;
                }
                if (x < wizardRadius || y < wizardRadius || world.getHeight() - y < wizardRadius ||
                        world.getWidth() - x < wizardRadius) {
                    longSearchGrid[i][h] = false;
                }
            }
        }
    }

    public Movement findPath(Wizard wizard, double x, double y) {
        Point longDistPoint = longSearchNextPoint(wizard.getX(), wizard.getY(), x, y);
        Optional<Point> straightLinePoint =
                straightLinePath(wizard.getX(), wizard.getY(), longDistPoint.getX(), longDistPoint.getY());
        if (straightLinePoint.isPresent()) {
            return findOptimalMovement(wizard, straightLinePoint.get().getX(), straightLinePoint.get().getY());
        } else {
            Point shortDistPoint = shortSearchNextPoint(longDistPoint.getX(), longDistPoint.getY());
            return findOptimalMovement(wizard, shortDistPoint.getX(), shortDistPoint.getY());
        }
    }

    private double toRealAxis(int index) {
        return index * cellWidth;
    }

    private Movement findOptimalMovement(Wizard wizard, double x, double y) {
        double bestDistance = Double.MAX_VALUE;
        double optimalSpeed = 0;
        double optimalStrafe = 0;
        double maxTurnAngle = WizardTraits.getWizardMaxTurnAngle(wizard, game);
        double optimalTurn = Math.min(maxTurnAngle, Math.max(-maxTurnAngle, wizard.getAngleTo(x, y)));
        double wizardNextAngle = wizard.getAngle() + optimalTurn;

        double cos = Math.cos(wizardNextAngle);
        double sin = Math.sin(wizardNextAngle);

        for (double speedOffset = 0.0; speedOffset <= 1.0; speedOffset++) {
            for (int speedSign = -1; speedSign <= 1; speedSign += 2) {
                for (int strafeSign = -1; strafeSign <= 1; strafeSign += 2) {
                    double strafeOffset = Math.sqrt(1 - speedOffset * speedOffset);
                    double speed;
                    if (speedSign == -1) {
                        speed = -WizardTraits.getWizardBackwardSpeed(wizard, game) * speedOffset;
                    } else {
                        speed = WizardTraits.getWizardForwardSpeed(wizard, game) * speedOffset;
                    }
                    double strafe = strafeSign * WizardTraits.getWizardStrafeSpeed(wizard, game) * strafeOffset;
                    double resX = wizard.getX() + speed * cos - strafe * sin;
                    double resY = wizard.getY() + speed * sin + strafe * cos;
                    boolean foundIntersection = false;
                    if (resX < wizard.getRadius() || resY < wizard.getRadius() ||
                            resX > world.getWidth() - wizard.getRadius() ||
                            resY > world.getHeight() - wizard.getRadius()) {
                        continue;
                    }
                    for (Unit unit : world.allUnits()) {
                        if (unit.getId() == wizard.getId()) {
                            continue;
                        }
                        if (!(unit instanceof CircularUnit)) {
                            throw new RuntimeException("Uncircular unit is not expected");
                        }
                        if (unit instanceof Projectile || unit instanceof Bonus) {
                            continue;
                        }
                        double unitR = ((CircularUnit) unit).getRadius();
                        if (unit instanceof Wizard || unit instanceof Minion) {
                            unitR += UNSTATIC_OBJECTS_RADIUS_ADJUST;
                        }
                        if (unit.getDistanceTo(resX, resY) < wizard.getRadius() + unitR) {
                            foundIntersection = true;
                            break;
                        }
                        if (isLineIntersectsCircle(wizard.getX(),
                                resX,
                                wizard.getY(),
                                resY,
                                unit.getX(),
                                unit.getY(),
                                wizard.getRadius() + ((CircularUnit) unit).getRadius())) {
                            foundIntersection = true;
                            break;
                        }
                    }
                    if (foundIntersection) {
                        continue;
                    }
                    double curDist = hypot(x - resX, y - resY);
                    if (curDist < bestDistance) {
                        bestDistance = curDist;
                        optimalSpeed = speed;
                        optimalStrafe = strafe;
                    }
                }
            }
        }
        return new Movement(optimalSpeed, optimalStrafe, optimalTurn);
    }

    private Point longSearchNextPoint(double fromX, double fromY, double toX, double toY) {
        if (hypot(fromX - toX, fromY - toY) <= SHORT_SEARCH_GRID_SPAN) {
            return new Point(toX, toY);
        }

        int fromI = (int) Math.round(fromX / cellWidth);
        int fromH = (int) Math.round(fromY / cellWidth);
        @SuppressWarnings("SuspiciousNameCombination")
        double diagDist = hypot(cellWidth, cellWidth);

        Queue<BfsPoint> bfs = new PriorityQueue<>((a, b) -> Double.compare(a.getDist(), b.getDist()));
        bfs.add(new BfsPoint(fromI, fromH, -1, -1, 0));
        Map<BfsPoint, Double> visitedPoints = new HashMap<>();
        visitedPoints.put(bfs.peek(), 0D);

        double bestDist = hypot(fromX - toX, fromY - toY);
        double bestX = toX;
        double bestY = toY;

        while (!bfs.isEmpty()) {
            BfsPoint cur = bfs.poll();
            if (Math.abs(visitedPoints.get(cur) - cur.getDist()) > E) {
                continue;
            }
            for (int j1 = -1; j1 <= 1; j1++) {
                for (int j2 = -1; j2 <= 1; j2++) {
                    if (j1 == 0 && j2 == 0) {
                        continue;
                    }
                    int nextI = cur.getI() + j1;
                    int nextH = cur.getH() + j2;
                    int firstMoveI = cur.getFirstMoveI();
                    int firstMoveH = cur.getFirstMoveH();
                    double dist = cur.getDist() + (j1 == 0 || j2 == 0 ? cellWidth : diagDist);
                    if (firstMoveI == -1 || dist < LONG_DISTANCE_MIN_FIRST_MOVE) {
                        firstMoveI = nextI;
                        firstMoveH = nextH;
                    }
                    BfsPoint nextPoint = new BfsPoint(nextI, nextH, firstMoveI, firstMoveH, dist);
                    if (nextI < 0 || nextI >= gridN || nextH < 0 || nextH >= gridM ||
                            (visitedPoints.containsKey(nextPoint) && visitedPoints.get(nextPoint) <= dist) ||
                            !longSearchGrid[nextI][nextH]) {
                        continue;
                    }
                    double curDist = hypot(toX - toRealAxis(nextI), toY - toRealAxis(nextH));
                    if (curDist < bestDist) {
                        bestDist = curDist;
                        bestX = toRealAxis(firstMoveI);
                        bestY = toRealAxis(firstMoveH);
                    }
                    visitedPoints.put(nextPoint, dist);
                    bfs.add(nextPoint);
                }
            }
        }

        return new Point(bestX, bestY);
    }

    private Point shortSearchNextPoint(double x, double y) {
        if (!shortSearchGridInitialized) {
            initShortSearchGrid();
        }

        int dim = shortSearchGridDim;

        int selfInd = dim / 2 - 1;
        double[][] visitedPoints = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            Arrays.fill(visitedPoints[i], -1);
        }
        Queue<BfsPoint> bfs = new PriorityQueue<>((a, b) -> Double.compare(a.getDist(), b.getDist()));
        bfs.add(new BfsPoint(selfInd, selfInd, -1, -1, 0));
        visitedPoints[selfInd][selfInd] = 0;

        double bestDist = hypot(shortSearchGrid[selfInd][selfInd].getPoint().getX() - x,
                shortSearchGrid[selfInd][selfInd].getPoint().getY() - y);
        double bestX = x;
        double bestY = y;

        while (!bfs.isEmpty()) {
            BfsPoint cur = bfs.poll();
            if (Math.abs(visitedPoints[cur.getI()][cur.getH()] - cur.getDist()) > E) {
                continue;
            }
            PathPoint p = shortSearchGrid[cur.getI()][cur.getH()];
            for (Map.Entry<PathPoint, Double> entry : p.getNeighbors().entrySet()) {
                int newI = entry.getKey().getI();
                int newH = entry.getKey().getH();
                double newDist = cur.getDist() + entry.getValue();
                if (Math.abs(visitedPoints[newI][newH] + 1) < E || visitedPoints[newI][newH] > newDist) {
                    BfsPoint next = new BfsPoint(newI,
                            newH,
                            cur.getFirstMoveI() == -1 ? entry.getKey().getI() : cur.getFirstMoveI(),
                            cur.getFirstMoveH() == -1 ? entry.getKey().getH() : cur.getFirstMoveH(),
                            newDist);
                    visitedPoints[newI][newH] = newDist;
                    bfs.add(next);
                    double curDist = hypot(x - entry.getKey().getPoint().getX(), y - entry.getKey().getPoint().getY());
                    if (curDist < bestDist) {
                        bestDist = curDist;
                        bestX = shortSearchGrid[next.getFirstMoveI()][next.getFirstMoveH()].getPoint().getX();
                        bestY = shortSearchGrid[next.getFirstMoveI()][next.getFirstMoveH()].getPoint().getY();
                    }
                }
            }
        }
        return new Point(bestX, bestY);
    }

    private void initShortSearchGrid() {
        int dim = SHORT_SEARCH_GRID_SPAN * 2 / SHORT_SEARCH_GRID_CELL + 2;
        this.shortSearchGridDim = dim;
        this.shortSearchGrid = new PathPoint[dim][dim];
        double startX = self.getX() - SHORT_SEARCH_GRID_SPAN;
        double startY = self.getY() - SHORT_SEARCH_GRID_SPAN;
        double wizardRadius = self.getRadius();
        for (int i = 0; i < dim; i++) {
            for (int h = 0; h < dim; h++) {
                double x = startX + i * SHORT_SEARCH_GRID_CELL;
                double y = startY + h * SHORT_SEARCH_GRID_CELL;
                shortSearchGrid[i][h] = new PathPoint(i, h, x, y);
                if (x < wizardRadius || x > world.getWidth() - wizardRadius || y < wizardRadius ||
                        y > world.getHeight() - wizardRadius) {
                    shortSearchGrid[i][h].setReachable(false);
                }
            }
        }
        for (Unit unit : world.allUnits()) {
            if (unit instanceof Bonus || unit instanceof Projectile) {
                continue;
            }
            if (unit.getId() == self.getId()) {
                continue;
            }
            CircularUnit cunit = (CircularUnit) unit;
            if (hypot(self.getX() - cunit.getX(), self.getY() - cunit.getY()) >
                    SHORT_SEARCH_GRID_SPAN * Math.sqrt(2) + cunit.getRadius() + wizardRadius) {
                continue;
            }
            for (int i = 0; i < dim; i++) {
                for (int h = 0; h < dim; h++) {
                    if (!shortSearchGrid[i][h].isReachable()) {
                        continue;
                    }
                    Point p = shortSearchGrid[i][h].getPoint();
                    double radius = cunit.getRadius() +
                            (cunit instanceof Minion || cunit instanceof Building ? UNSTATIC_OBJECTS_RADIUS_ADJUST : 0);
                    if (hypot(p.getX() - cunit.getX(), p.getY() - cunit.getY()) <= radius + wizardRadius) {
                        shortSearchGrid[i][h].setReachable(false);
                        shortSearchGrid[i][h].setIntersectsWith(cunit);
                    }
                }
            }
        }
        for (int i = 0; i < dim; i++) {
            for (int h = 0; h < dim; h++) {
                PathPoint pFrom = shortSearchGrid[i][h];
                if (!pFrom.isReachable()) {
                    continue;
                }
                List<CircularUnit> units = new ArrayList<>();
                for (int j1 = -1; j1 <= 1; j1++) {
                    for (int j2 = -1; j2 <= 1; j2++) {
                        int curI = i + j1;
                        int curH = h + j2;
                        if (curI < 0 || curH < 0 || curI >= dim || curH >= dim) {
                            continue;
                        }
                        if (shortSearchGrid[curI][curH].getIntersectsWith() != null) {
                            units.add(shortSearchGrid[curI][curH].getIntersectsWith());
                        }
                    }
                }
                for (int j1 = 0; j1 <= 1; j1++) {
                    for (int j2 = -1; j2 <= 1; j2++) {
                        if ((j1 == 0 && j2 == 0) || (j1 == 0 && j2 == -1)) {
                            continue;
                        }
                        int curI = i + j1;
                        int curH = h + j2;
                        if (curI < 0 || curH < 0 || curI >= dim || curH >= dim) {
                            continue;
                        }
                        PathPoint pTo = shortSearchGrid[curI][curH];
                        if (!pTo.isReachable()) {
                            continue;
                        }
                        boolean foundIntersect = false;
                        for (CircularUnit unit : units) {
                            if (isLineIntersectsCircle(pFrom.getPoint().getX(),
                                    pTo.getPoint().getX(),
                                    pFrom.getPoint().getY(),
                                    pTo.getPoint().getY(),
                                    unit.getX(),
                                    unit.getY(),
                                    unit.getRadius() + (unit instanceof Minion || unit instanceof Building ?
                                            UNSTATIC_OBJECTS_RADIUS_ADJUST :
                                            0) + wizardRadius)) {
                                foundIntersect = true;
                            }
                        }
                        if (!foundIntersect) {
                            double dist = hypot(pFrom.getPoint().getX() - pTo.getPoint().getX(),
                                    pFrom.getPoint().getY() - pTo.getPoint().getY());
                            pFrom.getNeighbors().put(pTo, dist);
                            pTo.getNeighbors().put(pFrom, dist);
                        }
                    }
                }
            }
        }
        shortSearchGridInitialized = true;
    }

    private Optional<Point> straightLinePath(double fromX, double fromY, double toX, double toY) {
        for (Point shortTo : Arrays.asList(distPoint(fromX, fromY, toX, toY, SHORT_SEARCH_GRID_SPAN),
                distPoint(fromX, fromY, toX, toY, 2 * SHORT_SEARCH_GRID_SPAN))) {
            Unit intersectsWith = findIntersectUnit(fromX, fromY, shortTo.getX(), shortTo.getY());
            if (intersectsWith == null) {
                return Optional.of(shortTo);
            }
            Point intersectPoint = lineCircleIntersection(fromX,
                    shortTo.getX(),
                    fromY,
                    shortTo.getY(),
                    intersectsWith.getX(),
                    intersectsWith.getY());

            double leftRadius = findAroundRadius(0,
                    MAX_ANGLE_RADIUS,
                    intersectsWith,
                    intersectPoint,
                    fromX,
                    fromY,
                    shortTo.getX(),
                    shortTo.getY());
            Point leftSide = distPoint(intersectsWith.getX(),
                    intersectsWith.getY(),
                    intersectPoint.getX(),
                    intersectPoint.getY(),
                    leftRadius);

            if (findIntersectUnit(fromX, fromY, leftSide.getX(), leftSide.getY()) == null &&
                    findIntersectUnit(leftSide.getX(), leftSide.getY(), shortTo.getX(), shortTo.getY()) == null) {
                return Optional.of(leftSide);
            }

            double rightRadius = findAroundRadius(0,
                    -MAX_ANGLE_RADIUS,
                    intersectsWith,
                    intersectPoint,
                    fromX,
                    fromY,
                    shortTo.getX(),
                    shortTo.getY());
            Point rightSide = distPoint(intersectsWith.getX(),
                    intersectsWith.getY(),
                    intersectPoint.getX(),
                    intersectPoint.getY(),
                    rightRadius);
            if (findIntersectUnit(fromX, fromY, rightSide.getX(), rightSide.getY()) == null &&
                    findIntersectUnit(rightSide.getX(), rightSide.getY(), shortTo.getX(), shortTo.getY()) == null) {
                return Optional.of(rightSide);
            }
        }
        return Optional.empty();
    }

    private double findAroundRadius(double rFrom,
                                    double rTo,
                                    Unit intersectsWith,
                                    Point intersectPoint,
                                    double fromX,
                                    double fromY,
                                    double toX,
                                    double toY) {
        double unitR = ((CircularUnit) intersectsWith).getRadius();
        if (intersectsWith instanceof Wizard || intersectsWith instanceof Minion) {
            unitR += UNSTATIC_OBJECTS_RADIUS_ADJUST;
        }
        double r0 = rFrom;
        double r1 = rTo;
        while (Math.abs(r1 - r0) > 1) {
            double rm = (r0 + r1) / 2;
            Point p = distPoint(intersectsWith.getX(),
                    intersectsWith.getY(),
                    intersectPoint.getX(),
                    intersectPoint.getY(),
                    rm);
            boolean firstLineIntersect = isLineIntersectsCircle(fromX,
                    p.getX(),
                    fromY,
                    p.getY(),
                    intersectsWith.getX(),
                    intersectsWith.getY(),
                    unitR + self.getRadius());
            boolean secondLineIntersect = isLineIntersectsCircle(p.getX(),
                    toX,
                    p.getY(),
                    toY,
                    intersectsWith.getX(),
                    intersectsWith.getY(),
                    unitR + self.getRadius());
            if (p.getX() < 0 || p.getY() < 0 || p.getX() > world.getWidth() || p.getY() > world.getHeight() ||
                    firstLineIntersect || secondLineIntersect ||
                    intersectsWith.getDistanceTo(p.getX(), p.getY()) <= unitR + self.getRadius()) {
                r0 = rm;
            } else {
                r1 = rm;
            }
        }
        return r1;
    }

    private Unit findIntersectUnit(double fromX, double fromY, double toX, double toY) {
        for (Unit unit : world.allUnits()) {
            if (unit.getId() == self.getId()) {
                continue;
            }
            if (unit instanceof Projectile || unit instanceof Bonus) {
                continue;
            }
            double unitR = ((CircularUnit) unit).getRadius();
            if (unit instanceof Wizard || unit instanceof Minion) {
                unitR += UNSTATIC_OBJECTS_RADIUS_ADJUST;
            }
            if (isLineIntersectsCircle(fromX, toX, fromY, toY, unit.getX(), unit.getY(), unitR + self.getRadius()) ||
                    unit.getDistanceTo(toX, toY) <= unitR + self.getRadius()) {
                return unit;
            }
        }
        return null;
    }

    private Point distPoint(double fromX, double fromY, double toX, double toY, double dist) {
        double curDist = hypot(fromX - toX, fromY - toY);
        double proportion = dist / curDist;
        double deltaX = (toX - fromX) * proportion;
        double deltaY = (toY - fromY) * proportion;
        return new Point(fromX + deltaX, fromY + deltaY);
    }

    private Point lineCircleIntersection(double x1, double x2, double y1, double y2, double cx, double cy) {
        double a = y1 - y2;
        double b = x2 - x1;
        double c = (x1 - x2) * y1 + (y2 - y1) * x1;
        double xi = (b * (b * cx - a * cy) - a * c) / (a * a + b * b);
        double yi = (a * (-b * cx + a * cy) - b * c) / (a * a + b * b);
        return new Point(xi, yi);
    }

    private boolean isLineIntersectsCircle(double x1, double x2, double y1, double y2, double cx, double cy, double r) {
        double a = y1 - y2;
        double b = x2 - x1;
        double c = (x1 - x2) * y1 + (y2 - y1) * x1;
        double xi = (b * (b * cx - a * cy) - a * c) / (a * a + b * b);
        double yi = (a * (-b * cx + a * cy) - b * c) / (a * a + b * b);
        return isBetween(xi, x1, x2) && isBetween(yi, y1, y2) && hypot(cx - xi, cy - yi) <= r;
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    private boolean isBetween(double v, double v1, double v2) {
        if (v1 > v2) {
            double z = v1;
            v1 = v2;
            v2 = z;
        }
        return v >= v1 && v <= v2;
    }

    private static class BfsPoint {

        private final int i;
        private final int h;
        private final int firstMoveI;
        private final int firstMoveH;
        private final double dist;

        public BfsPoint(int i, int h, int firstMoveI, int firstMoveH, double dist) {
            this.i = i;
            this.h = h;
            this.firstMoveI = firstMoveI;
            this.firstMoveH = firstMoveH;
            this.dist = dist;
        }

        public int getI() {
            return i;
        }

        public int getH() {
            return h;
        }

        public int getFirstMoveI() {
            return firstMoveI;
        }

        public int getFirstMoveH() {
            return firstMoveH;
        }

        public double getDist() {
            return dist;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BfsPoint bfsPoint = (BfsPoint) o;
            return i == bfsPoint.i && h == bfsPoint.h;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, h);
        }
    }

    private static class PathPoint {

        private int i;
        private int h;
        private Point point;
        private boolean isReachable;
        private Map<PathPoint, Double> neighbors;
        private CircularUnit intersectsWith;

        public PathPoint(int i, int h, double x, double y) {
            this.i = i;
            this.h = h;
            this.point = new Point(x, y);
            this.isReachable = true;
            this.neighbors = new HashMap<>();
        }

        public int getI() {
            return i;
        }

        public int getH() {
            return h;
        }

        public Point getPoint() {
            return point;
        }

        public boolean isReachable() {
            return isReachable;
        }

        public Map<PathPoint, Double> getNeighbors() {
            return neighbors;
        }

        public CircularUnit getIntersectsWith() {
            return intersectsWith;
        }

        public void setReachable(boolean reachable) {
            isReachable = reachable;
        }

        public void setIntersectsWith(CircularUnit intersectsWith) {
            this.intersectsWith = intersectsWith;
        }
    }
}
