import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Класс для работы со спасением волшебника
 */
class SaveHelper {

    /** Если расстояние от вражеского миньёна до нашей базы меньше этого числа, то надо защищать базу */
    static final double SAFE_BASE_DISTANCE = 600.0;

    /** Если здоровье волшебника в процентах меньше данного числа, то нужно спасаться */
    private static final double LOW_HP_FACTOR = 0.35D;

    /** Дистанция, на которой держимся от вражеских миньонов */
    private static final int SAFE_DISTANCE = 250;

    /** Дистанция, на которой держимся от вражеских миньонов дротиков */
    private static final int SAFE_DISTANCE_FETISH = 400;

    /** Если миньон подошёл к нам на расстояние меньше этого числа, то спасаемся */
    private static final int SAFE_MINION_ATTACK_DISTANCE = 100;


    /** Создаём singleton объект данного класса */
    private static SaveHelper helper;
    static synchronized SaveHelper getInstance() {
        if (helper == null) {
            helper = new SaveHelper();
        }
        return helper;
    }


    /** Данные которые пришли нам в данный тик */
    private Wizard self;
    private World world;
    private Game game;
    private Move move;

    /** Хелпер для работы с движением */
    private MovesHelper movesHelper;

    /** Хелпер для работы с атакой */
    private AttackHelper attackHelper;

    private boolean saveBase = false;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        this.movesHelper = MovesHelper.getInstance();
        this.attackHelper = AttackHelper.getInstance();

