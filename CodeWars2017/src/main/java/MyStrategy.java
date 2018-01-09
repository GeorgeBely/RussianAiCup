import model.*;

import java.util.*;


public final class MyStrategy implements Strategy {

    private static final int MAX_PRIORITY_MOVES = 2;

//    private static final int ACTION_TIME_LIFE = 50;

    private static final List<MoveAction> firstDelayedMoves = new ArrayList<>();
    static final List<MoveAction> secondDelayedMoves = new ArrayList<>();
    private static final List<MoveAction> thirdDelayedMoves = new ArrayList<>();

    static MoveAction currentMoveAction;

    static World world;
    static Player me;
    static Game game;

    private Move move;

    private int lastPriority;
    private int lastPriorityCount;


    /**
     * Основной метод стратегии, осуществляющий управление армией. Вызывается каждый тик.
     *
     * @param me    Информация о вашем игроке.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Player me, World world, Game game, Move move) {
        MyStrategy.world = world;
        MyStrategy.game = game;
        MyStrategy.me = me;
        this.move = move;

        WorldService.init();
        GroupHelper.init();

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }
        setActualMoveAction(false);
        if (currentMoveAction == null || !currentMoveAction.action.equals(ActionType.ASSIGN)) {
            FacilityService.init();
            if (currentMoveAction == null || !currentMoveAction.action.equals(ActionType.SETUP_VEHICLE_PRODUCTION)) {
                MoveHelper.init();
                NukeService.init();
            }
        }
        setActualMoveAction(true);
        executeDelayedMove();
    }

    private void setActualMoveAction(boolean remove) {
        MoveAction delayedMove = getActualAction(firstDelayedMoves, remove);
        if (delayedMove == null) {
            delayedMove = getSecondMA(remove);
            if (delayedMove == null) {
                if (remove) {
                    lastPriority = 3;
                    lastPriorityCount = 0;
                }
                delayedMove = getActualAction(thirdDelayedMoves, remove);
            }
        } else {
            if (remove) {
                lastPriority = 1;
                lastPriorityCount = 0;
            }
        }
        if (delayedMove != null) {
            currentMoveAction = delayedMove;
        }
    }

    private MoveAction getSecondMA(boolean remove) {
        if (lastPriority != 2 || lastPriorityCount < MAX_PRIORITY_MOVES || (thirdDelayedMoves.isEmpty())) {
            if (remove) {
                lastPriorityCount++;
                lastPriority = 2;
            }
            return getActualAction(secondDelayedMoves, remove);
        }
        return null;
    }

    private void executeDelayedMove() {
        if (currentMoveAction != null) {
            if (currentMoveAction.action.equals(ActionType.MOVE)) {
                currentMoveAction.moved = !(currentMoveAction.x + currentMoveAction.y < 32);
            }
            if (currentMoveAction.selected && currentMoveAction.group != 0 && currentMoveAction.priority == 2
                    && ActionType.MOVE.equals(currentMoveAction.action)) {
                MoveAction ma = new MoveAction();
                ma.update(currentMoveAction);
                ma.selected = false;
                addMoveAction(ma);
            }
            currentMoveAction.accept(move);
            if (move.getAction().equals(ActionType.TACTICAL_NUCLEAR_STRIKE)) {
                NukeService.attackNuke = true;
            }
            if (move.getAction().equals(ActionType.ASSIGN) && currentMoveAction.facilityId != -1) {
                FacilityService.facilitySetGroup.remove(currentMoveAction.facilityId);
            }
            currentMoveAction = null;
        }
    }

    private int lastGroup = 0;

    private MoveAction getActualAction(List<MoveAction> moveActions, boolean remove) {
        if (!moveActions.isEmpty()) {
            MoveAction delayedMove = moveActions.get(0);
            int noneGroup = delayedMove.group;
            while (delayedMove.action.equals(ActionType.NONE) && lastGroup != noneGroup
//                    && world.getTickIndex() - delayedMove.createTick > ACTION_TIME_LIFE
                    ) {
                moveActions.remove(0);
                MoveAction ma = new MoveAction();
                ma.update(delayedMove);
                ma.selected = false;
                ma.action = ActionType.MOVE;
                addMoveAction(ma);

                lastGroup = delayedMove.group;

                delayedMove = moveActions.get(0);
            }
            lastGroup = 0;

            if (delayedMove.selected && remove) {
                moveActions.remove(0);
            }
            return delayedMove;
        }
        return null;
    }

    static void addMoveAction(MoveAction moveAction) {
        if (moveAction.priority == 1) {
            if (!updateMoveAction(moveAction, firstDelayedMoves)) {
                firstDelayedMoves.add(moveAction);
            }
        } else if (moveAction.priority == 2) {
            if (!updateMoveAction(moveAction, secondDelayedMoves)) {
                secondDelayedMoves.add(moveAction);
            }
        } else if (moveAction.priority == 3) {
            if (!updateMoveAction(moveAction, thirdDelayedMoves)) {
                thirdDelayedMoves.add(moveAction);
            }
        }
    }


    static void removeActionByGroup(long groupId, List<MoveAction> list) {
        MoveAction moveAction = null;
        for (MoveAction ma : list) {
            if (ma.group == groupId) {
                moveAction = ma;
                break;
            }
        }
        if (moveAction != null) {
            list.remove(moveAction);
        }
    }

    static int getMovesSize() {
        return !firstDelayedMoves.isEmpty() ? firstDelayedMoves.size() :
                !secondDelayedMoves.isEmpty() ? secondDelayedMoves.size() :
                !thirdDelayedMoves.isEmpty() ? thirdDelayedMoves.size() : 0;
    }

    private static boolean updateMoveAction(MoveAction newMoveAction, List<MoveAction> queue) {
        for (MoveAction moveAction : queue) {
            if (!moveAction.notReplace) {
                if ((moveAction.group == newMoveAction.group && moveAction.group != 0)
                        || (moveAction.id == newMoveAction.id && moveAction.id != 0)) {
                    moveAction.update(newMoveAction);
                    return true;
                }
            }
        }
        return false;
    }
}