import model.*;



/**
 * @author George Beliy on 08-11-17
 */
class MoveHelper {

    private static boolean startMove;

    static void init() {
        if (!GroupHelper.groupTanks) {
            return;
        }

        if (MyStrategy.getMovesSize() == 0) {
            startMove = true;
        }
        if (!startMove)
            return;

        for (Group group : GroupHelper.groups) {
            if (MyStrategy.getMovesSize() < GroupHelper.groups.size() || MyStrategy.currentMoveAction == null
                    || MyStrategy.currentMoveAction.group == group.groupId) {
                if (!group.isEmpty()) {
                    if (MyStrategy.currentMoveAction == null || MyStrategy.getMovesSize() < GroupHelper.groups.size()
                            || MyStrategy.currentMoveAction.selected) {
                        MoveAction moveAction;
                        if (!isNeedScale(group)) {
                            moveAction = createMoveAction(group);
                        } else {
                            moveAction = scaleGroup(group);
                        }
                        if (moveAction != null) {
                            MyStrategy.addMoveAction(moveAction);
                        }
                    }
                }
            }
        }
    }

    private static boolean isNeedScale(Group group) {
        return StrategyHelper.maxX(group.vehicles.values()) - StrategyHelper.minX(group.vehicles.values())
                + StrategyHelper.maxY(group.vehicles.values()) - StrategyHelper.minY(group.vehicles.values()) > group.vehicles.size() + 40;
    }
    private static MoveAction rotateGroup(Group group) {
        Point center = group.getCenterGroup();
        MoveAction moveAction = new MoveAction(2, MyStrategy.world.getTickIndex(), group.groupId);


        moveAction.action = ActionType.ROTATE;
        moveAction.x = center.getX();
        moveAction.y = center.getY();
        moveAction.angle = Math.PI;
        moveAction.createTick = MyStrategy.world.getTickIndex();
        return moveAction;
    }

    private static MoveAction scaleGroup(Group group) {
        return scaleGroup(group, group.getScalePoint());
    }
    private static MoveAction scaleGroup(Group group, Point point) {
        MoveAction moveAction = new MoveAction(2, MyStrategy.world.getTickIndex(), group.groupId);
        moveAction.action = ActionType.SCALE;
        moveAction.x = point.getX();
        moveAction.y = point.getY();
        moveAction.factor = 0.1;
        moveAction.createTick = MyStrategy.world.getTickIndex() + 1000;

        return moveAction;
    }

    static MoveAction scaleGroupCenter(Group group) {
        return scaleGroup(group, group.getCenterGroup());
    }

    private static MoveAction createMoveAction(Group group) {
        MoveAction moveAction = new MoveAction();
        moveAction.priority = 2;
        moveAction.createTick = MyStrategy.world.getTickIndex();
        moveAction.group = group.groupId;
        moveAction.action = ActionType.MOVE;
        moveAction.maxSpeed = StrategyHelper.MAX_SPEED_BY_TYPE.get(group.type) * (group.isAir() ? 1.0 : MyStrategy.game.getSwampTerrainSpeedFactor());

        if (group.isAir() && !WorldService.haveEnemyInRange(group.getCenterGroup(), group.getVisionRange().intValue() * 2)) {
            moveAction.maxSpeed = StrategyHelper.MAX_SPEED_BY_TYPE.get(group.type) * MyStrategy.game.getRainWeatherSpeedFactor();
//            moveAction.priority = 3;
//            if (group.isAir())
//                moveAction.maxSpeed = MyStrategy.game.getTankSpeed();
        }

        Point point = group.getPriorityPoint();
        if (point == null) {
//            moveAction.priority = 3;
//            moveAction.maxSpeed = MyStrategy.game.getTankSpeed();
            point = group.getPriorityPoint(1024.0);
        }
        if (point != null) {
            Point centerGroup = group.getCenterGroup();
            moveAction.point = point;
            moveAction.x = point.getX() - centerGroup.getX();
            moveAction.y = point.getY() - centerGroup.getY();

            moveAction.action = ActionType.MOVE;
            if (centerGroup.distanceTo(point) < 10 ||
                    (MyStrategy.currentMoveAction != null && MyStrategy.currentMoveAction.point != null && MyStrategy.currentMoveAction.x + MyStrategy.currentMoveAction.y > 32
                            && MyStrategy.currentMoveAction.moved && point.distanceTo(MyStrategy.currentMoveAction.point) < 10)) {
//                moveAction.action = ActionType.NONE;
            }

            return moveAction;
        }
        return null;
    }

}
