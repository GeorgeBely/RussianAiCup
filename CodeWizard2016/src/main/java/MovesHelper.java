import model.*;

import java.util.*;
import java.util.List;


/**
 * Класс для работы с движением
 */
class MovesHelper {

    /** Если столько тиков мы не двигались, то считаем что мы застряли */
    private static final int MAX_NOT_MOVE_TICK = 100;

    /** Сколько тиков мы будем двигаться назад, если застряли */
    private static final int BACK_MOVE_TICK = 50;


    /** Создаём singleton объект данного класса */
    private static MovesHelper helper;
    static synchronized MovesHelper getInstance() {
        if (helper == null) {
            helper = new MovesHelper();
            helper.saveHelper = SaveHelper.getInstance();
            helper.attackHelper = AttackHelper.getInstance();
        }
        return helper;
    }


    /** Данные которые пришли нам в данный тик */
    private Wizard self;
    private World world;
    private Game game;
    private Move move;

    /** Хелпер для работы с атакой */
    private AttackHelper attackHelper;

    /** Хелпер для работы с атакой */
    private SaveHelper saveHelper;


    /**
     * Колличество тиков в застрявшем положении. Если ето число больше {MAX_NOT_MOVE_TICK},
     * то волшебник  будет двигаться назад {BACK_MOVE_TICK} тиков
     */
    private int notMoveCount = 0;

    /** Колличество тиков, которые мы двигаемся назад (если застряли до этого) */
    private int moveBackCount = 0;

    /** Признак в какую сторону двигаться, когда мы застряли и отходим назад */
    private boolean lastStrafe = false;

    /** Список контрольных точек */
    Point2D[] waypoints;

    /** Точка к которой бежим (Для отладки) */
    Point2D currentMovePoint;

    /** Слкдующая точка */
    Point2D nextWaypoint;

    /** Предыдущая точка */
    Point2D prevWaypoint;


    /** текущая линия, по которой движется волшебник */
    LaneType laneType;

    private Point2D backPoint;

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним, и инициализируем необходимые поля
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        this.currentMovePoint = null;

