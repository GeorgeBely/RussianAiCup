import model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Инициализируем и сохраняем данные, для дальнейшей работы.
 */
class DataHelper {

    /** Если расстояние до контрольной точки меньше, то эта контрольная точка при расчётах не учавствует */
    static final double WAYPOINT_RADIUS = 150.0D;

    private static final int UPDATE_TICK_RANGE = 100;

    /** Точки для оббегания базы в начале */
    private static final Point2D MIDDLE_TWO_TOP_POINT = new Point2D(200.0D, 3200.0D);
    private static final Point2D MIDDLE_TWO_BOTTOM_POINT = new Point2D(800.0D, 3800.0D);


    /** Деревья на карте */
    static Map<Long, Tree> trees = new HashMap<>();

    /** Строения на карте */
    static Map<Point2D, Building> buildings = new HashMap<>();

    /** Нейтральные миньоны на карте */
    static Map<Long, Minion> neutralMinions = new HashMap<>();

    /** Наша база */
    static Building selfBase;

    /** База врага */
    static Building enemyBase;

    static Building enemyMiddleTower;

    /** Позиции вражеских миньёнов по линиям */
    static Map<LaneType, Integer> enemyMinionPositionIndex = new HashMap<>();

    /** Карта с контрольными точками по линиям */
    static Map<LaneType, Point2D[]> laneWaypoints = new HashMap<>();

    /** Карта с позициями центров линий */
    static Map<LaneType, Point2D> minSaveIndexLaneWaypoint = new HashMap<>();

    /** На каком тике последний раз обновляли информацию о деревьях, строениях и юнитах */
    private static int lastUpdateTickIndex = -1;

    /** Список вражескию юнитов (+ агрессивные нейтральные) */
    static List<LivingUnit> enemyUnits = new ArrayList<>();

    /** Вражеские миньёны */
    static List<Minion> enemyMinion = new ArrayList<>();

    /** Наши миньёны */
    private static List<Minion> selfMinion = new ArrayList<>();

    /** Союзники */
    static List<Wizard> selfWizards = new ArrayList<>();

    /** Враги */
    private static List<Wizard> enemyWizards = new ArrayList<>();

    /** Карта хранящая создателей снарядов. Позиции создателей не меняются со временем! */
    private static Map<Long, Unit> projectileCreatorPositions = new HashMap<>();


    /**
     * Инициализация данных
     */
    static void init(World world, Wizard self) {
        if (laneWaypoints.isEmpty()) {
            laneWaypoints.put(LaneType.BOTTOM, getWaypointsBottom());
            laneWaypoints.put(LaneType.TOP, getWaypointsTop());
            laneWaypoints.put(LaneType.MIDDLE, getWaypointsMiddle());
            minSaveIndexLaneWaypoint.put(LaneType.BOTTOM, new Point2D(3500, 3500));
            minSaveIndexLaneWaypoint.put(LaneType.TOP, new Point2D(500, 500));
            minSaveIndexLaneWaypoint.put(LaneType.MIDDLE, new Point2D(2000, 2000));
        }

        initEnemyMinionPosition();
        initWizards(world, self);

        List<LivingUnit> selfFactionUnits = new ArrayList<>();
        Arrays.stream(world.getBuildings())
                .filter(building -> building.getFaction().equals(self.getFaction()))
                .forEach(selfFactionUnits::add);
        selfFactionUnits.addAll(selfMinion);
        selfFactionUnits.addAll(selfWizards);
        selfFactionUnits.add(self);

        if (lastUpdateTickIndex < world.getTickIndex()) {
            updateTrees(world, selfFactionUnits);
            lastUpdateTickIndex += UPDATE_TICK_RANGE;
        }
        if (buildings.isEmpty()) {
            initBuildings(world, self);
        }
        updateBuildings(world, selfFactionUnits);
        updateNeutralMinions(world, selfFactionUnits);

        buildings.values().forEach(building -> {
            if (BuildingType.GUARDIAN_TOWER.equals(building.getType())) {
                if (((int) building.getX()) == 2070 && building.getY() == 1600) {
                    enemyMiddleTower = building;
                }
            } else if (BuildingType.FACTION_BASE.equals(building.getType())) {
                if (building.getFaction().equals(self.getFaction())) {
                    selfBase = building;
                } else {
                    enemyBase = building;
                }
            }
        });

        initEnemyUnits(world, self);
        updateProjectileCreatorPositions(world);
    }

