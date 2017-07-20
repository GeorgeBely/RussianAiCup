import model.*;

import java.util.*;

import static java.lang.StrictMath.hypot;

public class StrategyHelper {

    /** Значение угла равное 1 градусу */
    public static final double STRIKE_ANGLE = Math.PI / 180.0D;


    public static World world;
    public static Car self;
    public static Game game;


    public static void initParams(Car self, Game game, World world) {
        StrategyHelper.world = world;
        StrategyHelper.self = self;
        StrategyHelper.game = game;
    }

    public static Point getNextWaypoints() {
        return getNextWaypoints(WorldHelper.waypoints.get(0));
    }

    public static Point getNextWaypoints(Point nextWaypoints) {
        if (!WorldHelper.waypoints.get(0).equals(nextWaypoints))
            return nextWaypoints;

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        Point selfPoint = WorldHelper.getSelfPoint();

        Double nextWaypointX = (nextWaypoints.getX() + 0.5D) * game.getTrackTileSize();
        Double nextWaypointY = (nextWaypoints.getY() + 0.5D) * game.getTrackTileSize();
        Point position = getNextWaypointPosition(nextWaypoints);
        if (Objects.equals(selfPoint.getX(), nextWaypoints.getX())) {
            position.setY(nextWaypointY + nextWaypointY - position.getY());
        } else if (Objects.equals(selfPoint.getY(), nextWaypoints.getY())) {
            position.setX(nextWaypointX + (nextWaypointX - position.getX()));
        }

        int indexWP = WorldHelper.way.indexOf(nextWaypoints);

        if (WorldHelper.way.size() - 1 <= indexWP)
            return nextWaypoints;

        Point nextNextPoint = WorldHelper.way.get(indexWP + 1);
        Point nextPosition  = getNextWaypointPosition(nextNextPoint);
        Point nextAnglePosition;
        if (selfPoint.getX() < nextNextPoint.getX()) {
            if (selfPoint.getY() < nextNextPoint.getY()) {
                nextAnglePosition = new Point(selfPoint.getX() * game.getTrackTileSize() + game.getTrackTileSize(), selfPoint.getY() * game.getTrackTileSize() + game.getTrackTileSize());
            } else {
                nextAnglePosition = new Point(selfPoint.getX() * game.getTrackTileSize() + game.getTrackTileSize(), selfPoint.getY() * game.getTrackTileSize());
            }
        } else {
            if (selfPoint.getY() < nextNextPoint.getY()) {
                nextAnglePosition = new Point(selfPoint.getX() * game.getTrackTileSize(), selfPoint.getY() * game.getTrackTileSize() + game.getTrackTileSize());
            } else {
                nextAnglePosition = new Point(selfPoint.getX() * game.getTrackTileSize(), selfPoint.getY() * game.getTrackTileSize());
            }
        }

        List<Point> intersection = lineCircleIntersection(nextAnglePosition, 80 + self.getHeight()/2, self, nextPosition);

        if (!isFlipFlop() && !isFlipFlop(0) && 50 > (self.getDistanceTo(position.getX(), position.getY())) / speedModule
                && getAngleToMove(self, nextPosition) < 90 && !Boolean.TRUE.equals(MyStrategy.getParam("isNextBonus")) && !Boolean.TRUE.equals(MyStrategy.getParam("isCornet"))
                && (intersection.isEmpty() || (getAngleToMove(self, nextAnglePosition) > 10 || speedModule > 15))
                && !selfPoint.equals(nextNextPoint)
//                && (!isNextZigzag(-1) || isNextZigzag(0) && isNextZigzag(1) && isNextZigzag(2) && isNextZigzag(3))
                ) {
            return getNextWaypoints(nextNextPoint);
        }

        if (!isFlipFlop() && isNextZigzag(-1) && isNextZigzag(0) && self.getDistanceTo(position.getX(), position.getY()) < game.getTrackTileSize()
                && WorldHelper.way.get(0).equals(nextWaypoints) && intersection.isEmpty()) {
            return getNextWaypoints(nextNextPoint);
        }

//        if ((isFlipFlop()))
//            return getNextWaypoints(WorldHelper.waypoints.get(indexWP + 1));
        return nextWaypoints;
    }