        initWaypoints();
        initNearWaypoints();
    }

    /**
     * Выбираем путь, по которому мы пойдём и инициализируем контрольные точки
     */
    private void initWaypoints() {
        if (StrategyHelper.getLane(self) != null && laneType != null && !laneType.equals(StrategyHelper.getLane(self))) {
            laneType = StrategyHelper.getLane(self);
            waypoints = DataHelper.getWaypoints(laneType);
        }
        if (waypoints == null
                || DataHelper.selfBase.getDistanceTo(self) < SaveHelper.SAFE_BASE_DISTANCE
                || BonusHelper.BONUS_BOTTOM.getDistanceTo(self) < 400
                || BonusHelper.BONUS_TOP.getDistanceTo(self) < 400) {
            laneType = StrategyHelper.selectMoveLine(world, self);
            waypoints = DataHelper.getWaypoints(laneType);
        }
    }

    /**
     * Инициализируем следующую и предыдущаую контрольную точку
     */
    private void initNearWaypoints() {
        initNearLaneWaypoints();
        backPoint = StrategyHelper.getBackPoint(self, 500.0);
        if (StrategyHelper.getLane(self) == null && DataHelper.selfBase.getDistanceTo(self) > SaveHelper.SAFE_BASE_DISTANCE) {
            if (StrategyHelper.countUnitByPath(DataHelper.getTrees(), self, prevWaypoint) == 0) {
                prevWaypoint = backPoint;
            }

            Integer enemyPositionIndex = DataHelper.enemyMinionPositionIndex.get(laneType);
            Integer nextWaypointIndex = StrategyHelper.getWaypointIndex(nextWaypoint, waypoints);
            Integer minSafeWaypointIndex = StrategyHelper.getWaypointIndex(DataHelper.minSaveIndexLaneWaypoint.get(laneType), waypoints);
            if (enemyPositionIndex != null) {
                if (enemyPositionIndex < nextWaypointIndex){
                    nextWaypoint = new Point2D(DataHelper.selfBase);
                } else if (enemyPositionIndex >= minSafeWaypointIndex && nextWaypointIndex < minSafeWaypointIndex) {
                    nextWaypoint = DataHelper.minSaveIndexLaneWaypoint.get(laneType);
                }
            }
        }
    }

    private void initNearLaneWaypoints() {
        Point2D nearPoint1 = null;
        Point2D nearPoint2 = null;
        for (Point2D waypoint : waypoints) {
            if (waypoint.getDistanceTo(self) > DataHelper.WAYPOINT_RADIUS || waypoint.equals(waypoints[0])) {
                if (nearPoint1 == null) {
                    nearPoint1 = waypoint;
                    continue;
                }
                if (waypoint.getDistanceTo(self) < nearPoint1.getDistanceTo(self)) {
                    nearPoint2 = nearPoint1;
                    nearPoint1 = waypoint;
                } else if (nearPoint2 == null || waypoint.getDistanceTo(self) < nearPoint2.getDistanceTo(self)) {
                    nearPoint2 = waypoint;
                }
            }
        }

        if (waypoints[0].getDistanceTo(nearPoint1) < waypoints[0].getDistanceTo(nearPoint2)) {
            prevWaypoint = nearPoint1;
            nextWaypoint = nearPoint2;
        } else {
            prevWaypoint = nearPoint2;
            nextWaypoint = nearPoint1;
        }

        if (StrategyHelper.getLane(self) == null) {
            int indexPrev = StrategyHelper.getWaypointIndex(prevWaypoint, waypoints);
            for (int i = indexPrev; i > 0; i--) {
                if (StrategyHelper.countUnitByPath(DataHelper.getTrees(), self, waypoints[i]) == 0) {
                    prevWaypoint = waypoints[i];
                }
            }
        }

        Integer enemyPosition = DataHelper.enemyMinionPositionIndex.get(laneType);
        int indexNext = StrategyHelper.getWaypointIndex(nextWaypoint, waypoints);
        if (StrategyHelper.getLane(self) == null && (enemyPosition == null || enemyPosition > indexNext)) {
            for (int i = indexNext; i < (enemyPosition != null ? enemyPosition - 1 : waypoints.length); i++) {
                if (StrategyHelper.countUnitByPath(DataHelper.getBlockUnits(world), self, waypoints[i]) == 0) {
                    nextWaypoint = waypoints[i];
                }
            }
        }
    }

    /**
     * Если нет других действий, просто продвигаемся вперёд.
     */
    void moving() {
        if (StrategyHelper.getLane(self) == null && DataHelper.selfBase.getDistanceTo(self) > SaveHelper.SAFE_BASE_DISTANCE) {
            goTo(getBackToLaneWaypoint(), false);
        } else {
            Point2D next = getNextWaypoint();
            goTo(next, false);
        }
    }

    /**
     * Задаём параметры перемещения волшебника.
     *
     * @param point   точка, ккоторой движемся
     * @param notTurn нужно ли поворачиваться, если {true}, то волшебник будет двигаться боком
     */
    void goTo(Point2D point, boolean notTurn) {
        if (isMoveBack())
            return;

        double angle = self.getAngleTo(point.getX(), point.getY());
        if (!notTurn) {
            move.setTurn(angle);
        }
        move.setSpeed(StrategyHelper.getSpeedWithMovementBonus(self, game, Math.abs(angle) > Math.PI/2 ? game.getWizardBackwardSpeed() : game.getWizardForwardSpeed()) * Math.cos(angle));
        move.setStrafeSpeed(StrategyHelper.getSpeedWithMovementBonus(self, game, game.getWizardStrafeSpeed()) * Math.sin(angle));

        List<LivingUnit> treesByPath = StrategyHelper.getUnitByPath(Arrays.asList(world.getTrees()), self, point);
        if (!treesByPath.isEmpty()) {
            LivingUnit nearestTarget = attackHelper.getNearestTarget(treesByPath.toArray(new LivingUnit[treesByPath.size()]), false);
            if (nearestTarget != null) {
                attackHelper.nearestTarget = nearestTarget;
                attackHelper.attack();
                notMoveCount = 0;
            }
        }
        currentMovePoint = point;
    }

    /**
     * @return точка, к которой будем двигаться, чтобы выйти на линию. Если движению к следующеей точки нам мешает какое-либо препядствие,
     *         то находим точку вокруг следующей, которая в прямой видимости. Не учитываются деревья (их ломаем).
     */
    Point2D getBackToLaneWaypoint() {
        if (!StrategyHelper.onRiver(self)) {
            List<LivingUnit> blockTreeUnits = new ArrayList<>();
            blockTreeUnits.addAll(DataHelper.neutralMinions.values());
            blockTreeUnits.addAll(DataHelper.buildings.values());
            blockTreeUnits.addAll(DataHelper.trees.values());

            Point2D priorityPoint = StrategyHelper.getPriorityPoint(blockTreeUnits, nextWaypoint, self);
            if (priorityPoint != null && nextWaypoint.getDistanceTo(self) - self.getRadius() * 2 > nextWaypoint.getDistanceTo(priorityPoint)) {
                return priorityPoint;
            }
        }

        List<LivingUnit> blockUnits = new ArrayList<>();
        blockUnits.addAll(DataHelper.neutralMinions.values());
        blockUnits.addAll(DataHelper.buildings.values());

        return StrategyHelper.getPriorityPoint(blockUnits, nextWaypoint, self);
    }

    /**
     * @return точка, к которой будем двигаться вперёд. Если движению к следующеей точки нам мешает какое-либо препядствие,
     *         то находим точку вокруг следующей, которая в прямой видимости.
     */
    Point2D getNextWaypoint() {
        return StrategyHelper.getPriorityPoint(DataHelper.getBlockUnits(world), nextWaypoint, self);
    }

    /**
     * @return точка, к которой будем двигаться назад. Если движению к предыдущей точки нам мешает какое-либо препядствие,
     *         то находим точку вокруг предыдущей, которая в прямой видимости.
     */
    Point2D getPreviousWaypoint() {
        if (saveHelper.getPointWarningLevel(prevWaypoint) > saveHelper.getPointWarningLevel(backPoint) && prevWaypoint.getDistanceTo(DataHelper.selfBase) > SaveHelper.SAFE_BASE_DISTANCE) {
            return StrategyHelper.getPriorityPoint(DataHelper.getBlockUnits(world), backPoint, self, true);
        } else {
            return StrategyHelper.getPriorityPoint(DataHelper.getBlockUnits(world), prevWaypoint, self, true);
        }
    }

    /**
     * Движемся назад, если застряли
     */
    private boolean isMoveBack() {
        if (notMoveCount >= MAX_NOT_MOVE_TICK) {
            if (moveBackCount < BACK_MOVE_TICK) {
                move.setSpeed(-game.getWizardBackwardSpeed() * 2);
                if (lastStrafe) {
                    move.setStrafeSpeed(game.getWizardStrafeSpeed());
                } else {
                    move.setStrafeSpeed(-game.getWizardStrafeSpeed());
                }
                moveBackCount++;
                return true;
            }
            notMoveCount = 0;
            moveBackCount = 0;
            lastStrafe = !lastStrafe;
        } else  {
            if (Math.abs(self.getSpeedX()) < 0.01 && Math.abs(self.getSpeedY()) < 0.01) {
                notMoveCount++;
            } else {
                notMoveCount = 0;
            }
        }
        return false;
    }
}