    /** Обновляем данные о создателях снарядов */
    private static void updateProjectileCreatorPositions(World world) {
        Map<Long, Unit> tmpMap = new HashMap<>();
        for (Projectile projectile : world.getProjectiles()) {
            if (projectileCreatorPositions.containsKey(projectile.getId())) {
                tmpMap.put(projectile.getId(), projectileCreatorPositions.get(projectile.getId()));
            } else {
                Wizard ownerWizard = StrategyHelper.getWizardById(world.getWizards(), projectile.getOwnerUnitId());
                if (ownerWizard != null) {
                    tmpMap.put(projectile.getId(), ownerWizard);
                } else {
                    Minion ownerMinion = StrategyHelper.getMinionById(world.getMinions(), projectile.getOwnerUnitId());
                    if (ownerMinion != null) {
                        tmpMap.put(projectile.getId(), ownerMinion);
                    } else {
                        tmpMap.put(projectile.getId(), new Point2D(projectile));
                    }
                }
            }
        }
        projectileCreatorPositions.clear();
        projectileCreatorPositions.putAll(tmpMap);
    }

    /** Сохраняем вражеских юнитов */
    private static void initEnemyUnits(World world, Wizard self) {
        enemyUnits = new ArrayList<>();
        enemyMinion = new ArrayList<>();
        selfMinion = new ArrayList<>();

        enemyUnits.addAll(Arrays.stream(world.getWizards()).filter(unit -> unit.getFaction() != self.getFaction()).collect(Collectors.toList()));
        enemyUnits.addAll(buildings.values().stream().filter(unit -> unit.getFaction() != self.getFaction()).collect(Collectors.toList()));

        for (Minion minion : world.getMinions()) {
            if (!minion.getFaction().equals(Faction.NEUTRAL)) {
                if (minion.getFaction().equals(self.getFaction())) {
                    selfMinion.add(minion);
                } else {
                    enemyMinion.add(minion);
                }
            }
        }

        neutralMinions.values().stream()
                .filter(minion -> minion.getSpeedY() != 0 || minion.getSpeedX() != 0 || minion.getLife() != minion.getMaxLife())
                .forEach(minion -> {
                    List<LivingUnit> livingUnits = StrategyHelper.getNearLivingUnit(minion, 400.0, getNeutralMinions(), Faction.NEUTRAL);
                    if (livingUnits != null && !livingUnits.isEmpty()) {
                        enemyMinion.addAll(livingUnits.stream().map(livingUnit -> (Minion) livingUnit).collect(Collectors.toList()));
                    }
                });

        enemyUnits.addAll(enemyMinion);
    }

    /** Сохраняем волшебников */
    private static void initWizards(World world, Wizard self) {
        selfWizards = new ArrayList<>();
        enemyWizards = new ArrayList<>();

        Arrays.stream(world.getWizards())
                .filter(wizard -> !wizard.isMe())
                .forEach(wizard -> {
                    if (wizard.getFaction().equals(self.getFaction())) {
                        selfWizards.add(wizard);
                    } else {
                        enemyWizards.add(wizard);
                    }
                });
    }

    /**
     * Пересчитываем позиции миньёнов противника
     */
    private static void initEnemyMinionPosition() {
        enemyMinionPositionIndex.clear();
        for (Minion minion : enemyMinion) {
            if (!minion.getFaction().equals(Faction.NEUTRAL)) {
                LaneType laneType = StrategyHelper.getLane(minion);
                if (laneType != null) {
                    Point2D[] waypoints = laneWaypoints.get(laneType);
                    Integer indexWaypoint = StrategyHelper.getNearPointIndex(minion, waypoints);
                    if (waypoints[indexWaypoint].getDistanceTo(minion) > WAYPOINT_RADIUS
                            && indexWaypoint != waypoints.length - 1) {
                        indexWaypoint++;
                    }
                    if (enemyMinionPositionIndex.get(laneType) == null || enemyMinionPositionIndex.get(laneType) > indexWaypoint) {
                        enemyMinionPositionIndex.put(laneType, indexWaypoint);
                    }
                }
            }
        }
    }

    /**
     * Инициализация строений
     */
    private static void initBuildings(World world, Wizard self) {
        for (Building building : world.getBuildings()) {
            buildings.put(new Point2D(building), building);
            buildings.put(new Point2D(4000 - building.getX(), 4000 - building.getY()),
                    new Building(0, 4000 - building.getX(), 4000 - building.getY(), building.getSpeedX(), building.getSpeedY(),
                            building.getAngle(), Faction.ACADEMY.equals(self.getFaction()) ? Faction.RENEGADES : Faction.ACADEMY,
                            building.getRadius(), building.getLife(), building.getMaxLife(), new Status[0], building.getType(),
                            building.getVisionRange(), building.getAttackRange(), building.getDamage(), building.getCooldownTicks(),
                            building.getRemainingActionCooldownTicks()));
        }
    }


