import model.*;

import java.util.HashMap;
import java.util.Map;


/**
 * @author George Beliy on 06-12-17
 */
class FacilityService {

    private static int idGroupCount = 6;

    private static Map<Long, VehicleType> facilityTypes = new HashMap<>();
    static Map<Long, Boolean> facilitySetGroup = new HashMap<>();

    static void init() {
        if (!GroupHelper.groupTanks) {
            return;
        }
        setupProduction();
    }

    private static void setupProduction() {
        for (Facility facility : WorldService.myFaculty.values()) {
            if (facility.getType().equals(FacilityType.VEHICLE_FACTORY)) {
                Map<Long, Vehicle> groupVehicles = WorldService.getVehiclesByRectangleWithPlayer((int) facility.getLeft(),
                        (int) (facility.getLeft() + MyStrategy.game.getFacilityWidth()), (int) facility.getTop(),
                        (int) (facility.getTop() + MyStrategy.game.getFacilityHeight()), MyStrategy.me, true, null);
                if (groupVehicles.isEmpty()) {
                    Map<Long, Vehicle> newVehicles = WorldService.getVehiclesByRectangleWithPlayer((int) facility.getLeft(),
                            (int) (facility.getLeft() + MyStrategy.game.getFacilityWidth()), (int) facility.getTop(),
                            (int) (facility.getTop() + MyStrategy.game.getFacilityHeight()), MyStrategy.me, false, null);
                    if (!facilityTypes.containsKey(facility.getId()) && !facilitySetGroup.containsKey(facility.getId())) {
                        addProductionAction(facility);
                    } else if (newVehicles.size() > 75 && !facilitySetGroup.containsKey(facility.getId())) {
                        setGroup(facility, newVehicles);
                    }
                }
            }
        }
    }

    private static void setGroup(Facility facility, Map<Long, Vehicle> newVehicles) {

        MoveAction moveAction2 = new MoveAction(3, 20000, 0);
        moveAction2.id = (int) facility.getId();
        moveAction2.action = ActionType.SETUP_VEHICLE_PRODUCTION;
        moveAction2.facilityId = facility.getId();
        moveAction2.vehicleType = null;
        MyStrategy.addMoveAction(moveAction2);

//        if (VehicleType.HELICOPTER.equals(facility.getVehicleType())) {
//            MoveAction moveAction = new MoveAction(1, MyStrategy.world.getTickIndex(), 2);
//            moveAction.action = ActionType.ASSIGN;
//            moveAction.left = facility.getLeft();
//            moveAction.right = facility.getLeft() + MyStrategy.game.getFacilityWidth();
//            moveAction.top = facility.getTop();
//            moveAction.bottom = facility.getTop() + MyStrategy.game.getFacilityHeight();
//            moveAction.notReplace = true;
//            MyStrategy.addMoveAction(moveAction);
//            facilityTypes.remove(facility.getId());
//        } else {
            Group group = new Group(idGroupCount++, newVehicles, facility.getVehicleType());
            GroupHelper.groups.add(group);
            MoveAction moveAction = new MoveAction(1, MyStrategy.world.getTickIndex(), group.groupId);
            moveAction.action = ActionType.ASSIGN;
            moveAction.left = facility.getLeft();
            moveAction.right = facility.getLeft() + MyStrategy.game.getFacilityWidth();
            moveAction.top = facility.getTop();
            moveAction.bottom = facility.getTop() + MyStrategy.game.getFacilityHeight();
            moveAction.notReplace = true;
            moveAction.facilityId = facility.getId();
            MyStrategy.addMoveAction(moveAction);
            facilityTypes.remove(facility.getId());

            facilitySetGroup.put(facility.getId(), true);
//        }
    }

    private static VehicleType getVehicleType(Facility facility) {
        if (facilityTypes.containsKey(facility.getId()))
            return facilityTypes.get(facility.getId());

        int countAir = WorldService.enemyFighters.size() + WorldService.enemyHelicopters.size();
        int countTanks = WorldService.enemyTank.size() + WorldService.enemyARRVs.size();
        int countIfv = WorldService.enemyIFVs.size();

        VehicleType type;
        if (countAir > countTanks && countAir > countTanks && countAir > countIfv)
            type = VehicleType.FIGHTER;
        else if (countTanks > countIfv)
            type = VehicleType.HELICOPTER;
        else
            type = VehicleType.TANK;

        facilityTypes.put(facility.getId(), type);
        return type;
    }

    private static void addProductionAction(Facility facility) {
        MoveAction moveAction = new MoveAction(3, 20000, 0);
        moveAction.id = (int) facility.getId();
        moveAction.action = ActionType.SETUP_VEHICLE_PRODUCTION;
        moveAction.facilityId = facility.getId();
        moveAction.vehicleType = getVehicleType(facility);
        MyStrategy.addMoveAction(moveAction);
    }

}