        if (DataHelper.selfBase.getDistanceTo(self) < SAFE_BASE_DISTANCE) {
            saveBase = false;
        }
    }


    /**
     * Если осталось мало жизненной энергии, отступаем назад.
     */
    boolean safeWizard() {
        double count = 0.0;

//        if (DataHelper.enemyBase != null && DataHelper.enemyBase.getDistanceTo(self) < 900
//                && DataHelper.enemyBase.getLife() > DataHelper.enemyBase.getMaxLife() / 2
//                && !game.isSkillsEnabled()) {
//            count += 2;
//        }


        LivingUnit nearEnemyWizard = StrategyHelper.getNearUnit(self, DataHelper.getEnemyWizards());
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR
                && (nearEnemyWizard != null && nearEnemyWizard.getDistanceTo(self) < self.getVisionRange() + self.getRadius())) {
            count += 2;
        }

        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getDistanceTo(self) < self.getVisionRange()) {
                if (intersectionWithTile(self, projectile, true)) {
                    count += 1;
                }
            }
        }

        LivingUnit nearestTarget = attackHelper.nearestTarget;
        if (attackHelper.isFinishHim() && nearestTarget.getLife() < self.getLife() && self.getLife() > self.getMaxLife()/2) {
            count -= game.isSkillsEnabled() ? 1 : 2;
        }

        if (self.getDistanceTo(DataHelper.enemyBase) < self.getCastRange()
                && DataHelper.enemyBase.getLife() < DataHelper.enemyBase.getMaxLife() / 4) {
            count -= 10;
        }

        count += getPointWarningLevel(self);

        return count >= 1;
    }

    Integer getPointWarningLevel(Unit unit) {
        Integer count = 0;

        Building preferableBuilding = (Building) attackHelper.getNearestTarget(DataHelper.buildings.values().toArray(new Building[DataHelper.buildings.size()]), true);
        if (preferableBuilding != null) {
            double distanceToBuilding = unit.getDistanceTo(preferableBuilding);
            if (distanceToBuilding < preferableBuilding.getAttackRange() + self.getRadius()
                    && StrategyHelper.getNearLivingUnit(preferableBuilding, preferableBuilding.getAttackRange(), DataHelper.getSelfMinions(), unit.getFaction()).isEmpty()) {
                count += 2;
            }
            if (distanceToBuilding < preferableBuilding.getAttackRange() / 2) {
                count += 1;
            }
        }

        for (Minion minion : DataHelper.enemyMinion) {
            double distance = unit.getDistanceTo(minion);

            int saveDistance = SAFE_DISTANCE;
            if (MinionType.FETISH_BLOWDART.equals(minion.getType()))
                saveDistance = SAFE_DISTANCE_FETISH;

            if (distance < SAFE_MINION_ATTACK_DISTANCE) {
                if (Math.abs(minion.getAngleTo(unit)) <  Math.abs(game.getStaffSector() / 2.0))
                    count += 2;
                else
                    count++;
            } else if (distance < saveDistance) {
                count++;
            }
        }

        int hardEnemy = 0;
        List<LivingUnit> nearEnemyWizard = StrategyHelper.getNearLivingUnit(unit, self.getVisionRange() + self.getRadius(), Arrays.asList(world.getWizards()), DataHelper.enemyBase.getFaction());
        Double distance = ((self.getRadius() + 10) / StrategyHelper.getSpeedWithMovementBonus(self, game, game.getWizardStrafeSpeed())) * game.getMagicMissileSpeed();
        distance += self.getRadius() + 40;
        for (LivingUnit livingUnit : nearEnemyWizard) {
            if (livingUnit instanceof Wizard) {
                Wizard wizard = (Wizard) livingUnit;
                if (wizard.getCastRange() >= unit.getDistanceTo(wizard) - self.getRadius()
                        && distance >= unit.getDistanceTo(wizard)
                        && StrategyHelper.countShotsForKill(self, wizard, game) >= StrategyHelper.countShotsForKill(wizard, self, game)/2) {
                    if (StrategyHelper.isHaveStatus(wizard, StatusType.EMPOWERED)) {
                        hardEnemy += 2;
                    } else {
                        hardEnemy++;
                    }
                }
            }
        }

        if (hardEnemy >= 1) {
            count++;
        }

        if (hardEnemy >= 1) {
            count += hardEnemy-1;
        }


        return count;
    }

    /**
     * Двигаемся назад, (спасаемся)
     */
    void runBack() {
        LivingUnit nearestTarget = attackHelper.getNearestTarget(false);

        if (nearestTarget != null) {
            attackHelper.nearestTarget = nearestTarget;
            attackHelper.attack();
        }

        boolean flyTile = false;
        Projectile tile = null;
        List<LivingUnit> list = new ArrayList<>();
        list.add(self);
        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getDistanceTo(self) < self.getVisionRange()
                    && StrategyHelper.countUnitByPath(list, new Point2D(projectile, game, false), StrategyHelper.getAnglePoint(projectile, self.getCastRange(), projectile.getAngle() + Math.PI)) != 0) {
                flyTile = true;
                tile = projectile;
            }
        }

        if (flyTile && intersectionWithTile(self, tile, false)) {
            Double speed = game.getDartSpeed();
            if (ProjectileType.FIREBALL.equals(tile.getType())) {
                speed = game.getFireballSpeed();
            } else if (ProjectileType.FROST_BOLT.equals(tile.getType())) {
                speed = game.getFrostBoltSpeed();
            } else if (ProjectileType.MAGIC_MISSILE.equals(tile.getType())) {
                speed = game.getMagicMissileSpeed();
            }

            Double distanceMove = (self.getDistanceTo(tile)/speed) * StrategyHelper.getSpeedWithMovementBonus(self, game, game.getWizardStrafeSpeed());
            Point2D pointLeft = StrategyHelper.getAnglePoint(self, distanceMove, self.getAngle() + self.getAngleTo(tile) + Math.PI / 2);
            Point2D pointRight = StrategyHelper.getAnglePoint(self, distanceMove, self.getAngle() + self.getAngleTo(tile) - Math.PI / 2);

            Point2D pointTo = null;
            if (StrategyHelper.countUnitByPath(DataHelper.getBlockUnits(world), self, pointLeft) == 0 && !intersectionWithTile(pointLeft, tile, false)) {
                pointTo = pointLeft;
            }
            if (StrategyHelper.countUnitByPath(DataHelper.getBlockUnits(world), self, pointRight) == 0 && !intersectionWithTile(pointRight, tile, false)) {
                if (pointTo == null) {
                    pointTo = pointRight;
                } else if (getPointWarningLevel(pointLeft) > getPointWarningLevel(pointRight)){
                    pointTo = pointRight;
                }
            }
            if (pointTo != null) {
                movesHelper.goTo(pointTo, true);
                return;
            }
        }

        movesHelper.goTo(movesHelper.getPreviousWaypoint(), true);

        if (nearestTarget != null && (movesHelper.currentMovePoint == null || !StrategyHelper.getUnitByPath(Arrays.asList(world.getTrees()), self, movesHelper.currentMovePoint).isEmpty())) {
            double angle = self.getAngleTo(nearestTarget);
            move.setTurn(angle);
        }
    }



    private boolean intersectionWithTile(LivingUnit point, Projectile projectile, boolean addRadius) {
        List<LivingUnit> list = new ArrayList<>();
        list.add(point);

        Unit ownerUnit = DataHelper.getProjectileOwnerPoint(projectile.getId());
        Double distance;
        if (ownerUnit != null && ownerUnit instanceof Wizard) {
            distance = ((Wizard) ownerUnit).getCastRange() - projectile.getDistanceTo(ownerUnit);
        } else if (ownerUnit != null && ownerUnit instanceof Minion) {
            distance = game.getFetishBlowdartAttackRange() - projectile.getDistanceTo(ownerUnit);
        } else {
            distance = 600 - projectile.getDistanceTo(ownerUnit);
        }
        distance += 10;
        return StrategyHelper.countUnitByPath(list, new Point2D(projectile, game, addRadius), StrategyHelper.getAnglePoint(projectile, distance, projectile.getAngle() + Math.PI)) != 0;

    }

    /**
     * @return {true}, если базе грозит опасность
     */
    boolean saveBase() {
        if (saveBase) {
            return true;
        }
        if (((DataHelper.isEnemyPositionLessSafePosition(LaneType.BOTTOM, -5) && !LaneType.BOTTOM.equals(StrategyHelper.getLane(self)))
                ||  (DataHelper.isEnemyPositionLessSafePosition(LaneType.TOP, -5) && !LaneType.TOP.equals(StrategyHelper.getLane(self)))
                ||  (DataHelper.isEnemyPositionLessSafePosition(LaneType.MIDDLE, -2) && !LaneType.MIDDLE.equals(StrategyHelper.getLane(self))))
                && DataHelper.selfBase.getDistanceTo(self) > SAFE_BASE_DISTANCE
                && self.getDistanceTo(DataHelper.enemyBase) > 2000) {
            saveBase = true;
            return true;
        }
        return false;
    }

    /**
     * Возвращаемся на базу, если ей грозит опасность
     */
    void goToBase() {
        if (StrategyHelper.getLane(self) != null) {
            movesHelper.goTo(movesHelper.getPreviousWaypoint(), false);
        } else {
            Integer position = DataHelper.enemyMinionPositionIndex.get(movesHelper.laneType);
            Integer minSafe = StrategyHelper.getMinSafePosition(movesHelper.laneType);
            if (position != null && position < minSafe) {
                movesHelper.nextWaypoint = new Point2D(DataHelper.selfBase);
                movesHelper.goTo(movesHelper.getBackToLaneWaypoint(), false);
            } else {
                movesHelper.goTo(DataHelper.minSaveIndexLaneWaypoint.get(movesHelper.laneType), false);
            }
        }
    }
}