    /**
     * Обновление строений
     */
    private static void updateBuildings(World world, List<LivingUnit> selfFactionUnits) {
        Map<Point2D, Building> buildingWorld = new HashMap<>();
        Arrays.stream(world.getBuildings()).forEach(building -> buildingWorld.put(new Point2D(building), building));

        List<Building> haveBuildingsList = new ArrayList<>();
        haveBuildingsList.addAll(buildings.values());

        haveBuildingsList.stream()
                .filter(building -> !buildingWorld.containsKey(new Point2D(building)))
                .filter(building -> isViewUnit(building, selfFactionUnits))
                .forEach(building -> {
                    buildings.remove(new Point2D(building));
                    if (((int) building.getX()) == 2070 && building.getY() == 1600) {
                        enemyMiddleTower = null;
                    }
                });

        Arrays.stream(world.getBuildings())
                .forEach(building -> {
                    buildings.put(new Point2D(building), building);
                    if (((int) building.getX()) == 2070 && building.getY() == 1600) {
                        enemyMiddleTower = building;
                    }
                });
    }

    /**
     * Инициализация нейтральных миньонов
     */
    private static void initNeutralMinions(World world) {
        for (Minion minion : world.getMinions()) {
            if (minion.getFaction() == Faction.NEUTRAL) {
                neutralMinions.put(minion.getId(), minion);
            }
        }
    }

    /**
     * Обновление нейтральных миньонов
     */
    private static void updateNeutralMinions(World world, List<LivingUnit> selfFactionUnits) {
        List<Minion> haveMinionsList = new ArrayList<>();
        haveMinionsList.addAll(neutralMinions.values());
        haveMinionsList.stream()
                .filter(minion -> isViewUnit(minion, selfFactionUnits))
                .forEach(minion -> neutralMinions.remove(minion.getId()));

        initNeutralMinions(world);
    }

    /**
     * Инициализация деревьев
     */
    private static void initTrees(World world) {
        for (Tree tree : world.getTrees()) {
            trees.put(tree.getId(), tree);
        }
    }

    /**
     * Обновление деревьев
     */
    private static void updateTrees(World world, List<LivingUnit> selfFactionUnits) {
        Map<Long, Tree> treesWorld = new HashMap<>();
        Arrays.stream(world.getTrees()).forEach(tree -> treesWorld.put(tree.getId(), tree));

        List<Tree> haveTreeList = new ArrayList<>();
        haveTreeList.addAll(trees.values());

        haveTreeList.stream()
                .filter(tree -> !treesWorld.containsKey(tree.getId()))
                .filter(tree -> isViewUnit(tree, selfFactionUnits))
                .forEach(tree -> trees.remove(tree.getId()));

        initTrees(world);
    }

    static Unit getProjectileOwnerPoint(Long id) {
        return projectileCreatorPositions.get(id);
    }

    static List<LivingUnit> getTrees() {
        return trees.values().stream().collect(Collectors.toList());
    }

    static List<LivingUnit> getNeutralMinions() {
        return neutralMinions.values().stream().collect(Collectors.toList());
    }

    static List<LivingUnit> getSelfMinions() {
        return selfMinion.stream().collect(Collectors.toList());
    }

    static List<LivingUnit> getEnemyMinions() {
        return enemyMinion.stream().collect(Collectors.toList());
    }

    static List<LivingUnit> getEnemyWizards() {
        return enemyWizards.stream().collect(Collectors.toList());
    }

    /**
     * @return список юнитов, которые могут мешать движению
     */
    static List<LivingUnit> getBlockUnits(World world) {
        List<LivingUnit> blockUnits = new ArrayList<>();
        blockUnits.addAll(buildings.values());
        blockUnits.addAll(trees.values());
        blockUnits.addAll(neutralMinions.values());
        blockUnits.addAll(Arrays.asList(world.getMinions()));
        blockUnits.addAll(Arrays.asList(world.getWizards()));

        return blockUnits;
    }

    static Integer getMinSafeLaneWaypointIndex(LaneType laneType) {
        return StrategyHelper.getWaypointIndex(minSaveIndexLaneWaypoint.get(laneType), getWaypoints(laneType));
    }

    static boolean isEnemyPositionLessSafePosition(LaneType laneType, Integer addIndex) {
        return enemyMinionPositionIndex.get(laneType) != null && enemyMinionPositionIndex.get(laneType) < getMinSafeLaneWaypointIndex(laneType) + addIndex;
    }