    public static Point getNextWaypointPosition() {
        return getNextWaypointPosition(getNextWaypoints());
    }

    public static Point getNextWaypointPosition(Point waypoint) {
        return getNextWaypointPosition(waypoint, WorldHelper.way, WorldHelper.waypoints);
    }

    public static Point getNextWaypointPosition(Point waypoint, List<Point> way, List<Point> waypoints) {
        Double nextWaypointX = (waypoint.getX() + 0.5D) * game.getTrackTileSize();
        Double nextWaypointY = (waypoint.getY() + 0.5D) * game.getTrackTileSize();

        boolean notUseCorner = false;
        MyStrategy.setParam("isNextBonus", false);
        MyStrategy.setParam("isCornet", false);
        double cornerTileOffset = 0.25 * game.getTrackTileSize();

        Unit bonus = getNearestBonus(waypoints.get(0), way);
        boolean importableBonus = false;
        if (bonus != null) {
            Integer bonusX = new Double(bonus.getX() / game.getTrackTileSize()).intValue();
            Integer bonusY = new Double(bonus.getY() / game.getTrackTileSize()).intValue();
            Point bonusPoint = WorldHelper.worldPoint[bonusX][bonusY];
            if (WorldHelper.getSelfPoint().equals(bonusPoint)
                    || waypoint.equals(bonusPoint)) {
                importableBonus = true;
            }
        }
        if (importableBonus) {
            nextWaypointX = bonus.getX();
            nextWaypointY = bonus.getY();
            notUseCorner = true;
            MyStrategy.setParam("isNextBonus", true);
        } else if (isFlipFlop(way)) {
            if (getAngleToPoint(self, way.get(1)) > 30 && self.getDistanceTo(nextWaypointX, nextWaypointY) > game.getTrackTileSize()) {
                cornerTileOffset = -0.25 * game.getTrackTileSize();
            } else {
                cornerTileOffset = 0.3D * game.getTrackTileSize();
            }
        } else if (isNextFlipFlop(0, way) || isNextFlipFlop(1, way)) {
            nextWaypointX = (way.get(0).getX() + 0.5D) * game.getTrackTileSize();
            nextWaypointY = (way.get(0).getY() + 0.5D) * game.getTrackTileSize();
            waypoint = way.get(0);
            cornerTileOffset = -0.25 * game.getTrackTileSize();
            MyStrategy.setParam("isCornet", true);
        } else if (isNextTurnFlipFlop(0, way) || isNextTurnFlipFlop(1, way)) {
            cornerTileOffset = 0.3D * game.getTrackTileSize();
        } else if (isNextZigzag(0, way)) {
            cornerTileOffset = 0.25D * game.getTrackTileSize();
        } else if (self.getDistanceTo(nextWaypointX, nextWaypointY) > game.getTrackTileSize() * 2) {
            Unit obstacle = getNearestObstacle(waypoints.get(0), way);
          /*
            Point selfPoint = WorldHelper.getSelfPoint();
            if (obstacle != null && self.getDistanceTo(obstacle) < 3 * game.getTrackTileSize()) {
                Integer obstacleX = new Double(obstacle.getX()/game.getTrackTileSize()).intValue();
                Integer obstacleY = new Double(obstacle.getY()/game.getTrackTileSize()).intValue();
                if (Objects.equals(selfPoint.getY().intValue(), obstacleY)) {
                    if (nextWaypointY > obstacle.getY()) {
                        nextWaypointY = nextWaypointY + 300;
                    } else {
                        nextWaypointY = nextWaypointY - 300;
                    }
                } else if (Objects.equals(selfPoint.getX().intValue(), obstacleX)) {
                    if (nextWaypointX > obstacle.getX()) {
                        nextWaypointX = nextWaypointX + 300;
                    } else {
                        nextWaypointX = nextWaypointX - 300;
                    }
                }

                notUseCorner = true;
                MyStrategy.setParam("isCornet", true);
            } else*/
            if (bonus != null) {
                nextWaypointX = bonus.getX();
                nextWaypointY = bonus.getY();
                notUseCorner = true;
                MyStrategy.setParam("isNextBonus", true);
            } else {
                nextWaypointX = (way.get(1).getX() + 0.5D) * game.getTrackTileSize();
                nextWaypointY = (way.get(1).getY() + 0.5D) * game.getTrackTileSize();
                cornerTileOffset = -0.25D * game.getTrackTileSize();
                MyStrategy.setParam("isCornet", true);
            }
        } else if (self.getDistanceTo(nextWaypointX, nextWaypointY) > game.getTrackTileSize()) {
            if (bonus != null) {
                nextWaypointX = bonus.getX();
                nextWaypointY = bonus.getY();
                notUseCorner = true;
                MyStrategy.setParam("isNextBonus", true);
            } else {
//                nextWaypointX = (way.get(0).getX() + 0.5D) * game.getTrackTileSize();
//                nextWaypointY = (way.get(0).getY() + 0.5D) * game.getTrackTileSize();
                notUseCorner = true;
            }
        }
        if (!notUseCorner) {
            return getCornerPoint(waypoint, nextWaypointX, nextWaypointY, cornerTileOffset, way, waypoints);
        }

        return new Point(nextWaypointX, nextWaypointY);
    }

