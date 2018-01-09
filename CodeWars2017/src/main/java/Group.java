import model.Unit;
import model.Vehicle;
import model.VehicleType;
import model.VehicleUpdate;

import java.util.Map;
import java.util.Random;


/**
 * @author George Beliy on 10-11-17
 */
public class Group {

    Double[][] defaultPotentialPoints;
    Double[][] potentialPoints = new Double[StrategyHelper.CELL_WIDTH][StrategyHelper.CELL_WIDTH];
    Map<Long, Vehicle> vehicles;
    int groupId;
    public VehicleType type;
    boolean withArrv;


    Group(int groupId, Map<Long, Vehicle> vehicles, VehicleType type, boolean withArrv) {
        this.groupId = groupId;
        this.vehicles = StrategyHelper.safeCopyVehicles(vehicles);
        this.type = type;
        this.withArrv = withArrv;
    }

    Group(int groupId, Map<Long, Vehicle> vehicles, VehicleType type) {
        this(groupId, vehicles, type, false);
    }

    void update(VehicleUpdate[] vehicleUpdates) {
        for (VehicleUpdate vehicleUpdate : vehicleUpdates) {
            Vehicle vehicle = vehicles.get(vehicleUpdate.getId());
            if (vehicle != null) {
                vehicle = new Vehicle(vehicle, vehicleUpdate);
                StrategyHelper.updateVehicle(vehicle, vehicles);
            }
        }
    }

    private void initPotentialPoints() {
        if (defaultPotentialPoints == null) {
            defaultPotentialPoints = new Double[StrategyHelper.CELL_WIDTH][StrategyHelper.CELL_WIDTH];
            StrategyHelper.initDefaultPP(this);
        }
        StrategyHelper.clearPP(this);
        WorldService.updatePP(this);
    }

    Double getVisionRange() {
        return StrategyHelper.VISION_RANGE_BY_TYPE.get(type);
    }

    Point getPriorityPoint() {
        initPotentialPoints();
        return getPriorityPoint(getVisionRange());
    }

    Point getPriorityPoint(Double range) {
        return StrategyHelper.getPriorityPoint(potentialPoints, getCenterGroup(), range, 1.0);
    }

    Point getCenterGroup() {
        return StrategyHelper.center(vehicles.values());
    }

    Point getScalePoint() {
        int i = new Random().nextInt(7);
        switch (i) {
            case 0: return StrategyHelper.min(vehicles.values());
            case 1: return StrategyHelper.maxXminY(vehicles.values());
            case 2: return StrategyHelper.minXmaxY(vehicles.values());
        }
        return getCenterGroup();

//        Point center = getCenterGroup();
//        Point minMin = new Point(center.x - 50 < 0 ? 0 : center.x - 50, center.y - 50 < 0 ? 0 : center.y - 50);
//        Point maxMax = new Point(center.x + 50 > 1023 ? 1023 : center.x + 50, center.y + 50 > 1023 ? 1023 : center.y + 50);
//        Point minMax = new Point(center.x - 50 < 0 ? 0 : center.x - 50, center.y + 50 < 1023 ? 1023 : center.y + 50);
//        Point maxMin = new Point(center.x + 50 > 1023 ? 1023 : center.x + 50, center.y - 50 < 0 ? 0 : center.y - 50);
//        Point maxX = new Point(center.x + 50 > 1023 ? 1023 : center.x + 50, center.y);
//        Point minX = new Point(center.x - 50 < 0 ? 0 : center.x - 50, center.y);
//        Point minY = new Point(center.x, center.y - 50 < 0 ? 0 : center.y - 50);
//        Point maxY = new Point(center.x, center.y + 50 > 1023 ? 1023 : center.y + 50);
//
//
//        int i = new Random().nextInt(10);
//        switch (i) {
//            case 0: return minMin;
//            case 1: return maxMax;
//            case 2: return minMax;
//            case 3: return maxMin;
//            case 4: return minX;
//            case 5: return minY;
//            case 6: return maxX;
//            case 7: return maxY;
//        }
//        return center;
    }

    Integer getMaxDurability() {
        return vehicles.values().stream().map(Vehicle::getMaxDurability).reduce((v1, v2) -> v1 + v2).orElse(0);
    }

    Integer getDurability() {
        return vehicles.values().stream().map(Vehicle::getDurability).reduce((v1, v2) -> v1 + v2).orElse(0);
    }

    boolean isEmpty() {
        return vehicles.isEmpty();
    }

    boolean isAir() {
        return VehicleType.FIGHTER.equals(type) || VehicleType.HELICOPTER.equals(type);
    }

    boolean isCurrentGroup(Unit unit) {
        if (unit instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) unit;
            if (MyStrategy.me.getId() == vehicle.getPlayerId()) {
                for (Integer group : vehicle.getGroups()) {
                    if (group == groupId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void printArray() {
        printArray(potentialPoints, getCenterGroup());
    }

    private static void printArray(Double[][] pp, Point center) {
        int centerX = center.x/32;
        int centerY = center.y/32;
        for (int i = 0; i < pp.length; i++) {
            System.out.print("[");
            for (int j = 0; j < pp.length; j++ ) {
                System.out.printf(", %.9f", pp[j][i]);
                if (i == centerY && j == centerX) {
                    System.out.print("|||");
                }
            }
            System.out.println("]");
        }
    }

}