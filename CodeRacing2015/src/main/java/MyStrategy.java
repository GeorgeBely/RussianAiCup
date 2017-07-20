import model.*;

import java.util.*;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {

    public static Map<Long, Map<String, Object>> carParams = new HashMap<>();

    public static Car self;
    public static World world;
    public static Game game;
    public static Move move;

    public Double speedModule;
    public Point nextWaypointPosition;
    public Point selfPoint;


    @Override
    public void move(Car self, World world, Game game, Move move) {
        MyStrategy.self = self;
        MyStrategy.move = move;
        MyStrategy.game = game;
        MyStrategy.world = world;
        StrategyHelper.initParams(self, game, world);
        WorldHelper.initParams(self, game, world);
        initCarParams();

        if (world.getTick() == 0) {
            WorldHelper.initWorld();
        }
        if (self.isFinishedTrack()) {
            return;
        }
        WorldHelper.updateWorld();
        WorldHelper.initWay();
        speedModule = Math.abs(hypot(self.getSpeedX(), self.getSpeedY()));
        nextWaypointPosition = StrategyHelper.getNextWaypointPosition();
        selfPoint = WorldHelper.getSelfPoint();

//        useTurnTile();
        updateCarPartMove();

        setEnginePowerAndAngle();
        useOilCanister();
        useThrowProjectile();
        useNitro();
    }

    public void updateCarPartMove() {
        if ((Integer) getParam("carPartMoveStep") == 0) {
            if ((int) getParam("step") == 0 && StrategyHelper.getAngleToMove(self, nextWaypointPosition.getX(), nextWaypointPosition.getY()) > 135) {
                setParam("carPartMove", (int) getParam("carPartMove") * -1);
                setParam("carPartMoveStep", 200);
                setParam("twoStep", 200);
                setParam("turnBrake", true);
            }
        } else {
            setParam("carPartMoveStep", (int) getParam("carPartMoveStep") - 1);
        }
    }

    public void useTurnTile() {
        Point turnTile = (Point) MyStrategy.getParam("turnTile");
        if (turnTile != null) {
            Double turnTileX = (turnTile.getX() + 0.5) * game.getTrackTileSize();
            Double turnTileY = (turnTile.getY() + 0.5) * game.getTrackTileSize();
            if (turnTile.equals(MyStrategy.getParam("currentPoint"))) {
                if (self.getDistanceTo(turnTileX, turnTileY) < game.getTrackTileSize()/2) {
                    MyStrategy.setParam("twoStep", 200);
                    MyStrategy.setParam("turnTile", null);
                } else {
                    nextWaypointPosition = new Point(turnTileX, turnTileY);
                }
            } else if (((Point) MyStrategy.getParam("currentPoint")).getChild().contains(turnTile)){
                WorldHelper.way.add(0, turnTile);
                WorldHelper.way.add(1, (Point) MyStrategy.getParam("currentPoint"));
                nextWaypointPosition = new Point(turnTileX, turnTileY);
            }
        }
    }

    public void initCarParams() {
        if (!carParams.containsKey(self.getId())) {
            Map<String, Object> params = new HashMap<>();
            params.put("step", 0);
            params.put("twoStep", 0);
            params.put("carPartMove", 1);
            params.put("nitroStep", 0);
            params.put("carPartMove", 1);
            params.put("carPartMoveStep", 0);

            carParams.put(self.getId(), params);
        }
    }

    public static Object getParam(String name) {
        return carParams.get(self.getId()).get(name);
    }

    public static void setParam(String name, Object value) {
        carParams.get(self.getId()).put(name, value);
    }

    public void setEnginePowerAndAngle() {
        double angleToWaypoint = self.getAngleTo(nextWaypointPosition.getX(), nextWaypointPosition.getY());
        if (Math.abs(angleToWaypoint) > Math.PI/2) {
            if (angleToWaypoint > 0)
                angleToWaypoint = Math.PI - angleToWaypoint;
            else
                angleToWaypoint = -1 * (Math.PI + angleToWaypoint);
        }
        move.setWheelTurn(angleToWaypoint * 32.0D / PI);

        Point nextWaypoint = WorldHelper.waypoints.get(0);
        Point nextNextWaypoint = WorldHelper.waypoints.get(1);
        Double nextWaypointX = (nextWaypoint.getX() + 0.5) * game.getTrackTileSize();
        Double nextWaypointY = (nextWaypoint.getY() + 0.5) * game.getTrackTileSize();

        if ((speedModule != 0  && speedModule < 0.1 || (Integer) getParam("step") != 0) && (Integer) getParam("twoStep") == 0 && world.getTick() > 200) {
            setParam("lastPoint", null);
            if ((Integer) getParam("step") == 0) {
                setParam("step", 100);
            }
            move.setEnginePower((Integer) getParam("carPartMove") * -1D);
            move.setWheelTurn(-angleToWaypoint * 32.0D / PI);

            if (move.getWheelTurn() > 0)
                move.setWheelTurn(1);
            else
                move.setWheelTurn(-1);

            setParam("step", (Integer) getParam("step") - 1);
            if ((Integer) getParam("step") == 0) {
                setParam("twoStep", 200);
            }
        } else {
            if ((Integer) getParam("twoStep") != 0)
                setParam("twoStep" , (Integer) getParam("twoStep") - 1);

            if (StrategyHelper.isFlipFlop()) {
                if (speedModule > 13) {
                    move.setBrake(true);
                }
                move.setEnginePower((Integer) getParam("carPartMove") * 0.2);
            } else if (StrategyHelper.isNextZigzag(0) && (StrategyHelper.isNextZigzag(1) || StrategyHelper.isNextZigzag(-1))) {
                if (StrategyHelper.getAngleToMove(self, nextWaypointPosition) < 15) {
                    move.setEnginePower((Integer) getParam("carPartMove") * 1D);
                } else {
                    move.setEnginePower((Integer) getParam("carPartMove") * 0.5);
                }
            } else if (self.getDistanceTo(nextWaypointX, nextWaypointY) < game.getTrackTileSize() * 3
                    && !Boolean.TRUE.equals(getParam("isNextBonus"))) {
                if(speedModule > 20 && self.getRemainingOiledTicks() > 0 || speedModule > 30)
                    move.setBrake(true);
                move.setEnginePower((Integer) getParam("carPartMove") * 1D);
            } else {
                move.setEnginePower((Integer) getParam("carPartMove") * 1D);
            }

            int maxSpeed = 15;
            if (self.getDurability() < 0.25 && (Integer) getParam("carPartMove") != -1) {
                maxSpeed = 10;
            }

            if (Objects.equals(selfPoint.getX(), nextWaypoint.getX()) && !Objects.equals(selfPoint.getX(), nextNextWaypoint.getX())) {
                if (Math.abs(self.getSpeedX()) > maxSpeed && StrategyHelper.isNextZigzag(-1) && !StrategyHelper.isNextZigzag(1)) {
                    move.setBrake(true);
                }
            } else if (Objects.equals(selfPoint.getY(), nextWaypoint.getY()) && !Objects.equals(selfPoint.getY(), nextNextWaypoint.getY())) {
                if (Math.abs(self.getSpeedY()) > maxSpeed && StrategyHelper.isNextZigzag(-1) && !StrategyHelper.isNextZigzag(1)) {
                    move.setBrake(true);
                }
            }

            if (nextWaypoint.equals(getParam("lastPoint"))) {
                if (speedModule > 1 && Boolean.TRUE.equals(getParam("turnBrake"))) {
                    move.setBrake(true);
                } else {
                    setParam("turnBrake", false);
                }
            }
        }
    }


    public void useNitro() {
        Point nextWaypoint = WorldHelper.waypoints.get(0);
        Double nextPositionX = (nextWaypoint.getX() + 0.5) * game.getTrackTileSize();
        Double nextPositionY = (nextWaypoint.getY() + 0.5) * game.getTrackTileSize();
        if (self.getNitroChargeCount() > 0 && world.getTick() > 300 && (Integer) getParam("carPartMove") != -1
                && self.getRemainingNitroTicks() < 1 && self.getRemainingOiledTicks() <= 0) {
            if (speedModule > 1 && speedModule < 30 && StrategyHelper.getAngleToMove(self, nextWaypointPosition.getX(), nextWaypointPosition.getY()) < 15) {
                Integer nextX = nextWaypoint.getX().intValue();
                Integer nextY = nextWaypoint.getY().intValue();
                Point nextPoint = WorldHelper.way.get(0);
                if (self.getDistanceTo(nextPositionX, nextPositionY) > game.getTrackTileSize() * 5
                        && (Objects.equals(nextPoint.getY().intValue(), nextY) || Objects.equals(nextPoint.getX().intValue(), nextX))) {
                    move.setUseNitro(true);
//                } else if (WorldHelper.way.size() > 10 && StrategyHelper.isNextZigzag(-1) && StrategyHelper.isNextZigzag(0) && StrategyHelper.isNextZigzag(10)
//                        && Math.abs(nextPoint.getX() - WorldHelper.way.get(10).getX()) + Math.abs(nextPoint.getY() - WorldHelper.way.get(10).getY()) >= 10
//                         && !StrategyHelper.isFlipFlop(-1)) {
//                    if (nitroStep < 50) {
//                        nitroStep++;
//                    } else {
//                        nitroStep = 0;
//                        move.setUseNitro(true);
//                    }
                } else {
                    setParam("nitroStep", 0);
                }
            }
        }
    }


    public void useThrowProjectile() {
        if (self.getProjectileCount() > 0) {
            for (Car car : world.getCars()) {
                boolean haveLet = false;
                Integer carX = (int) (car.getX()/game.getTrackTileSize());
                Integer carY = (int) (car.getY()/game.getTrackTileSize());
                if (CarType.JEEP.equals(self.getType())) {
                    if (selfPoint.getX().intValue() != carX && selfPoint.getY().intValue() != carY) {
                        haveLet = true;
                    }
                    if (!haveLet) {
                        Queue<Point> start = new ArrayDeque<>();
                        start.add(selfPoint);
                        Point carPoint = WorldHelper.worldPoint[carX][carY];
                        List<Point> visited = new ArrayList<>();
                        visited.add(selfPoint);
                        List<List<Point>> list = WorldHelper.getWay(start, carPoint, visited, new ArrayList<>());
                        if (!list.isEmpty() && !list.get(0).isEmpty() && !selfPoint.getChild().contains(carPoint)
                                && self.getDistanceTo(car) / game.getTrackTileSize() <= list.get(0).size()
                                && self.getDistanceTo(car) < game.getTrackTileSize() * 3){
                            haveLet = true;
                        }
                    }
                }
                if (!car.isTeammate() && self.getDistanceTo(car) < game.getTrackTileSize() * 3 && !haveLet
                        && StrategyHelper.getAngleTo(self, car) < 2 && car.getDurability() > 0 && !car.isFinishedTrack()) {
                    move.setThrowProjectile(true);
                }
            }
        }
    }


    public void useOilCanister() {
        if (self.getOilCanisterCount() > 0 && world.getTick() > 300 && (Integer) getParam("carPartMove") != -1 && (int) getParam("step") == 0) {
            for (Car car : world.getCars()) {
                if (car.getDurability() != 0 && !car.isTeammate()) {
                    if (self.getDistanceTo(car) < game.getTrackTileSize() * 2 && StrategyHelper.getAngleToMove(car, self) < 10) {
                        move.setSpillOil(true);
                    }
                    if (!StrategyHelper.isNextZigzag(0) && !StrategyHelper.isNextZigzag(2) && StrategyHelper.isNextZigzag(1)) {
                        move.setSpillOil(true);
                    }
                }
            }
        }
    }
}