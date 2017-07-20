import model.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Класс для работы с бонусами
 */
class BonusHelper {

    /** Позиция бонуса вверху */
    static final Point2D BONUS_TOP = new Point2D(1200, 1200);

    /** Позиция бонуса внизу */
    static final Point2D BONUS_BOTTOM = new Point2D(2800, 2800);

    /** Бонус появляется каждые 2500 тиков */
    private static final int CREATE_BONUS_TICK_INTERVAL = 2500;

    /** Радиус бонуса */
    private static final int BONUS_RADIUS = 30;

    /** Диапазон времени до появления бонуса и после. В течение этого диапазона (1200 тиков) волшебник может пойти за бонусом */
    private static final int GET_BONUS_RANGE = 600;


    /** Создаём singleton объект данного класса */
    private static BonusHelper helper;
    static synchronized BonusHelper getInstance() {
        if (helper == null) {
            helper = new BonusHelper();
            helper.attackHelper = AttackHelper.getInstance();
            helper.movesHelper = MovesHelper.getInstance();
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

    /** Хелпер для работы с движением */
    private MovesHelper movesHelper;


    /** Ближайший бонус */
    Point2D nearestBonus;

    /** {true}, если был взят бонус вверху */
    private boolean getTopBonus = false;

    /** {true}, если был взят бонус внизу */
    private boolean getBottomBonus = false;

    /** Тик на котором появится бонус */
    private int emergenceBonus = CREATE_BONUS_TICK_INTERVAL;

    /** Флаг, что мы начали бежать к бонусу через лес */
    boolean goToBonus = false;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     * Инициализируем данные.
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        updateBonusData(world);
        if (Math.abs(emergenceBonus - world.getTickIndex()) < GET_BONUS_RANGE
                || isHaveBonus(BONUS_BOTTOM) || isHaveBonus(BONUS_TOP)) {
            if (world.getTickIndex() > emergenceBonus) {
                updateViewBonus();
            }
            selectNearestBonus();
        }
    }

    /**
     * Обновляем данные о бонусах.
     */
    private void updateBonusData(World world) {
        if (!isHaveBonus(BONUS_TOP) && !isHaveBonus(BONUS_BOTTOM)
                && world.getTickIndex() < world.getTickCount() - (CREATE_BONUS_TICK_INTERVAL - GET_BONUS_RANGE)
                && Math.abs(emergenceBonus - world.getTickIndex()) > GET_BONUS_RANGE) {
            emergenceBonus = (world.getTickIndex() / CREATE_BONUS_TICK_INTERVAL + 1) * CREATE_BONUS_TICK_INTERVAL + 1;
            getBottomBonus = false;
            getTopBonus = false;
            getTopBonus = false;
            goToBonus = false;
        }
        nearestBonus = null;
    }

    /**
     * Если юонус видно фиксируем это
     */
    private void updateViewBonus() {
        updateBonus(BONUS_TOP);
        updateBonus(BONUS_BOTTOM);
    }

    /**
     * Обновляем данные по бонусу. Если его видит противники он ближе к нему чем мы,
     * то считаем что за бонусом нет смысла бежать
     */
    private void updateBonus(Unit bonus) {
        for (Wizard wizard : world.getWizards()) {
            if (!wizard.isMe() && bonus.getDistanceTo(wizard) < wizard.getVisionRange()
                    && self.getDistanceTo(bonus) > wizard.getDistanceTo(bonus)) {
                if (bonus.getX() == BONUS_BOTTOM.getX()) {
                    getBottomBonus = true;
                } else {
                    getTopBonus = true;
                }
            }
        }
        if (self.getDistanceTo(bonus) < self.getVisionRange()) {
            if (!isHaveBonus(bonus)) {
                if (bonus.getX() == BONUS_BOTTOM.getX()) {
                    getBottomBonus = true;
                } else {
                    getTopBonus = true;
                }
            } else {
                if (bonus.getX() == BONUS_BOTTOM.getX()) {
                    getBottomBonus = false;
                } else {
                    getTopBonus = false;
                }
            }
        }
    }

    /**
     * @return {true}, если бонус {bonusPoint} появился и его видно (союзником или нами)
     */
    boolean isHaveBonus(Unit bonusPoint) {
        for (Bonus bonus : world.getBonuses()) {
            if (bonus.getX() == bonusPoint.getX()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Выбираем бонус к которому побежим, или не побежим, если нет смысла
     */
    private void selectNearestBonus() {
        Double selfSpeed = 3.0;
        if (StrategyHelper.isHaveStatus(self, StatusType.HASTENED))
            selfSpeed += selfSpeed * game.getHastenedMovementBonusFactor();

        nearestBonus = null;
        if (!LaneType.BOTTOM.equals(movesHelper.laneType) && !getTopBonus
                && !(StrategyHelper.getLane(self) == null && self.getDistanceTo(BONUS_TOP) > self.getDistanceTo(BONUS_BOTTOM))) {
            if (emergenceBonus < world.getTickIndex() + (getDistanceToBonus(BONUS_TOP) / selfSpeed)) {
                nearestBonus = BONUS_TOP;
            }
        }
        if (!LaneType.TOP.equals(movesHelper.laneType) && !getBottomBonus
                && !(StrategyHelper.getLane(self) == null && self.getDistanceTo(BONUS_TOP) < self.getDistanceTo(BONUS_BOTTOM))) {
            if (emergenceBonus < world.getTickIndex() + (getDistanceToBonus(BONUS_BOTTOM) / selfSpeed)) {
                if (nearestBonus == null || BONUS_BOTTOM.getDistanceTo(self) < BONUS_TOP.getDistanceTo(self)) {
                    nearestBonus = BONUS_BOTTOM;
                }
            }
        }
    }

    /**
     * @return дистанцию до бонуса {bonus}. Если на прямик нельзя, то считаем через перекрёсток.
     */
    private Double getDistanceToBonus(Unit bonus) {
        Double distance = 0.0;
        if (StrategyHelper.getUnitByPath(DataHelper.getTrees(), self, new Point2D(bonus)).isEmpty()) {
            distance += self.getDistanceTo(bonus);
        } else {
            Point2D safePoint = DataHelper.minSaveIndexLaneWaypoint.get(movesHelper.laneType);
            distance += self.getDistanceTo(safePoint) / 2;
            distance += safePoint.getDistanceTo(bonus);
        }
        return distance;
    }

    /**
     * @return нужно ли бежать за бонусом
     */
    boolean isGoToBonus() {
        if (nearestBonus != null && (!game.isSkillsEnabled() || DataHelper.enemyBase.getDistanceTo(self) > 1100)) {
            Integer position = DataHelper.enemyMinionPositionIndex.get(LaneType.MIDDLE);
            Integer minSafe = StrategyHelper.getMinSafePosition(LaneType.MIDDLE);
            if (position != null && position <= minSafe && nearestBonus != null
                    && ((self.getDistanceTo(BONUS_TOP) < 600 && nearestBonus.equals(BONUS_BOTTOM))
                          || (nearestBonus.equals(BONUS_TOP) && self.getDistanceTo(BONUS_BOTTOM) < 600))) {
                return false;
            }

            if (isMayHackWay() || goToBonus) {
                goToBonus = true;
                return true;
            }

            Point2D safePoint = DataHelper.minSaveIndexLaneWaypoint.get(movesHelper.laneType);
            if ((!game.isSkillsEnabled() || StrategyHelper.isHaveStatus(self, StatusType.HASTENED))
                    && StrategyHelper.getNearPointIndex(safePoint, movesHelper.waypoints) <= StrategyHelper.getNearPointIndex(self, movesHelper.waypoints)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {true}, если можно прорубаться до бонуса
     */
    private boolean isMayHackWay() {
        List<LivingUnit> blocks = new ArrayList<>();
        blocks.addAll(DataHelper.buildings.values());
        blocks.addAll(DataHelper.neutralMinions.values());
        blocks.addAll(DataHelper.trees.values());

        int countHealth = 0;
        List<LivingUnit> units = StrategyHelper.getUnitByPath(blocks, self, nearestBonus);
        for (LivingUnit unit : units) {
            if (!(unit instanceof Tree)) {
                countHealth += 100;
            } else {
                countHealth += unit.getLife();
            }
        }

        return countHealth / attackHelper.magicMissileDamage <= 3 && units.size() <= 3;
    }

    /**
     * Бежим за бонусом
     */
    void runToBonus() {
        if (attackHelper.nearestTarget != null) {
            attackHelper.attack();
        }

        if (StrategyHelper.countUnitByPath(DataHelper.getTrees(), self, nearestBonus) == 0) {
            if (emergenceBonus - world.getTickIndex() > 0) {
                if (nearestBonus.getDistanceTo(self) > BONUS_RADIUS + self.getRadius()
                        || isHaveBonus(nearestBonus)) {
                    movesHelper.nextWaypoint = nearestBonus;
                    movesHelper.goTo(movesHelper.getNextWaypoint(), attackHelper.isAttackNearestTarget());
                } else if (nearestBonus.getDistanceTo(self) < BONUS_RADIUS + self.getRadius()) {
                    movesHelper.goTo(movesHelper.getPreviousWaypoint(), true);
                }
            } else {
                movesHelper.nextWaypoint = nearestBonus;
                movesHelper.goTo(movesHelper.getNextWaypoint(), attackHelper.isAttackNearestTarget());
            }
        } else {
            if (isMayHackWay()) {
                Point2D priorityPoint = StrategyHelper.getPriorityPoint(DataHelper.getBlockUnits(world), nearestBonus, self);
                if (priorityPoint != null && nearestBonus.getDistanceTo(self) - self.getRadius() > nearestBonus.getDistanceTo(priorityPoint)) {
                    movesHelper.goTo(priorityPoint, false);
                } else {
                    Point2D priorityPoint2 = StrategyHelper.getPriorityPoint(DataHelper.getNeutralMinions(), nearestBonus, self);
                    if (priorityPoint2 != null) {
                        movesHelper.goTo(priorityPoint2, false);
                    } else {
                        movesHelper.goTo(nearestBonus, false);
                    }
                }
            } else {
                Point2D safePoint = DataHelper.minSaveIndexLaneWaypoint.get(movesHelper.laneType);
                if (StrategyHelper.getNearPointIndex(safePoint, movesHelper.waypoints) <= StrategyHelper.getNearPointIndex(self, movesHelper.waypoints)) {
                    movesHelper.nextWaypoint = safePoint;
                    movesHelper.goTo(movesHelper.getNextWaypoint(), false);
                } else {
                    movesHelper.goTo(nearestBonus, false);
                }
            }
        }
    }
}
