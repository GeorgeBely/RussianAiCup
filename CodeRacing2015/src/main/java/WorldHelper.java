import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class WorldHelper {

    public static World world;
    public static Car self;
    public static Game game;

    public static Point[][] worldPoint;
    public static List<Point> defaultNextWaypoints;
    public static boolean haveUnknownTile;

    public static int defaultNextWaypointsIndex = 0;
    public static Point lastDefaultWaypoint;

    public static List<Point> waypoints;
    public static int nextWaypointsIndex;
    public static List<Point> way;


    public static void initParams(Car self, Game game, World world) {
        WorldHelper.world = world;
        WorldHelper.self = self;
        WorldHelper.game = game;
    }

    public static void initWorld() {
        worldPoint = new Point[world.getWidth()][world.getHeight()];
        for (int i = 0; i < world.getHeight(); i++) {
            for (int j = 0; j < world.getWidth(); j++) {
                if (!TileType.EMPTY.equals(world.getTilesXY()[j][i])) {
                    worldPoint[j][i] = new Point(j, i, world.getTilesXY()[j][i]);
                } else {
                    worldPoint[j][i] = new Point(TileType.EMPTY);
                }
                if (TileType.UNKNOWN.equals(world.getTilesXY()[j][i])) {
                    haveUnknownTile = true;
                }
            }
        }
        updateChild();

        defaultNextWaypoints = new ArrayList<>();
        for (int j = 0; j < world.getWaypoints().length; j++) {
            defaultNextWaypoints.add(worldPoint[world.getWaypoints()[j][0]][world.getWaypoints()[j][1]]);
        }
    }

    public static void updateChild() {
        for (int j = 0; j < world.getWidth(); j++) {
            for (int i = 0; i < world.getHeight(); i++) {
                worldPoint[j][i].setChild(new ArrayList<>());
            }
        }
        for (int j = 0; j < world.getWidth(); j++) {
            for (int i = 0; i < world.getHeight(); i++) {
                if (worldPoint[j][i].getX() != null) {
                    worldPoint[j][i].setChild(new ArrayList<>());
                    if (j > 0) {
                        if (isAvailableLeft(j, i ,world))
                            worldPoint[j][i].getChild().add(worldPoint[j - 1][i]);
                    }
                    if (j < world.getWidth() - 1) {
                        if (isAvailableRight(j, i, world))
                            worldPoint[j][i].getChild().add(worldPoint[j + 1][i]);
                    }
                    if (i < world.getHeight() - 1) {
                        if (isAvailableBottom(j, i, world))
                            worldPoint[j][i].getChild().add(worldPoint[j][i + 1]);
                    }
                    if (i > 0) {
                        if (isAvailableTop(j, i, world))
                            worldPoint[j][i].getChild().add(worldPoint[j][i - 1]);
                    }
                }
            }
        }
    }

    public static void updateWorld() {
        if (WorldHelper.haveUnknownTile) {
            for (int i = 0; i < world.getHeight(); i++) {
                for (int j = 0; j < world.getWidth(); j++) {
                    if (!TileType.UNKNOWN.equals(world.getTilesXY()[j][i]) && TileType.UNKNOWN.equals(worldPoint[j][i].getType())) {
                        worldPoint[j][i].setType(world.getTilesXY()[j][i]);
                        if (TileType.EMPTY.equals(world.getTilesXY()[j][i])) {
                            worldPoint[j][i].setX(null);
                            worldPoint[j][i].setY(null);
                        }
                    }
                }
            }
            updateChild();
        }
    }


    public static void initWay() {
        Integer selfX = new Double(self.getX()/game.getTrackTileSize()).intValue();
        Integer selfY = new Double(self.getY()/game.getTrackTileSize()).intValue();

        if (MyStrategy.getParam("currentPoint") == null) {
            MyStrategy.setParam("currentPoint", worldPoint[selfX][selfY]);
        }
        if (!worldPoint[selfX][selfY].equals(MyStrategy.getParam("currentPoint"))) {
            MyStrategy.setParam("lastPoint", MyStrategy.getParam("currentPoint"));
        }
        MyStrategy.setParam("currentPoint", worldPoint[selfX][selfY]);

        Point start = worldPoint[selfX][selfY];

        List<Point> wayTmp = new ArrayList<>();
        clearPath();
        if (lastDefaultWaypoint != null && !worldPoint[self.getNextWaypointX()][self.getNextWaypointY()].equals(lastDefaultWaypoint)) {
            if (defaultNextWaypointsIndex < defaultNextWaypoints.size() - 1)
                defaultNextWaypointsIndex++;
            else
                defaultNextWaypointsIndex = 0;
        }
        Point nextWaypoints = worldPoint[self.getNextWaypointX()][self.getNextWaypointY()];
        lastDefaultWaypoint = nextWaypoints;
        int indexW = self.getNextWaypointIndex();
        Point nextNextWaypoints;
        Point next2NextWaypoints;
        if (defaultNextWaypoints.size() - 1 > indexW) {
            nextNextWaypoints = defaultNextWaypoints.get(indexW + 1);
            if (defaultNextWaypoints.size() - 2 > indexW) {
                next2NextWaypoints = defaultNextWaypoints.get(indexW + 2);
            } else {
                next2NextWaypoints = defaultNextWaypoints.get(0);
            }
        } else {
            nextNextWaypoints = defaultNextWaypoints.get(0);
            next2NextWaypoints = defaultNextWaypoints.get(1);
        }

        List<List<Point>> result = new ArrayList<>();

        List<List<Point>> next = getAllWay(start, nextWaypoints);
        int minSize = 0;
        List<Point> minList = new ArrayList<>();
        for (List<Point> list : next) {
            if (minSize == 0 || list.size() < minSize) {
                minSize = list.size();
                minList = list;
            }
        }
        if (minSize < 20 && getWaypoints(minList).size() < 3) {
            List<List<Point>> next2 = getAllWay(nextWaypoints, nextNextWaypoints);
            List<List<Point>> concat = concatWay(next, next2, nextWaypoints);

            int minSize2 = 0;
            List<Point> minList2 = new ArrayList<>();
            for (List<Point> list : next2) {
                if (minSize2 == 0 || list.size() < minSize2) {
                    minSize2 = list.size();
                    minList2 = list;
                }
            }
            List<Point> allMinList = new ArrayList<>();
            allMinList.addAll(minList);
            allMinList.add(nextWaypoints);
            allMinList.addAll(minList2);
            if (minSize + minSize2 < 20 && getWaypoints(allMinList).size() < 3) {
                List<List<Point>> next3 = getAllWay(nextNextWaypoints, next2NextWaypoints);

                List<List<Point>> concat2 = concatWay(concat, next3, nextNextWaypoints);
                List<List<Point>> concat3 = concatWay(concat2, new ArrayList<>(), next2NextWaypoints);

                result.addAll(concat3);
            } else {
                result.addAll(concat);
            }
        } else {
            result.addAll(next);
        }
        wayTmp.addAll(getOptimalWay(result));

        way = wayTmp;
        waypoints = new ArrayList<>();
        waypoints.addAll(getWaypoints(way));

//        addTurnTile(0);
//        addTurnTile(1);
//        addTurnTile(2);
//        addTurnTile(3);

        nextWaypointsIndex = 0;
    }

    public static void addTurnTile(int index) {
        if (way.size() > index && way.indexOf(waypoints.get(0)) > index) {
            Point nextTile = way.get(index);
            TileType type = StrategyHelper.getWayTileType(nextTile, way);
            if ((TileType.VERTICAL.equals(type) || TileType.HORIZONTAL.equals(type))
                    && !TileType.HORIZONTAL.equals(nextTile.getType())
                    && !TileType.VERTICAL.equals(nextTile.getType())
                    && (int) MyStrategy.getParam("carPartMove") == -1) {
                Double wpX = (waypoints.get(0).getX() + 0.5) * game.getTrackTileSize();
                Double wpY = (waypoints.get(0).getY() + 0.5) * game.getTrackTileSize();
                if (self.getDistanceTo(wpX, wpY) > game.getTrackTileSize() * (1 + index)) {
                    boolean add = false;
                    for (Point child : nextTile.getChild()) {
                        if (!add && way.indexOf(child) == -1 && !MyStrategy.getParam("currentPoint").equals(child)) {
                            add = true;
                            way.add(index + 1, child);
                            way.add(index + 2, nextTile);
                            MyStrategy.setParam("turnTile", child);
                            waypoints.add(0, nextTile);
                            waypoints.add(1, child);
                        }
                    }
                }
            }
        }
    }

    public static List<List<Point>> getAllWay(Point start, Point end) {
        List<List<Point>> results = new ArrayList<>();

        List<List<Point>> childs = new ArrayList<>();
        for (Point child : start.getChild()) {
            clearPath();
            List<Point> res = new ArrayList<>();
            res.add(child);
            if (child.equals(end)) {
                return new ArrayList<>();
            } else {
                childs.add(res);
            }
        }

        for (List<Point> list : childs) {
            for (Point child : list.get(0).getChild()) {
                if (!child.equals(start)) {
                    clearPath();
                    if (child.equals(end)) {
                        List<Point> res = new ArrayList<>();
                        res.add(list.get(0));
                        results.add(res);
                    } else {
                        Queue<Point> queue = new ArrayDeque<>();
                        queue.add(child);
                        List<Point> visited = new ArrayList<>();
                        visited.add(start);
                        visited.add(list.get(0));
                        List<List<Point>> ress = getWay(queue, end, visited, new ArrayList<>());
                        for (List<Point> list2 : ress) {
                            List<Point> res = new ArrayList<>();
                            res.add(list.get(0));
                            res.add(child);
                            res.addAll(list2);
                            if (res.size() != 1)
                                results.add(res);
                            else {
                                results.addAll(res.get(0).getChild().stream()
                                        .filter(childRes -> childRes.equals(end))
                                        .map(childRes -> res).collect(Collectors.toList()));
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    public static List<List<Point>> concatWay(List<List<Point>> lastWays, List<List<Point>> nextWays, Point waypoint) {
        List<List<Point>> results = new ArrayList<>();

        if (nextWays.isEmpty() && lastWays.isEmpty()) {
            List<Point> res = new ArrayList<>();
            res.add(waypoint);
            results.add(res);
        } else if (nextWays.isEmpty()) {
            for (List<Point> last : lastWays) {
                List<Point> res = new ArrayList<>();
                res.addAll(last);
                res.add(waypoint);
                results.add(res);
            }
        } else if (lastWays.isEmpty()) {
            for (List<Point> next : nextWays) {
                List<Point> res = new ArrayList<>();
                res.add(waypoint);
                res.addAll(next);
                results.add(res);
            }
        } else {
            for (List<Point> last : lastWays) {
                for (List<Point> next : nextWays) {
                    List<Point> res = new ArrayList<>();
                    res.addAll(last);
                    res.add(waypoint);
                    res.addAll(next);
                    results.add(res);
                }
            }
        }
        return results;
    }

    public static List<Point> getOptimalWay(List<List<Point>> allWay) {
        List<Point> result = new ArrayList<>();
        double resultLength = 0;
        for (List<Point> res : allWay) {
            if (result.isEmpty()) {
                result = res;
                resultLength = getWayLength(res);
            } else {
                double length = getWayLength(res);
                if (length < resultLength || (length == resultLength && StrategyHelper.getAngleToPoint(self, result.get(0)) > StrategyHelper.getAngleToPoint(self, res.get(0)))) {
                    result = res;
                    resultLength = length;
                }
            }
        }
        return result;
    }

    public static void clearPath() {
        for (int k = 0; k < world.getWidth(); k++) {
            for (int p = 0; p < world.getHeight(); p++) {
                worldPoint[k][p].setPath(new ArrayList<>());
            }
        }
    }

    public static double getWayLength(List<Point> way) {
        if (way.isEmpty())
            return 0;

        double count = 0;

        for (int i = 0; i < way.size(); i++) {
            if (StrategyHelper.isFlipFlop(way, i)) {
                count += 1;
            } else if (i > 0 && i < way.size() - 1 && StrategyHelper.isNextZigzag(i-1, way) && StrategyHelper.isNextZigzag(i, way) && StrategyHelper.isNextZigzag(i+1, way)
                    && Math.abs(way.get(i).getY() - way.get(i+2).getY()) + Math.abs(way.get(i).getX() - way.get(i+2).getX()) >= 2) {
                count += 0.9;
            } else {
                count += 1;
            }
        }
        List<Point> waypoints = getWaypoints(way);
        Point position = StrategyHelper.getNextWaypointPosition(way.get(0), way, waypoints);

        if ((MyStrategy.getParam("lastPoint") != null && MyStrategy.getParam("lastPoint").equals(way.get(0))) || getSelfPoint().equals(way.get(1)) || way.get(0).equals(way.get(2))
                || (way.size() > 3 && way.get(1).equals(way.get(3)))) {
            if ((Integer) MyStrategy.getParam("carPartMove") == -1) {
                count += 1;
            } else {
                count += 3;
            }
        } else if (StrategyHelper.getAngleToMove(self, position) >= 60) {
            count += 3;
        } else if (StrategyHelper.getAngleToMove(self, position) >= 30){
            count -= 0.5;
        } else {
            count -= 1.5;
        }
        for (Bonus bonus : world.getBonuses()) {
            if (BonusType.PURE_SCORE.equals(bonus.getType()) || (self.getDurability() < 0.25 && BonusType.REPAIR_KIT.equals(bonus.getType()))) {
                Point bonusPoint = worldPoint[(int) (bonus.getX() / game.getTrackTileSize())][(int) (bonus.getY() / game.getTrackTileSize())];
                Integer index = way.indexOf(bonusPoint);
                if (index != -1) {
                    if (!StrategyHelper.isNextZigzag(index - 1, way)) {
                        count -= 0.5;
                    }
                    if (!StrategyHelper.isNextZigzag(index, way)) {
                        count -= 0.5;
                    }
                    count -= 0.5;
                }
            }
        }

        Point nextNextPoint = way.get(1);
        Point selfPoint = WorldHelper.getSelfPoint();
        Point nextPosition  = StrategyHelper.getNextWaypointPosition(way.get(1), way, waypoints);
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
        List<Point> intersection = StrategyHelper.lineCircleIntersection(nextAnglePosition, 80 + self.getHeight() / 2, self, nextPosition);
        if (!intersection.isEmpty() && StrategyHelper.getAngleToMove(self, nextAnglePosition) < 10) {
            count += 0.5;
        }
        return count;
    }

    public static List<List<Point>> getWay(Queue<Point> queue, Point end, List<Point> visited, List<List<Point>> result) {
        Point p = queue.poll();
        if (end.equals(p))
            return new ArrayList<>();

        for (Point child : p.getChild()) {
            if (!visited.contains(child)) {
                if (end.equals(child)) {
                    result.add(p.getPath());
                } else {
                    visited.add(child);
                    List<Point> path = new ArrayList<>();
                    path.addAll(p.getPath());
                    path.add(child);
                    child.setPath(path);
                    queue.offer(child);
                }
            }
        }
        if (queue.peek() != null) {
            getWay(queue, end, visited, result);
        }

        return result;
    }

    public static List<Point> getWaypoints(List<Point> way) {
        List<Point> waypoints = new ArrayList<>();
        Point current = getSelfPoint();
        int index = 0;
        for (Point point : way) {
            if ((!Objects.equals(point.getX(), current.getX()) && !Objects.equals(point.getY(), current.getY())
                    || (index < way.size() - 1 && current.equals(way.get(index + 1))))) {
                if (index != 0)
                    addWaypoints(way.get(index - 1), waypoints);
                addWaypoints(way.get(index), waypoints);
                current = point;
            }
            index++;
        }
        if (way.size() > 2) {
            addWaypoints(way.get(way.size() - 2), waypoints);
        }
        if (!way.isEmpty()) {
            addWaypoints(way.get(way.size() - 1), waypoints);
        }

        return waypoints;
    }


    public static void addWaypoints(Point point, List<Point> waypoints) {
        if (!waypoints.contains(point)) {
            waypoints.add(point);
        }
    }

    public static Point getSelfPoint() {
        Integer selfX = new Double(self.getX()/game.getTrackTileSize()).intValue();
        Integer selfY = new Double(self.getY()/game.getTrackTileSize()).intValue();

        return worldPoint[selfX][selfY];
    }

    public static boolean isAvailableRight(Integer x, Integer y, World world) {
        switch (world.getTilesXY()[x][y]) {
            case VERTICAL: return false;
            case HORIZONTAL: return true;
            case LEFT_TOP_CORNER: return true;
            case RIGHT_TOP_CORNER: return false;
            case LEFT_BOTTOM_CORNER: return true;
            case RIGHT_BOTTOM_CORNER: return false;
            case LEFT_HEADED_T: return false;
            case RIGHT_HEADED_T: return true;
            case TOP_HEADED_T: return true;
            case BOTTOM_HEADED_T: return true;
            case CROSSROADS: return true;
            case UNKNOWN: return true;
        }
        return false;
    }

    public static boolean isAvailableLeft(Integer x, Integer y, World world) {
        switch (world.getTilesXY()[x][y]) {
            case VERTICAL: return false;
            case HORIZONTAL: return true;
            case LEFT_TOP_CORNER: return false;
            case RIGHT_TOP_CORNER: return true;
            case LEFT_BOTTOM_CORNER: return false;
            case RIGHT_BOTTOM_CORNER: return true;
            case LEFT_HEADED_T: return true;
            case RIGHT_HEADED_T: return false;
            case TOP_HEADED_T: return true;
            case BOTTOM_HEADED_T: return true;
            case CROSSROADS: return true;
            case UNKNOWN: return true;
        }
        return false;
    }

    public static boolean isAvailableTop(Integer x, Integer y, World world) {
        switch (world.getTilesXY()[x][y]) {
            case VERTICAL: return true;
            case HORIZONTAL: return false;
            case LEFT_TOP_CORNER: return false;
            case RIGHT_TOP_CORNER: return false;
            case LEFT_BOTTOM_CORNER: return true;
            case RIGHT_BOTTOM_CORNER: return true;
            case LEFT_HEADED_T: return true;
            case RIGHT_HEADED_T: return true;
            case TOP_HEADED_T: return true;
            case BOTTOM_HEADED_T: return false;
            case CROSSROADS: return true;
            case UNKNOWN: return true;
        }
        return false;
    }

    public static boolean isAvailableBottom(Integer x, Integer y, World world) {
        switch (world.getTilesXY()[x][y]) {
            case VERTICAL: return true;
            case HORIZONTAL: return false;
            case LEFT_TOP_CORNER: return true;
            case RIGHT_TOP_CORNER: return true;
            case LEFT_BOTTOM_CORNER: return false;
            case RIGHT_BOTTOM_CORNER: return false;
            case LEFT_HEADED_T: return true;
            case RIGHT_HEADED_T: return true;
            case TOP_HEADED_T: return false;
            case BOTTOM_HEADED_T: return true;
            case CROSSROADS: return true;
            case UNKNOWN: return true;
        }
        return false;
    }

}