    public static Point getCornerPoint(Point nextWaypoint, double nextWaypointX, double nextWaypointY, double cornerTileOffset,
                                       List<Point> way, List<Point> waypoints) {
        Point selfPoint = WorldHelper.getSelfPoint();
        switch (getWayTileType(nextWaypoint, way)) {
            case LEFT_TOP_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                break;
            case RIGHT_TOP_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                break;
            case LEFT_BOTTOM_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                break;
            case RIGHT_BOTTOM_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                break;
            case VERTICAL:
                Point waypoint2 = waypoints.get(0);
                if (Objects.equals(waypoint2.getX(), selfPoint.getX())) {
                    waypoint2 = waypoints.get(1);
                }
                if (selfPoint.getX() < waypoint2.getX()) {
                    nextWaypointX += cornerTileOffset;
                } else {
                    nextWaypointX -= cornerTileOffset;
                }
                break;
            case HORIZONTAL:
                Point waypoint = waypoints.get(0);
                if (Objects.equals(waypoint.getY(), selfPoint.getY())) {
                    if (waypoints.size() == 1) {
                        break;
                    }
                    waypoint = waypoints.get(1);
                }
                if (selfPoint.getY() < waypoint.getY()) {
                    nextWaypointY += cornerTileOffset;
                } else {
                    nextWaypointY -= cornerTileOffset;
                }
                break;
            default:
        }
        return new Point(nextWaypointX, nextWaypointY);
    }

    public static TileType getWayTileType(Point tile, List<Point> way) {
//        TileType tileType = world.getTilesXY()[tile.getX().intValue()][tile.getY().intValue()];
        TileType tileType = world.getTilesXY()[tile.getX().intValue()][tile.getY().intValue()];
        if (way.size() > 2 && TileType.LEFT_HEADED_T.equals(tileType) || TileType.RIGHT_HEADED_T.equals(tileType)
                || TileType.BOTTOM_HEADED_T.equals(tileType) || TileType.TOP_HEADED_T.equals(tileType) || TileType.CROSSROADS.equals(tileType)) {
            Point lastPoint;
            Point nextPoint;
            int wayPointIndex = way.indexOf(tile);
            if (wayPointIndex == 0) {
                lastPoint = (Point) MyStrategy.getParam("currentPoint");
            } else {
                lastPoint = way.get(wayPointIndex - 1);
            }
            if (wayPointIndex == way.size() - 1) {
                nextPoint = way.get(0);
            } else {
                nextPoint = way.get(wayPointIndex + 1);
            }

            if (Objects.equals(lastPoint.getY(), nextPoint.getY())) {
                return TileType.HORIZONTAL;
            } else if (Objects.equals(lastPoint.getX(), nextPoint.getX())) {
                return TileType.VERTICAL;
            } else if (lastPoint.getY() < nextPoint.getY()) {
                if (lastPoint.getX() < nextPoint.getX()) {
                    if (Objects.equals(tile.getY(), nextPoint.getY()))
                        return TileType.LEFT_BOTTOM_CORNER;
                    else
                        return TileType.RIGHT_TOP_CORNER;
                } else {
                    if (Objects.equals(tile.getY(), nextPoint.getY()))
                        return TileType.RIGHT_BOTTOM_CORNER;
                    else
                        return TileType.LEFT_TOP_CORNER;
                }
            } else {
                if (lastPoint.getX() < nextPoint.getX()) {
                    if (Objects.equals(tile.getY(), nextPoint.getY()))
                        return TileType.LEFT_TOP_CORNER;
                    else
                        return TileType.RIGHT_BOTTOM_CORNER;
                } else {
                    if (Objects.equals(tile.getY(), nextPoint.getY()))
                        return TileType.RIGHT_TOP_CORNER;
                    else
                        return TileType.LEFT_BOTTOM_CORNER;
                }
            }
        }
        return tileType;
    }

