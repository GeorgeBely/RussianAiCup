import model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author George Beliy on 08-11-17
 */
class GroupHelper {

    static boolean groupTanks = false;
    private static boolean select = false;
    private static boolean byX = false;
    private static boolean byY = false;
    private static boolean moveToGroup = false;
    private static boolean unitsDecouples = false;
    private static boolean reverse = false;
    private static boolean clearCell = false;
    private static boolean move = false;

    static List<Group> groups = new ArrayList<>();

    private static boolean byTank = true;


    static void init() {
        if (!select) {
            initGroup(new Group(1, WorldService.myFighters, VehicleType.FIGHTER));
            initGroup(new Group(2, WorldService.myHelicopters, VehicleType.HELICOPTER));
            select = true;
            return;
        }

        if (!groupTanks) {
            int minTankX = StrategyHelper.minX(WorldService.myTank.values());
            int minTankY = StrategyHelper.minY(WorldService.myTank.values());
            int minArrvX = StrategyHelper.minX(WorldService.myARRVs.values());
            int minArrvY = StrategyHelper.minY(WorldService.myARRVs.values());
            int maxTankX = StrategyHelper.maxX(WorldService.myTank.values());
            int maxTankY = StrategyHelper.maxY(WorldService.myTank.values());
            int maxArrvX = StrategyHelper.maxX(WorldService.myARRVs.values());
            int maxArrvY = StrategyHelper.maxY(WorldService.myARRVs.values());
            int minIfvX = StrategyHelper.minX(WorldService.myIFVs.values());
            int maxIfvX = StrategyHelper.maxX(WorldService.myIFVs.values());
            int minIfvY = StrategyHelper.minY(WorldService.myIFVs.values());
            int maxIfvY = StrategyHelper.maxY(WorldService.myIFVs.values());

            int currMinX;
            int currMaxX;
            int currMaxY;
            int currMinY;
            if (byTank) {
                currMinX = minTankX;
                currMaxX = maxTankX;
                currMinY = minTankY;
                currMaxY = maxTankY;
            } else {
                currMinX = minIfvX;
                currMaxX = maxIfvX;
                currMinY = minIfvY;
                currMaxY = maxIfvY;
            }


            int minX = Math.min(minArrvX, currMinX);
            int minY = Math.min(minArrvY, currMinY);
            int maxX = Math.max(currMaxX, maxArrvX);
            int maxY = Math.max(maxArrvY, currMaxY);

            VehicleType type = byTank ? VehicleType.TANK : VehicleType.IFV;

            if (!byY && !byX) {
                if (Math.abs(minArrvX - currMinX) < 100 && Math.abs(minArrvY - currMinY) < 100) {
                    if (minArrvX == currMinX) {
                        byX = true;
                    } else if (minArrvY == currMinY) {
                        byY = true;
                    } else if (minArrvX - currMinX == minArrvY - currMinY) {
                        reverse = true;
                        boolean notHaveRight = WorldService.getVehiclesByRectangle(minX + 70, maxX, minY, minY + 50).isEmpty();
                        boolean notHaveLeft = WorldService.getVehiclesByRectangle(minX, minX + 50, minY + 70, maxY).isEmpty();
                        if (notHaveLeft && notHaveRight) {
                            byX = true;
                        } else if (notHaveRight) {
                            byY = true;
                            moveTo(minX - 5, minX + 60, minY + 70, minY + 220, 0, 75, null);
                        } else if (notHaveLeft) {
                            byX = true;
                            moveTo(minX + 60, minX + 220, minY, minY + 70, 75, 0, null);
                        } else {
                            if (!clearCell) {
                                moveTo(minX + 60, minX + 220, minY, minY + 70, 75, 0, null);
                                moveTo(minX - 5, minX + 60, minY + 70, minY + 220, 0, 75, null);
                                clearCell = true;
                            }
                        }
                    } else if (minArrvX - currMinX == -(minArrvY - currMinY) && !move) {
                        boolean notHaveRight = WorldService.getVehiclesByRectangle(minX + 70, maxX, minY+70, maxY).isEmpty();
                        boolean notHaveLeft = WorldService.getVehiclesByRectangle(minX, minX + 70, minY, minY + 70).isEmpty();

                        if (notHaveLeft && notHaveRight) {
                            reverse = true;
                            byX = true;
                        } else if (notHaveLeft) {
                            reverse = true;
                            byX = true;
                            moveTo(minX + 60, minX + 220, minY + 60, maxY, 75, 0, null);
                        } else if (notHaveRight) {
                            move = true;
                            moveTo(minX - 5, minX + 70, minY + 70, maxY, Math.abs(minArrvX - currMinX), 0, type);
                        } else {
                            move = true;
                            moveTo(minX - 5, minX + 220, minY + 70, maxY, Math.abs(minArrvX - currMinX), 0, type);
                        }
                    }
                } else if (currMinX == minTankX && currMinY == minTankY) {
                    byTank = false;
                    return;
                } else {
                    initGroup(new Group(3, WorldService.myTank, VehicleType.TANK));
                    initGroup(new Group(5, WorldService.myARRVs, VehicleType.ARRV));
                    initGroup(new Group(4, WorldService.myIFVs, VehicleType.IFV));
                    scaleGroups();
                    groupTanks = true;
                    return;
                }
            }

            if (byX ? (Math.abs(currMaxY - maxArrvY) <= 1 && Math.abs(currMaxX - maxArrvX) <= 15)
                    : (Math.abs(minArrvX - currMinX) <= 1 && Math.abs(currMaxY - maxArrvY) <= 15)) {
                if (MyStrategy.world.getFacilities().length != 0) {
                    initGroup(new Group(3,
                            WorldService.getVehiclesByRectangle(minX-5, byX ? (maxX + minX)/2 : maxX + 5,
                                    minY-5, byX ? maxY + 5 : (maxY + minY)/2), type));
                    initGroup(new Group(5,
                            WorldService.getVehiclesByRectangle(byX ? (maxX + minX)/2 : minX - 5, maxX + 5,
                                    byX ? minY - 5 : (maxY + minY)/2, maxY + 5), type));
                } else {
                    initGroup(new Group(3,
                            WorldService.getVehiclesByRectangle(minX - 5, maxX + 5, minY - 5, maxY + 5), type, true));
                }
                initGroup(new Group(4, byTank ? WorldService.myIFVs : WorldService.myTank, byTank ? VehicleType.IFV : VehicleType.TANK));
                scaleGroups();
                groupTanks = true;
                return;
            }

            if (!moveToGroup && (byX ? maxArrvX - minArrvX >= 101 : maxArrvY - minArrvY >= 101) &&
                    (byX ? currMaxX - currMinX >= 101 : currMaxY - currMinY >= 101)) {
                moveTo(minArrvX - 5, maxArrvX + 5, minArrvY - 5, maxArrvY + 5,
                        byX ? 0 : -(minArrvX - currMinX), byX ? -(minArrvY - currMinY) : 0, VehicleType.ARRV);
                moveToGroup = true;
                return;
            }

            if (!unitsDecouples && (byX || byY)) {
                int factorMin = !reverse ? 1 : minArrvX - currMinX == minArrvY - currMinY ? 1 : byX ? -1 : 1;
                int factorMax = !reverse ? 1 : minArrvX - currMinX == minArrvY - currMinY ? -1 : byX ? 1 : -1;
                int range = maxArrvX - minArrvX + 10;
                if (!reverse) {
                    if (byX) {
                        if (WorldService.getVehiclesByRectangle(minX - range, minX - 5, minY, maxY + 5).isEmpty()
                                && minX - 70 > 0) {
                            factorMin = -1;
                        } else {
                            if (!WorldService.getVehiclesByRectangle(maxX + 5, maxX + range, minY, maxY + 5).isEmpty()) {
                                moveTo(minX + 60, minX + 220, minY-5,maxY + 5, 75, 0, null);
                            }
                            factorMin = 1;
                        }
                    } else {
                        if (WorldService.getVehiclesByRectangle(minX, maxX + 5, minY - range, minY - 5).isEmpty()
                                && minY - 70 > 0) {
                            factorMin = -1;
                        } else {
                            if (!WorldService.getVehiclesByRectangle(minX, maxX + 5, maxY + 5, maxY + range).isEmpty()) {
                                moveTo(minX-5, maxX + 5, minY + 70, minY + 220, 0, 75, null);
                            }
                            factorMin = 1;
                        }
                    }
                    factorMax = factorMin;
                }

                int factorTank = reverse ? currMinY > minArrvY ? factorMax : factorMin : factorMin;
                int factorArrv = reverse ? currMinY > minArrvY ? factorMin : factorMax : factorMin;
                for (int i = 5; i > 0; i--) {
                    int jTank = factorTank == -1 ? 6 - i : i;
                    int jArrv = factorArrv == -1 ? 6 - i : i;
                    if (byY) {
                        moveTo(currMinX-1, currMinX + 55, currMinY + ((jTank - 1) * 11), currMinY + (jTank * 11),
                                0, factorTank * ((i) * 12), type);
                        moveTo(minArrvX-1, minArrvX + 55, minArrvY + ((jArrv - 1) * 11), minArrvY + (jArrv * 11),
                                0, factorArrv * (((i) - 1) * 12), VehicleType.ARRV);
                    } else {
                        moveTo(currMinX + ((jTank - 1) * 11), currMinX + (jTank * 11), currMinY-1, currMinY + 55,
                                factorTank * (i) * 12, 0, type);
                        moveTo(minArrvX + ((jArrv - 1) * 11), minArrvX + (jArrv * 11), minArrvY-1, minArrvY + 55,
                                factorArrv * ((i) - 1) * 12, 0, VehicleType.ARRV);
                    }
                }
                unitsDecouples = true;
            }

            return;
        }
        List<Group> emptyGroup = new ArrayList<>();
        for (Group group : groups) {
            if (group.isEmpty()) {
                emptyGroup.add(group);
                MyStrategy.removeActionByGroup(group.groupId, MyStrategy.secondDelayedMoves);
            }
        }
        groups.removeAll(emptyGroup);
        groups.stream().filter(group -> !group.vehicles.isEmpty()).forEach(group -> group.update(MyStrategy.world.getVehicleUpdates()));
    }


