import model.*;

import java.util.*;

import static java.lang.StrictMath.hypot;

public class PathFinder {
    private static final int DEFAULT_CELL_WIDTH = 100;
    private static final int STATIC_OBJECTS_BOOSTED_RADIUS = DEFAULT_CELL_WIDTH * 2;
    private static final int UNSTATIC_OBJECTS_RADIUS_ADJUST = 3;
    private static final double E = 1e-9;

    private final WorldProxy world;
    private final Game game;
    private final boolean[][] isPassable;
    private final int cellWidth;
    private final int gridN;
    private final int gridM;

    public PathFinder(Wizard self, WorldProxy world, Game game, UnitLocationType unitLocationType) {
        this.world = world;
        this.game = game;
        this.cellWidth = DEFAULT_CELL_WIDTH;
        this.gridN = (int) (world.getWidth() / cellWidth + 1);
        this.gridM = (int) (world.getHeight() / cellWidth + 1);
        this.isPassable = new boolean[gridN][gridM];

        double wizardRadius = game.getWizardRadius();

        for (int i = 0; i * cellWidth <= world.getWidth(); i++) {
            for (int h = 0; h * cellWidth <= world.getHeight(); h++) {
                isPassable[i][h] = true;
                double x = toRealAxis(i);
                double y = toRealAxis(h);
                if (unitLocationType.getLocationType(x, y) == LocationType.FOREST) {
                    isPassable[i][h] = false;
                }
                if (x < wizardRadius || y < wizardRadius || world.getHeight() - y < wizardRadius ||
                        world.getWidth() - x < wizardRadius) {
                    isPassable[i][h] = false;
                }
            }
        }

        for (Unit unit : world.allUnits()) {
            if (!(unit instanceof Tree) && !(unit instanceof Building)) {
                continue;
            }
            double unitR = STATIC_OBJECTS_BOOSTED_RADIUS;
            for (int i = rightCell(unit.getX() - unitR, cellWidth);
                    i <= leftCell(unit.getX() + unitR, cellWidth);
                    i++) {
                for (int h = rightCell(unit.getY() - unitR, cellWidth);
                        h <= leftCell(unit.getY() + unitR, cellWidth);
                        h++) {
                    if (i < 0 || h < 0 || i >= gridN || h >= gridM) {
                        continue;
                    }
                    if (unit.getDistanceTo(i * cellWidth, h * cellWidth) <= unitR) {
                        isPassable[i][h] = false;
                    }
                }
            }
        }
    }

    public Movement findPath(Wizard wizard, double x, double y) {
        Point straightPoint = straightPoint(wizard.getX(), wizard.getY(), x, y);
        return findOptimalMovement(wizard, straightPoint.getX(), straightPoint.getY());
    }

    private double toRealAxis(int index) {
        return index * cellWidth;
    }

    private static int rightCell(double value, int cellWidth) {
        int ceil = (int) Math.ceil(value);
        if (ceil % cellWidth == 0) {
            return ceil / cellWidth;
        } else {
            return ceil / cellWidth + 1;
        }
    }

    private static int leftCell(double value, int cellWidth) {
        int floor = (int) Math.floor(value);
        if (floor % cellWidth == 0) {
            return floor / cellWidth;
        } else {
            return floor / cellWidth - 1;
        }
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

    private Point straightPoint(double fromX, double fromY, double toX, double toY) {
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
                    if (firstMoveI == -1) {
                        firstMoveI = nextI;
                        firstMoveH = nextH;
                        dist = hypot(fromX - toRealAxis(nextI), fromY - toRealAxis(nextH));
                    }
                    BfsPoint nextPoint = new BfsPoint(nextI, nextH, firstMoveI, firstMoveH, dist);
                    if (nextI < 0 || nextI >= gridN || nextH < 0 || nextH >= gridM ||
                            (visitedPoints.containsKey(nextPoint) && visitedPoints.get(nextPoint) <= dist) ||
                            !isPassable[nextI][nextH]) {
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
}