    public static Unit getNearestObstacle(Point waypoint, List<Point> way) {
        List<Unit> mayObstacle = new ArrayList<>();

        double selfSpeedModule = hypot(self.getSpeedX(), self.getSpeedY());
        for (Car car : world.getCars()) {
            if (!car.isTeammate() && world.getTick() > 300) {
                Integer carX = new Double(car.getX()/game.getTrackTileSize()).intValue();
                Integer carY = new Double(car.getY()/game.getTrackTileSize()).intValue();
                Point carPoint = WorldHelper.worldPoint[carX][carY];
                double speedModule = hypot(car.getSpeedX(), car.getSpeedY());
                if (way.indexOf(carPoint) != -1  && (speedModule * 2 < selfSpeedModule || (self.getAngleTo(car) < 15 && car.getAngleTo(self) < 15))
                        && way.indexOf(carPoint) < way.indexOf(waypoint)) {
                    mayObstacle.add(car);
                }
            }
        }
        for (OilSlick oil : world.getOilSlicks()) {
            Integer oilX = new Double(oil.getX()/game.getTrackTileSize()).intValue();
            Integer oilY = new Double(oil.getY()/game.getTrackTileSize()).intValue();
            if (oilX < world.getWidth() && oilY < world.getHeight()) {
                Point oilPoint = WorldHelper.worldPoint[oilX][oilY];
                if (way.indexOf(oilPoint) != -1 && getAngleToMove(self, oil) < 10 && selfSpeedModule > 10
                        && way.indexOf(oilPoint) < way.indexOf(waypoint)) {
                    mayObstacle.add(oil);
                }
            }
        }

        if (mayObstacle.isEmpty())
            return null;

        Unit res = null;
        for (Unit obstacle : mayObstacle) {
            if (res == null || self.getDistanceTo(res) > self.getDistanceTo(obstacle)) {
                res = obstacle;
            }
        }

        return res;
    }

    public static Unit getNearestBonus(Point waypoint, List<Point> way) {
        List<Unit> mayBonuses = new ArrayList<>();
        Point waypointPosition = new Point(waypoint.getX() * game.getTrackTileSize(), waypoint.getY() * game.getTrackTileSize());
        for (Bonus bonus : world.getBonuses()) {
            Integer bonusX = new Double(bonus.getX()/game.getTrackTileSize()).intValue();
            Integer bonusY = new Double(bonus.getY()/game.getTrackTileSize()).intValue();
            Point bonusPoint = WorldHelper.worldPoint[bonusX][bonusY];
            if (getAngleToMove(self, bonus) < 20
                    && (self.getDistanceTo(waypointPosition.getX(), waypointPosition.getY()) > bonus.getDistanceTo(waypointPosition.getX(), waypointPosition.getY())
                    || BonusType.PURE_SCORE.equals(bonus.getType()))
                    && (WorldHelper.getSelfPoint().equals(bonusPoint) && BonusType.PURE_SCORE.equals(bonus.getType())
                    || (way.indexOf(bonusPoint) != -1 && way.indexOf(bonusPoint) < way.indexOf(waypoint) - 1)
                    || (way.indexOf(bonusPoint) != -1 && way.indexOf(bonusPoint) <= way.indexOf(waypoint) && BonusType.PURE_SCORE.equals(bonus.getType())))) {
                mayBonuses.add(bonus);
            }
        }

        if (mayBonuses.isEmpty())
            return null;

        Unit res = null;
        for (Unit bonus : mayBonuses) {
            if (res == null || self.getDistanceTo(res) > self.getDistanceTo(bonus)) {
                res = bonus;
            }
        }

        return res;
    }


