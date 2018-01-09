import model.*;

import java.util.*;


/**
 * @author George Beliy on 07-11-17
 */
class WorldService {

    private static final Map<VehicleType, Map<VehicleType, Double>> TYPES_COEFFICIENT = new HashMap<VehicleType, Map<VehicleType, Double>>() {{
        put(VehicleType.FIGHTER, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 0.999);
            put(VehicleType.HELICOPTER, 0.991);
            put(VehicleType.TANK, 1.0);
            put(VehicleType.ARRV, 1.0);
            put(VehicleType.IFV, 1.003);
        }});
        put(VehicleType.HELICOPTER, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 1.05);
            put(VehicleType.HELICOPTER, 0.999);
            put(VehicleType.TANK, 0.993);
            put(VehicleType.ARRV, 0.998);
            put(VehicleType.IFV, 1.004);
        }});
        put(VehicleType.TANK, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 0.99999);
            put(VehicleType.HELICOPTER, 1.005);
            put(VehicleType.TANK, 0.999);
            put(VehicleType.ARRV, 0.998);
            put(VehicleType.IFV, 0.993);
        }});
        put(VehicleType.IFV, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 0.991);
            put(VehicleType.HELICOPTER, 0.997);
            put(VehicleType.TANK, 1.003);
            put(VehicleType.ARRV, 0.998);
            put(VehicleType.IFV, 0.999);
        }});
        put(VehicleType.ARRV, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 0.999999999);
            put(VehicleType.HELICOPTER, 1.008);
            put(VehicleType.TANK, 1.005);
            put(VehicleType.ARRV, 0.999999999);
            put(VehicleType.IFV, 1.004);
        }});
    }};

    private static final Map<VehicleType, Map<VehicleType, Double>> TYPES_COEFFICIENT_MY = new HashMap<VehicleType, Map<VehicleType, Double>>() {{
        put(VehicleType.FIGHTER, new HashMap<VehicleType, Double>() {{
            put(VehicleType.HELICOPTER, 1.0002);
            put(VehicleType.FIGHTER, 1.2);
            put(VehicleType.ARRV, 0.9995);
        }});
        put(VehicleType.HELICOPTER, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 1.5);
            put(VehicleType.HELICOPTER, 1.5);
            put(VehicleType.ARRV, 0.99995);
        }});
        put(VehicleType.TANK, new HashMap<VehicleType, Double>() {{
            put(VehicleType.TANK, 1.5);
        }});
        put(VehicleType.IFV, new HashMap<VehicleType, Double>() {{
            put(VehicleType.TANK, 1.5);
            put(VehicleType.ARRV, 1.5);
            put(VehicleType.IFV, 1.5);
        }});
        put(VehicleType.ARRV, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 1.0);
            put(VehicleType.HELICOPTER, 1.0);
            put(VehicleType.ARRV, 1.5);
            put(VehicleType.TANK, 1.9);
            put(VehicleType.IFV, 1.9);
        }});
    }};


    private static final Map<VehicleType, Map<VehicleType, Integer>> DAMAGE_BY_TYPE = new HashMap<VehicleType, Map<VehicleType, Integer>>() {{
        put(VehicleType.FIGHTER, new HashMap<VehicleType, Integer>() {{
            put(VehicleType.FIGHTER, MyStrategy.game.getFighterAerialDamage() - MyStrategy.game.getFighterAerialDefence() - 20);
            put(VehicleType.HELICOPTER, MyStrategy.game.getFighterAerialDamage() - MyStrategy.game.getHelicopterAerialDefence());
            put(VehicleType.TANK, 0);
            put(VehicleType.IFV, 0);
            put(VehicleType.ARRV, 0);
        }});
        put(VehicleType.HELICOPTER, new HashMap<VehicleType, Integer>() {{
            put(VehicleType.FIGHTER, MyStrategy.game.getHelicopterAerialDamage() - MyStrategy.game.getFighterAerialDefence() - 5);
            put(VehicleType.HELICOPTER, MyStrategy.game.getHelicopterAerialDamage() - MyStrategy.game.getHelicopterAerialDefence());
            put(VehicleType.TANK, MyStrategy.game.getHelicopterGroundDamage() - MyStrategy.game.getTankAerialDefence());
            put(VehicleType.IFV, MyStrategy.game.getHelicopterGroundDamage() - MyStrategy.game.getIfvAerialDefence());
            put(VehicleType.ARRV, (MyStrategy.game.getHelicopterGroundDamage() - MyStrategy.game.getArrvAerialDefence())/2);
        }});
        put(VehicleType.TANK, new HashMap<VehicleType, Integer>() {{
            put(VehicleType.FIGHTER, 0);
            put(VehicleType.HELICOPTER, MyStrategy.game.getTankAerialDamage() - MyStrategy.game.getHelicopterGroundDefence());
            put(VehicleType.TANK, MyStrategy.game.getTankGroundDamage() - MyStrategy.game.getTankGroundDefence());
            put(VehicleType.IFV, MyStrategy.game.getTankGroundDamage() - MyStrategy.game.getIfvGroundDefence());
            put(VehicleType.ARRV, MyStrategy.game.getTankGroundDamage() - MyStrategy.game.getArrvGroundDefence());
        }});
        put(VehicleType.IFV, new HashMap<VehicleType, Integer>() {{
            put(VehicleType.FIGHTER, MyStrategy.game.getIfvAerialDamage() - MyStrategy.game.getFighterAerialDefence());
            put(VehicleType.HELICOPTER, MyStrategy.game.getIfvAerialDamage() - MyStrategy.game.getHelicopterAerialDefence());
            put(VehicleType.TANK, MyStrategy.game.getIfvGroundDamage() - MyStrategy.game.getTankGroundDefence());
            put(VehicleType.IFV, MyStrategy.game.getIfvGroundDamage() - MyStrategy.game.getIfvGroundDefence());
            put(VehicleType.ARRV, MyStrategy.game.getIfvGroundDamage() - MyStrategy.game.getArrvGroundDefence());
        }});
        put(VehicleType.ARRV, new HashMap<VehicleType, Integer>() {{
            put(VehicleType.FIGHTER, 0);
            put(VehicleType.HELICOPTER, 0);
            put(VehicleType.TANK, 0);
            put(VehicleType.IFV, 0);
            put(VehicleType.ARRV, 0);
        }});
    }};

    private static final Map<VehicleType, Map<VehicleType, Double>> FACTOR_BY_TYPE = new HashMap<VehicleType, Map<VehicleType, Double>>() {{
        put(VehicleType.FIGHTER, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 1.0);
            put(VehicleType.HELICOPTER, 6.0);
            put(VehicleType.TANK, 0.0);
            put(VehicleType.IFV, 1/10.0);
            put(VehicleType.ARRV, 0.0);
        }});
        put(VehicleType.HELICOPTER, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 1/6.0);
            put(VehicleType.HELICOPTER, 1.0);
            put(VehicleType.TANK, 2.0);
            put(VehicleType.IFV, 1/2.0);
            put(VehicleType.ARRV, 5.0);
        }});
        put(VehicleType.TANK, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 0.0);
            put(VehicleType.HELICOPTER, 1/2.0);
            put(VehicleType.TANK, 1.0);
            put(VehicleType.IFV, 4.0);
            put(VehicleType.ARRV, 3.0);
        }});
        put(VehicleType.IFV, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 5.0);
            put(VehicleType.HELICOPTER, 2.0);
            put(VehicleType.TANK, 1/4.0);
            put(VehicleType.IFV, 1.0);
            put(VehicleType.ARRV, 2.0);
        }});
        put(VehicleType.ARRV, new HashMap<VehicleType, Double>() {{
            put(VehicleType.FIGHTER, 0.0);
            put(VehicleType.HELICOPTER, 1/10.0);
            put(VehicleType.IFV, 1/100.0);
            put(VehicleType.TANK, 1/100.0);
            put(VehicleType.ARRV, 1/100.0);
        }});
    }};


    static Map<Long, Vehicle> allVehicle = new HashMap<>();

    static Map<Long, Vehicle> enemyFighters = new HashMap<>();
    static Map<Long, Vehicle> enemyARRVs = new HashMap<>();
    static Map<Long, Vehicle> enemyTank = new HashMap<>();
    static Map<Long, Vehicle> enemyHelicopters = new HashMap<>();
    static Map<Long, Vehicle> enemyIFVs = new HashMap<>();

    static Map<Long, Vehicle> myFighters = new HashMap<>();
    static Map<Long, Vehicle> myARRVs = new HashMap<>();
    static Map<Long, Vehicle> myTank = new HashMap<>();
    static Map<Long, Vehicle> myHelicopters = new HashMap<>();
    static Map<Long, Vehicle> myIFVs = new HashMap<>();

    private static Player enemyPlayer = MyStrategy.world.getOpponentPlayer();


    static Map<Long, Facility> myFaculty = new HashMap<>();
    private static List<Facility> enemyFaculty = new ArrayList<>();
    private static List<Facility> neutralFaculty = new ArrayList<>();


    static void init() {
        updateVehicles();
        updateFaculty();
    }


    private static void updateFaculty() {
        myFaculty = new HashMap<>();
        enemyFaculty = new ArrayList<>();
        neutralFaculty = new ArrayList<>();
        for (Facility facility : MyStrategy.world.getFacilities()) {
            if (MyStrategy.me.getId() == facility.getOwnerPlayerId()) {
                myFaculty.put(facility.getId(), facility);
            } else if (enemyPlayer.getId() == facility.getOwnerPlayerId()) {
                enemyFaculty.add(facility);
            } else {
                neutralFaculty.add(facility);
            }
        }
    }

    private static void updateVehicles() {
        for (Vehicle vehicle : MyStrategy.world.getNewVehicles()) {
            StrategyHelper.updateVehicle(vehicle, getVehiclesByType(vehicle.getType(), vehicle.getPlayerId()));
            allVehicle.put(vehicle.getId(), vehicle);
        }
        for (VehicleUpdate vehicleUpdate : MyStrategy.world.getVehicleUpdates()) {
            Vehicle vehicle = allVehicle.get(vehicleUpdate.getId());
            vehicle = new Vehicle(vehicle, vehicleUpdate);
            if (vehicleUpdate.getDurability() != 0) {
                allVehicle.put(vehicle.getId(), vehicle);
            } else {
                allVehicle.remove(vehicle.getId());
            }

            StrategyHelper.updateVehicle(vehicle, getVehiclesByType(vehicle.getType(), vehicle.getPlayerId()));
        }
    }

    private static Map<Long, Vehicle> getVehiclesByType(VehicleType type, Long playerId) {
        if (playerId != MyStrategy.me.getId()) {
            switch (type) {
                case FIGHTER:
                    return enemyFighters;
                case HELICOPTER:
                    return enemyHelicopters;
                case TANK:
                    return enemyTank;
                case ARRV:
                    return enemyARRVs;
                case IFV:
                    return enemyIFVs;
            }
        } else {
            switch (type) {
                case FIGHTER:
                    return myFighters;
                case HELICOPTER:
                    return myHelicopters;
                case TANK:
                    return myTank;
                case IFV:
                    return myIFVs;
                case ARRV:
                    return myARRVs;
            }
        }
        return null;
    }

    static void updatePP(Unit unit, Group group, Double[][] pp, double defFactor, int range, boolean staticFactor) {
        if (defFactor != 1.0) {
            if (group != null && group.isCurrentGroup(unit)) {
                return;
            }

            int x = (int) unit.getX() / StrategyHelper.CELL_WIDTH;
            int y = (int) unit.getY() / StrategyHelper.CELL_WIDTH;


            if (unit instanceof Vehicle && VehicleType.ARRV.equals(((Vehicle) unit).getType())
                    && ((Vehicle) unit).getPlayerId() == MyStrategy.me.getId() && group != null) {
                if (group.getDurability() < group.getMaxDurability() * 0.80) {
                    pp[x][y] = pp[x][y] * (group.getDurability()/group.getMaxDurability());
                }
                return;
            }
            range = range != -1 ? range : defFactor > 1 ? 2 : (flag ? 5 : 15);
            for (int i = -range; i < range + 1; i++) {
                for (int j = -range; j < range + 1; j++) {
                    int xPP = x + i;
                    int yPP = y + j;
                    if (xPP >= 0 && xPP < 32 && yPP >= 0 && yPP < 32) {
                        if (!staticFactor) {
                            int max = Math.max(Math.abs(i), Math.abs(j));
                            double factor = defFactor;
                            if (defFactor < 1) {
                                int pos = max;
                                int fac = max;
                                if (max > 25) {
                                    pos = 8;
                                    fac = 26;
                                } else if (max > 15) {
                                    pos = 7;
                                    fac = 16;
                                } else if (max > 5) {
                                    pos = 6;
                                    fac = 6;
                                }
                                if (String.valueOf(factor).length() > 5) {
                                    factor = Math.floor(factor * 1000) / 1000;
                                }
                                factor = 1 - Math.pow(10, -pos) + factor * Math.pow(10, -pos) + (max - fac) * Math.pow(10, -(pos+1+String.valueOf(factor).length()-2));
                            } else if (defFactor > 1) {
                                factor = (defFactor - 1) * Math.pow(10, -max) + 1;
                            }
                            pp[xPP][yPP] = pp[xPP][yPP] * factor;
                        } else {
                            pp[xPP][yPP] = defFactor;
                        }
                    }
                }
            }
        }
    }

    private static void updatePPForVehicles(Group group, Map<Long, Vehicle> map, Map<VehicleType, Map<VehicleType, Double>> coff) {
        map.values().forEach(vehicle -> {
            double defFactor = coff.get(group.type).get(vehicle.getType());
            if (FACTOR_BY_TYPE.get(group.type).get(vehicle.getType()) != 0) {
                int groupSize = group.vehicles.size();
                int mapSize = map.size();
//                int i = (int) vehicle.getX() / StrategyHelper.CELL_WIDTH;
//                int j = (int) vehicle.getY() / StrategyHelper.CELL_WIDTH;
//                int mapSize = getVehiclesByRectangleWithPlayer((int) ((i - 1.5) * StrategyHelper.CELL_WIDTH),
//                        (int) ((i + 2.5) * StrategyHelper.CELL_WIDTH - 1),
//                        (int) ((j - 1.5) * StrategyHelper.CELL_WIDTH),
//                        (int) ((j + 2.5) * StrategyHelper.CELL_WIDTH - 1), enemyPlayer, vehicle.getType()).size();

                mapSize /= FACTOR_BY_TYPE.get(group.type).get(vehicle.getType());

                if (defFactor > 1 ? groupSize > mapSize : groupSize < mapSize) {
                    double max = Math.max(groupSize, mapSize);
                    double min = Math.min(groupSize, mapSize);
                    defFactor = 1 + ((min - max / 2.0) / (max / 2.0) * (defFactor - 1.0));
                }
            } else {
                defFactor = 0.999999999;
            }
            updatePP(vehicle, group, group.potentialPoints, defFactor, -1, false);
        });
    }

    private static boolean flag;

    private static boolean isBigDistance(Collection<Vehicle> vehicles) {
        if (flag)
            return true;
        Point minMin = StrategyHelper.min(vehicles);
        Point maxMax = StrategyHelper.max(vehicles);
        if (maxMax.x - minMin.x > 256 && maxMax.y - minMin.y > 256) {
            flag = true;
            return true;
        }
        return false;
    }

    static void updatePP(Group group) {
        if (isBigDistance(enemyFighters.values()) || isBigDistance(enemyARRVs.values()) || isBigDistance(enemyHelicopters.values())
                || isBigDistance(enemyTank.values()) || isBigDistance(enemyIFVs.values())) {
            updatePPForVehicles(group, enemyFighters, TYPES_COEFFICIENT);
            updatePPForVehicles(group, enemyHelicopters, TYPES_COEFFICIENT);
            updatePPForVehicles(group, enemyTank, TYPES_COEFFICIENT);
            updatePPForVehicles(group, enemyIFVs, TYPES_COEFFICIENT);
            updatePPForVehicles(group, enemyARRVs, TYPES_COEFFICIENT);
        } else {
            updatePPNew(group);
        }

        if (TYPES_COEFFICIENT_MY.get(group.type).containsKey(VehicleType.FIGHTER)) {
            myFighters.values().forEach(vehicle -> updatePP(vehicle, group, group.potentialPoints,
                    TYPES_COEFFICIENT_MY.get(group.type).get(vehicle.getType()), 3, false));
        }
        if (TYPES_COEFFICIENT_MY.get(group.type).containsKey(VehicleType.HELICOPTER))
            myHelicopters.values().forEach(vehicle -> updatePP(vehicle, group, group.potentialPoints,
                    TYPES_COEFFICIENT_MY.get(group.type).get(vehicle.getType()), 0, false));

        if (TYPES_COEFFICIENT_MY.get(group.type).containsKey(VehicleType.TANK))
            myTank.values().forEach(vehicle -> updatePP(vehicle, group, group.potentialPoints,
                    TYPES_COEFFICIENT_MY.get(group.type).get(vehicle.getType()), 2, false));

        if (TYPES_COEFFICIENT_MY.get(group.type).containsKey(VehicleType.IFV))
            myIFVs.values().forEach(vehicle -> updatePP(vehicle, group, group.potentialPoints,
                    TYPES_COEFFICIENT_MY.get(group.type).get(vehicle.getType()), 1, false));

        if (TYPES_COEFFICIENT_MY.get(group.type).containsKey(VehicleType.ARRV))
            myARRVs.values().forEach(vehicle -> updatePP(vehicle, group, group.potentialPoints,
                    TYPES_COEFFICIENT_MY.get(group.type).get(vehicle.getType()), 1, false));

        if (VehicleType.ARRV.equals(group.type)) {
            allVehicle.values().stream()
                    .filter(vehicle -> vehicle.getPlayerId() == MyStrategy.me.getId())
                    .filter(vehicle -> !vehicle.getType().equals(VehicleType.ARRV))
                    .forEach(vehicle -> updatePP(vehicle, group, group.potentialPoints, TYPES_COEFFICIENT_MY.get(group.type).get(vehicle.getType()), 1, false));
        }

        updatePP(new Point(0, 0), null, group.potentialPoints, 1.5, 0, false);
        updatePP(new Point(31 * StrategyHelper.CELL_WIDTH, 31 * StrategyHelper.CELL_WIDTH),
                null, group.potentialPoints, 1.5, 0, false);

        if (!group.isAir()) {
            for (Facility facility : enemyFaculty) {
                if (WorldService.getVehiclesByRectangleWithPlayer((int) facility.getLeft(),
                        (int) (facility.getLeft() + MyStrategy.game.getFacilityWidth()), (int) facility.getTop(),
                        (int) (facility.getTop() + MyStrategy.game.getFacilityHeight()), MyStrategy.me, true, group.groupId).isEmpty()) {
                    updatePP(new Point(facility.getLeft()+48, facility.getTop()+48),
                            null, group.potentialPoints, group.type.equals(VehicleType.ARRV) ? 0.1 : 0.5,
                            group.type.equals(VehicleType.ARRV) ? 31 : 31, false);
                }
            }
            for (Facility facility : neutralFaculty) {
                if (WorldService.getVehiclesByRectangleWithPlayer((int) facility.getLeft(),
                        (int) (facility.getLeft() + MyStrategy.game.getFacilityWidth()), (int) facility.getTop(),
                        (int) (facility.getTop() + MyStrategy.game.getFacilityHeight()), MyStrategy.me, true, group.groupId).isEmpty()) {
                    updatePP(new Point(facility.getLeft()+48, facility.getTop()+48),
                            null, group.potentialPoints, group.type.equals(VehicleType.ARRV) ? 0.1 : 0.5,
                            group.type.equals(VehicleType.ARRV) ? 31 : 31, false);
                }
            }
        }
        for (Facility facility : myFaculty.values()) {
            updatePP(new Point(facility.getLeft(), facility.getTop()),
                    null, group.potentialPoints, 1.1,1, false);
            updatePP(new Point(facility.getLeft()+48, facility.getTop()),
                    null, group.potentialPoints, 1.1,1, false);
            updatePP(new Point(facility.getLeft(), facility.getTop()+48),
                    null, group.potentialPoints, 1.1,1, false);
            updatePP(new Point(facility.getLeft()+48, facility.getTop()+48),
                    null, group.potentialPoints, 1.1,1, false);
        }
        if (MyStrategy.me.getNextNuclearStrikeY() != -1)
            updatePP(new Point(MyStrategy.me.getNextNuclearStrikeX(), MyStrategy.me.getNextNuclearStrikeY()),
                    null, group.potentialPoints, 5, 1, true);
        if (enemyPlayer.getNextNuclearStrikeX() != -1) {
            updatePP(new Point(enemyPlayer.getNextNuclearStrikeX(), enemyPlayer.getNextNuclearStrikeY()),
                    null, group.potentialPoints, 5, 1, true);
        }

        updatePP(group.getCenterGroup(), group, group.potentialPoints, 1.1, 1, false);

        updatePP(new Point(512, 512), group, group.potentialPoints, 0.99999999999999999, 16, false);
    }

    private static void updatePPNew(Group group) {
        for (int i = 0; i < StrategyHelper.CELL_WIDTH; i++) {
            for (int j = 0; j < StrategyHelper.CELL_WIDTH; j++) {
                Map<Long, Vehicle> vehicles = getVehiclesByRectangleWithPlayer(i * StrategyHelper.CELL_WIDTH,
                        (i+1) * StrategyHelper.CELL_WIDTH - 1, j * StrategyHelper.CELL_WIDTH,
                        (j+1) * StrategyHelper.CELL_WIDTH - 1, enemyPlayer, null, null);
                if (!vehicles.isEmpty()) {
                    double r =
                            group.vehicles.size() > 100 ? 2.0 :
                                    1.5;
                    vehicles = getVehiclesByRectangleWithPlayer((int) ((i - r) * StrategyHelper.CELL_WIDTH),
                            (int) ((i + 1.0 + r) * StrategyHelper.CELL_WIDTH - 1),
                            (int) ((j - r) * StrategyHelper.CELL_WIDTH),
                            (int) ((j + 1.0 + r) * StrategyHelper.CELL_WIDTH - 1), enemyPlayer, null, null);
                    updatePPForVehiclesNew(group, vehicles.values(), i, j);
                }
            }
        }
    }

    private static void updatePPForVehiclesNew(Group group, Collection<Vehicle> enemyVehicles, int i, int j) {
        double factor = 1.0;

        if (enemyVehicles.size() != 0) {
            double allEnemyDamage = 0.0;
            double allMeDamage = 0.0;
            int allDurabilityMe = 0;
            int allDurabilityEnemy = 0;
            for (Vehicle vehicle : enemyVehicles) {
                allEnemyDamage += DAMAGE_BY_TYPE.get(vehicle.getType()).get(group.type);
                allMeDamage += DAMAGE_BY_TYPE.get(group.type).get(vehicle.getType());
                allDurabilityEnemy += vehicle.getDurability();
            }
            if (group.withArrv)
                allMeDamage /= 2;
            for (Vehicle vehicle : group.vehicles.values()) {
                allDurabilityMe += vehicle.getDurability();
            }
            double avgDamagEnemy = allEnemyDamage / enemyVehicles.size();
            double avgDamagMe = allMeDamage / enemyVehicles.size();
            if (avgDamagEnemy != 0 && avgDamagMe != 0) {
                allEnemyDamage = avgDamagEnemy * enemyVehicles.size();
                allMeDamage = avgDamagMe * group.vehicles.size();

                double enemyAttackCount = allDurabilityMe / allEnemyDamage;
                double meAttackCount = allDurabilityEnemy / allMeDamage;

                factor = meAttackCount / enemyAttackCount;
                if (factor > 1.5) {
                    factor = 1.5;
                }
                if (factor < 0.5) {
                    factor = 0.5;
                }
            } else if (avgDamagEnemy == 0 && avgDamagMe == 0) {
                factor = 0.99999999999;
            } else if (avgDamagEnemy == 0) {
                factor = 0.5;
            } else {
                factor = 1.5;
            }
        }
        updatePP(new Point(i * StrategyHelper.CELL_WIDTH, j * StrategyHelper.CELL_WIDTH),
                group, group.potentialPoints, factor, -1, false);
        if (factor > 1.0 && !group.type.equals(VehicleType.ARRV)) {
            updatePP(new Point(i * StrategyHelper.CELL_WIDTH, j * StrategyHelper.CELL_WIDTH),
                    group, group.potentialPoints, 0.9999999999999, -1, false);
        }
    }

    static boolean haveEnemyInRange(Point center, Integer range) {
        return !getVehiclesByRectangleWithPlayer(center.x - range, center.x + range, center.y - range,
                center.y + range, enemyPlayer, null, null).isEmpty();
    }

    static Map<Long, Vehicle> getVehiclesByRectangle(int xL, int xR, int yT, int yB) {
        return getVehiclesByRectangleWithPlayer(xL, xR, yT, yB, null, null,null);
    }

    static Map<Long, Vehicle> getVehiclesByRectangleWithPlayer(int xL, int xR, int yT, int yB, Player player, Boolean withGroup,
                                                               Integer excludeGroup) {
        Map<Long, Vehicle> result = new HashMap<>();

        for (Map.Entry<Long, Vehicle> vehicle : allVehicle.entrySet()) {
            if (withGroup == null ||
                    (withGroup && (vehicle.getValue().getGroups().length != 0 && (excludeGroup == null || !haveGroup(vehicle.getValue().getGroups(), excludeGroup))))
                    || (!withGroup && vehicle.getValue().getGroups().length == 0)) {
                if (player == null || player.getId() == vehicle.getValue().getPlayerId()) {
                    if (vehicle.getValue().getX() > xL && vehicle.getValue().getX() < xR
                            && vehicle.getValue().getY() > yT && vehicle.getValue().getY() < yB) {
                        result.put(vehicle.getKey(), vehicle.getValue());
                    }
                }
            }
        }
        return result;
    }

    private static boolean haveGroup(int[] groups, int group) {
        for (int i : groups) {
            if (i == group) {
                return true;
            }
        }
        return false;
    }
}