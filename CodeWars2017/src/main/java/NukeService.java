import model.*;


/**
 * @author George Beliy on 20-11-17
 */
class NukeService {

    private static Double[][] nukePP = new Double[StrategyHelper.CELL_WIDTH][StrategyHelper.CELL_WIDTH];

    static boolean attackNuke;

    static void init() {
        if (!attackNuke && MyStrategy.me.getRemainingNuclearStrikeCooldownTicks() == 0 && GroupHelper.groupTanks) {
            updateNukePP(2);
            Vehicle vehicle = getVehicleUseNuke(0.1);
            if (vehicle == null && MyStrategy.world.getTickIndex() > 5000) {
                updateNukePP(0);
                vehicle = getVehicleUseNuke(0.9999);
            }
            if (vehicle != null) {
                Point nukePoint = StrategyHelper.getPriorityPoint(nukePP, new Point(vehicle), StrategyHelper.getVisionRange(vehicle), 0.3);
                if (nukePoint != null) {
                    attackNuke(vehicle, nukePoint);
                }
            }
        }
        if (MyStrategy.me.getRemainingNuclearStrikeCooldownTicks() != 0)
            attackNuke = false;
    }

    private static Vehicle getVehicleUseNuke(double nukeFactor) {
        Vehicle vehicleUseNuke = null;
        for (Vehicle vehicle : WorldService.allVehicle.values()) {
            if (vehicle.getPlayerId() == MyStrategy.me.getId()) {
                Point nukePoint = StrategyHelper.getPriorityPoint(nukePP, new Point(vehicle), StrategyHelper.getVisionRange(vehicle) * 1.0, nukeFactor);
                if (nukePoint != null) {
                    if (nukePoint.getPPoint(nukePP) < nukeFactor) {
                        vehicleUseNuke = vehicle;
                        nukeFactor = nukePoint.getPPoint(nukePP);
                    }
                }
            }
        }
        return vehicleUseNuke;
    }

    private static void attackNuke(Vehicle vehicle, Point nukePoint) {
        MoveAction moveAction = new MoveAction(1, MyStrategy.world.getTickIndex(), 0);
        moveAction.vehicleId = vehicle.getId();
        moveAction.action = ActionType.TACTICAL_NUCLEAR_STRIKE;
        moveAction.x = nukePoint.getX();
        moveAction.y = nukePoint.getY();
        MyStrategy.addMoveAction(moveAction);
    }

    private static void updateNukePP(int radius) {
        for (int i = 0; i < nukePP.length; i++) {
            for (int j = 0; j < nukePP.length; j++) {
                nukePP[i][j] = 1.0;
            }
        }
        for (Vehicle vehicle : WorldService.allVehicle.values()) {
            WorldService.updatePP(vehicle, null, nukePP,
                    (vehicle.getPlayerId() == MyStrategy.me.getId() ? 1.05
                            : VehicleType.ARRV.equals(vehicle.getType()) ? WorldService.allVehicle.size() < 250 ? 0.99 : 1.05 : 0.91), radius, false);
        }
    }

}