    /**
     * @param viewUnits список союзных юнитов
     * @return {true} если {unit} находится в поле зрения хотябы одного юнита из списка {viewUnits}
     */
    private static boolean isViewUnit(LivingUnit unit, List<LivingUnit> viewUnits) {
        for (LivingUnit livingUnit : viewUnits) {
            Double distance = livingUnit.getDistanceTo(unit) - (unit instanceof Building ? 0 : livingUnit.getRadius());
            if (livingUnit instanceof Wizard && ((Wizard) livingUnit).getVisionRange() > distance
                    || livingUnit instanceof Minion && ((Minion) livingUnit).getVisionRange() > distance
                    || livingUnit instanceof Building && ((Building) livingUnit).getVisionRange() > distance) {
                return true;
            }
        }
        return false;
    }

    static Point2D[] getWaypoints(LaneType laneType) {
        Point2D[] waypoints = laneWaypoints.get(laneType);
        if (LaneType.MIDDLE.equals(laneType)) {
            if (!StrategyHelper.getNearLivingUnit(selfBase, SaveHelper.SAFE_BASE_DISTANCE, enemyUnits, enemyBase.getFaction()).isEmpty()) {
                Integer countTop = StrategyHelper.getNearLivingUnit(getWaypoints(LaneType.TOP)[1], 400.0, enemyUnits, enemyBase.getFaction()).size();
                Integer countBottom = StrategyHelper.getNearLivingUnit(getWaypoints(LaneType.BOTTOM)[1], 400.0, enemyUnits, enemyBase.getFaction()).size();
                if (countTop > countBottom) {
                    waypoints[1] = MIDDLE_TWO_BOTTOM_POINT;
                } else {
                    waypoints[1] = MIDDLE_TWO_TOP_POINT;
                }
            }
        }
        return waypoints;
    }

    /** Точки по которым бегает волшебник по верхней линии */
    static Point2D[] getWaypointsTop() {
        return new Point2D[] {
                new Point2D(100.0D, 3900.0D),
                new Point2D(100.0D, 3500.0D),
                new Point2D(200.0D, 3200.0D),
                new Point2D(200.0D, 2800.0D),
                new Point2D(200.0D, 2400.0D),
                new Point2D(200.0D, 2000.0D),
                new Point2D(200.0D, 1600.0D),
                new Point2D(200.0D, 1200.0D),
                new Point2D(200.0D, 800.0D),

                new Point2D(500D, 500D),

                new Point2D(800D, 200.0D),
                new Point2D(1200D, 200.0D),
                new Point2D(1600D, 200.0D),
                new Point2D(2000D, 200.0D),
                new Point2D(2400D, 200.0D),
                new Point2D(2800D, 200.0D),
                new Point2D(3200D, 200.0D),
                new Point2D(3600D, 200.0D),
                new Point2D(3800.0D, 200.0D)
        };
    }

    /** Точки по которым бегает волшебник по нижней линии */
    static Point2D[] getWaypointsBottom() {
        return new Point2D[]{
                new Point2D(100.0D, 3900.0D),
                new Point2D(500.0D, 3900.0D),
                new Point2D(800.0D, 3800.0D),
                new Point2D(1200.0D, 3800.0D),
                new Point2D(1600.0D, 3800.0D),
                new Point2D(2000.0D, 3800.0D),
                new Point2D(2400.0D, 3800.0D),
                new Point2D(2800.0D, 3800.0D),
                new Point2D(3200.0D, 3800.0D),

                new Point2D(3500D, 3500D),

                new Point2D(3800D, 3200.0D),
                new Point2D(3800D, 2800.0D),
                new Point2D(3800D, 2400.0D),
                new Point2D(3800D, 2000.0D),
                new Point2D(3800D, 1600.0D),
                new Point2D(3800D, 1200.0D),
                new Point2D(3800D, 800.0D),
                new Point2D(3900D, 500.0D),
                new Point2D(3900.0D, 100.0D)
        };
    }

    /** Точки по которым бегает волшебник по средней линии */
    static Point2D[] getWaypointsMiddle() {
        return new Point2D[] {
                new Point2D(100.0D, 3900.0D),
                new Point2D(200.0D, 3200.0D),
                new Point2D(800.0D, 3200.0D),
                new Point2D(1200.0D, 2800.0D),
                new Point2D(1600.0D, 2400.0D),
                new Point2D(2000.0D, 2000.0D),
                new Point2D(2400.0D, 1600.0D),
                new Point2D(2800.0D, 1200.0D),
                new Point2D(3200.0D, 800.0D),
                new Point2D(3600.0D, 400.0D)
        };
    }
}