    private static void moveTo(double l, double r, double t, double b, double x, double y, VehicleType type) {
        MoveAction moveAction = new MoveAction(1, 5000, 0);
        moveAction.action = ActionType.MOVE;
        moveAction.left = l;
        moveAction.right = r;
        moveAction.top = t;
        moveAction.bottom = b;
        moveAction.y = y;
        moveAction.x = x;
        if (type != null)
            moveAction.maxSpeed = StrategyHelper.MAX_SPEED_BY_TYPE.get(type) * MyStrategy.game.getSwampTerrainSpeedFactor();
        MyStrategy.addMoveAction(moveAction);
    }

    private static void initGroup(Group group) {
        groups.add(group);
        Point pointMin = StrategyHelper.min(group.vehicles.values());
        Point pointMax = StrategyHelper.max(group.vehicles.values());
        MoveAction moveAction = new MoveAction(1, MyStrategy.world.getTickIndex(), group.groupId);
        moveAction.action = ActionType.ASSIGN;
        moveAction.left = pointMin.getX();
        moveAction.right = pointMax.getX() + 5;
        moveAction.top = pointMin.getY();
        moveAction.bottom = pointMax.getY() + 5;
        moveAction.notReplace = true;
        MyStrategy.addMoveAction(moveAction);
    }

    private static void scaleGroups() {
        for (Group group : groups) {
            MyStrategy.addMoveAction(MoveHelper.scaleGroupCenter(group));
        }
    }


}