    public static double getAngleToMove(Unit a, Unit b) {
        return getAngleToMove(a, b.getX(), b.getY());
    }

    public static double getAngleToMove(Unit a, Point b) {
        return getAngleToMove(a, b.getX(), b.getY());
    }

    public static double getAngleToPoint(Unit a, Point b) {
        Double bX = (b.getX() + 0.5D) * game.getTrackTileSize();
        Double bY = (b.getY() + 0.5D) * game.getTrackTileSize();
        return getAngleToMove(a, bX, bY);
    }

    public static double getAngleToMove(Unit a, double bx, double by) {
        double angle = Math.abs(a.getAngleTo(bx, by)) / STRIKE_ANGLE;
        if ((int) MyStrategy.getParam("carPartMove") == -1) {
            angle = 180 - angle;
        }
        return angle;
    }

    public static double getAngleTo(Unit a, double x, double y) {
        return  Math.abs(a.getAngleTo(x, y)) / STRIKE_ANGLE;

    }

    public static double getAngleTo(Unit a, Unit b) {
        return getAngleTo(a, b.getX(), b.getY());
    }

    public static Point nextNextWay(int nextWaypointsIndex, int nextIndex, List<Point> way) {
        if (way.size() > nextIndex) {
            int indexW = nextWaypointsIndex;
            if (indexW > way.size() -1 - nextIndex) {
                indexW = way.size() -1 - nextIndex;
            }
            return way.get(indexW + nextIndex);
        }
        return null;
    }

    public static boolean isNextZigzag(int startPoint) {
        return isNextZigzag(startPoint, WorldHelper.way);
    }

    public static boolean isNextZigzag(int startPoint, List<Point> way) {
        Point currentWaypoints;
        Point nextWaypoints;
        if (startPoint == -1) {
            currentWaypoints = (Point) MyStrategy.getParam("lastPoint");
            nextWaypoints = (Point) MyStrategy.getParam("currentPoint");
        } else if (startPoint == 0) {
            currentWaypoints = (Point) MyStrategy.getParam("currentPoint");
            nextWaypoints = nextNextWay(0, 0, way);
        } else {
            currentWaypoints = nextNextWay(startPoint, -1, way);
            nextWaypoints = nextNextWay(startPoint, 1, way);
        }
        Point next2Waypoints = nextNextWay(startPoint, 1, way);

        if (currentWaypoints != null && (startPoint != -1 || !currentWaypoints.equals(way.get(0)))) {
            if (nextWaypoints != null) {
                if (Math.abs(nextWaypoints.getY() - currentWaypoints.getY()) == Math.abs(nextWaypoints.getX() - currentWaypoints.getX())
                        && Math.abs(nextWaypoints.getY() - currentWaypoints.getY()) + Math.abs(nextWaypoints.getX() - currentWaypoints.getX()) < 4) {
                    return true;
                }
            }
            if (next2Waypoints != null) {
                if (Math.abs(next2Waypoints.getY() - currentWaypoints.getY()) == Math.abs(next2Waypoints.getX() - currentWaypoints.getX())
                        && Math.abs(next2Waypoints.getY() - currentWaypoints.getY()) + Math.abs(next2Waypoints.getX() - currentWaypoints.getX()) < 4) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNextFlipFlop(int index, List<Point> way) {
        Point selfPoint = WorldHelper.getSelfPoint();
        return isFlipFlop(way, index)
                && (Objects.equals(selfPoint.getX(), way.get(index).getX()) || Objects.equals(selfPoint.getY(), way.get(index).getY()))
                && (Objects.equals(selfPoint.getX(), way.get(index + 1).getX()) || Objects.equals(selfPoint.getY(), way.get(index + 1).getY()));
    }

    public static boolean isNextTurnFlipFlop(int index, List<Point> way) {
        Point selfPoint = WorldHelper.getSelfPoint();
        return isFlipFlop(way, index)
                && (Objects.equals(selfPoint.getX(), way.get(index).getX()) || Objects.equals(selfPoint.getY(), way.get(index).getY()));
    }

    public static boolean isFlipFlop() {
        return isFlipFlop(-1);
    }

    public static boolean isFlipFlop(List<Point> way) {
        return isFlipFlop(way, -1);
    }

    public static boolean isFlipFlop(int index) {
        return isFlipFlop(WorldHelper.way, index);
    }

    public static boolean isFlipFlop(List<Point> way, int index) {
        if (way.size() > index + 4) {
            Point currentPoint;
            if (index == -1) {
                currentPoint = (Point) MyStrategy.getParam("currentPoint");
            } else {
                currentPoint = way.get(index);
            }
            Point nextWayPoint = way.get(index + 1);
            Point next1WayPoint = way.get(index + 2);
            Point next2WayPoint = way.get(index + 3);

            if ((Objects.equals(currentPoint.getX(), next2WayPoint.getX()) || Objects.equals(currentPoint.getY(), next2WayPoint.getY()))
                    && Math.abs(next2WayPoint.getX() - currentPoint.getX() + next2WayPoint.getY() - currentPoint.getY()) == 1
                    && !next1WayPoint.equals(currentPoint) && !nextWayPoint.equals(next2WayPoint)) {
                return true;
            }
            if (MyStrategy.getParam("lastPoint") != null && !nextWayPoint.equals(MyStrategy.getParam("lastPoint"))
                    && (Objects.equals(((Point) MyStrategy.getParam("lastPoint")).getX(), next1WayPoint.getX()) || Objects.equals(((Point) MyStrategy.getParam("lastPoint")).getY(), next1WayPoint.getY()))
                    && Math.abs(next1WayPoint.getX() - ((Point) MyStrategy.getParam("lastPoint")).getX()) + Math.abs(next1WayPoint.getY() - ((Point) MyStrategy.getParam("lastPoint")).getY()) == 1
                    && !currentPoint.equals(next1WayPoint)) {
                return true;
            }
        }
        return false;
    }


    public static List<Point> lineCircleIntersection(Point circle, double r, Unit a, Point b) {
        return lineCircleIntersection(circle, r, new Point(a.getX(), a.getY()), b);
    }

    /**
     * Рассчитывает точки пересечение отрезка проходящего через окружность.
     *
     * @param circle центр окружности
     * @param r  радиус окружности
     * @param a первая точка отрезка
     * @param b вторая точка отрезка
     * @return точки пересечения
     */
    public static List<Point> lineCircleIntersection(Point circle, double r, Point a, Point b) {
        List<Point> positions = new ArrayList<>();
        double q = Math.pow(circle.getX(), 2) + Math.pow(circle.getY(), 2) - r*r;
        double k = -2.0 * circle.getX();
        double l = -2.0 * circle.getY();

        double z = a.getX() * b.getY() - b.getX()*a.getY();
        double p = a.getY() - b.getY();
        double s = a.getX() - b.getX();

        if (equalDoubles(s, 0.0, 0.001)) {
            s = 0.001;
        }

        double A = s*s + p*p;
        double B = s*s*k + 2.0*z*p + s*l*p;
        double C = q*s*s + z*z + s*l*z;

        double D = B*B - 4.0*A*C;

        if (D > 0.0) {
            if (D < 0.001) {
                double x = -B / (2.0 * A);
                positions.add(new Point(x, (p * x + z) / s));
            } else {
                double x = (-B + Math.sqrt(D)) / (2.0 * A);
                double y = (p * x + z) / s;
                positions.add(new Point(x, y));

                x = (-B - Math.sqrt(D)) / (2.0 * A);
                y = (p * x + z) / s;
                positions.add(new Point(x, y));
            }
        }

        return positions;
    }

    public static boolean equalDoubles(double n1, double n2, double precision_) {
        return (Math.abs(n1-n2) <= precision_);
    }
}