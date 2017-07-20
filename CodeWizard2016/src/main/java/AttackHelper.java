import model.*;

import java.util.List;

/**
 * Класс для работы с атакой
 */
class AttackHelper {

    private static final int NOT_ATTACK_NEUTRAL_RANGE = 2000;

    /** Создаём singleton объект данного класса */
    private static AttackHelper helper;
    static synchronized AttackHelper getInstance() {
        if (helper == null) {
            helper = new AttackHelper();
            helper.movesHelper = MovesHelper.getInstance();
            helper.skillHelper = SkillHelper.getInstance();
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

    /** Хелпер для работы со скилами  */
    private SkillHelper skillHelper;

    /** Урон магической ракеты на данном тике */
    int magicMissileDamage;

    /** Приоритетная цель */
    LivingUnit nearestTarget;

    /** Позиция в которую переместится вражеский юнит за время полёта снаряда "Для отладки" */
    Point2D attackPoint;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        magicMissileDamage = skillHelper.getDamage();
        nearestTarget = getNearestTarget(self.getLife() > self.getMaxLife() * 0.5);
    }

    /**
     * @return {true}, если есть кого атаковать
     */
    boolean isMayAttack() {
        return nearestTarget != null;
    }

    /**
     * @return {true}, если волшебник может атаковать
     */
    boolean isMayUseAttackAction() {
        return self.getRemainingActionCooldownTicks() == 0
                && self.getMana() >= game.getMagicMissileManacost()
                && (self.getRemainingCooldownTicksByAction()[2] == 0
                        || (StrategyHelper.haveSkill(SkillType.FROST_BOLT, self, game) && self.getRemainingCooldownTicksByAction()[3] == 0)
                        || (StrategyHelper.haveSkill(SkillType.FIREBALL, self, game) && self.getRemainingCooldownTicksByAction()[4] == 0)
                        || (self.getDistanceTo(nearestTarget) < game.getStaffRange() + nearestTarget.getRadius() && self.getRemainingCooldownTicksByAction()[1] == 0));

    }

    /**
     * @return {true}, если противник в радиусе атаки
     */
    boolean isAttackNearestTarget() {
        return nearestTarget != null && self.getDistanceTo(nearestTarget) < self.getCastRange() + nearestTarget.getRadius();
    }

    /**
     * @return {true}, если можем кастануть фаербол
     */
    private boolean isMayUseFrostBolt() {
        return StrategyHelper.haveSkill(SkillType.FROST_BOLT, self, game) && self.getMana() > game.getFrostBoltManacost()
                && self.getRemainingCooldownTicksByAction()[3] == 0
                && (nearestTarget instanceof Wizard || self.getMana() > game.getFrostBoltManacost() * 2);
    }

    /**
     * @return {true}, если можем кастануть леденую ракету
     */
    private boolean isMayUseFireBall() {
        return StrategyHelper.haveSkill(SkillType.FIREBALL, self, game)
                && self.getMana() > game.getFireballManacost()
                && self.getRemainingCooldownTicksByAction()[4] == 0
                && self.getDistanceTo(nearestTarget) > nearestTarget.getRadius() + game.getFireballRadius() + game.getFireballExplosionMinDamageRange()
                && StrategyHelper.getUnitByPath(DataHelper.getBlockUnits(world), self, nearestTarget, game.getFireballRadius()).isEmpty();
    }

    /**
     * @return {true}, если можем добить волшебника, который является приоритетной целью
     */
    boolean isFinishHim() {
        return (nearestTarget instanceof Wizard || nearestTarget instanceof Building)
                && isFinishHim(nearestTarget);
    }

    /**
     * @return {true}, если можем добить противника {livingUnit}
     */
    private boolean isFinishHim(LivingUnit livingUnit) {
        return livingUnit.getLife() < magicMissileDamage * 3;
    }

    /**
     * Атакуем противника, если он виден. Если нет, то бежим к нему.
     */
    void attack() {
        Point2D nearestPoint = new Point2D(nearestTarget);

        if (nearestPoint.getDistanceTo(self) > self.getCastRange() + nearestTarget.getRadius()
                && StrategyHelper.countUnitByPath(DataHelper.getBlockUnits(world), self, nearestPoint) != 0) {
            nearestPoint = StrategyHelper.getPriorityPoint(DataHelper.getBlockUnits(world), nearestPoint, self);
            if (nearestPoint == null) {
                nearestTarget = getNearestTarget(false);
                if (nearestTarget == null)
                    return;
                nearestPoint = new Point2D(nearestTarget);
            }
        }


        Double speed = game.getMagicMissileSpeed();
        if (isMayUseFrostBolt()) {
            speed = game.getFireballSpeed();
        } else if (isMayUseFireBall()) {
            speed = game.getFrostBoltSpeed();
        }
        Double distanceMove = (self.getDistanceTo(nearestTarget)/speed) * Math.hypot(nearestTarget.getSpeedX(), nearestTarget.getSpeedY());
        distanceMove = distanceMove - (distanceMove/10);
        Double angleMove = nearestTarget.getAngle() + Math.PI + nearestTarget.getAngleTo(new Point2D(nearestTarget.getX() + nearestTarget.getSpeedX(), nearestTarget.getY() + nearestTarget.getSpeedY()));
        Point2D nearestPointWithMove = StrategyHelper.getAnglePoint(nearestTarget, distanceMove, angleMove);

        attackPoint = nearestPointWithMove;
        double distance = self.getDistanceTo(nearestPointWithMove);
        if (distance <= self.getCastRange() + nearestTarget.getRadius()) {
            double angle = self.getAngleTo(nearestPointWithMove);
            move.setTurn(angle);

            if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                if (isMayUseFrostBolt()) {
                    move.setAction(ActionType.FROST_BOLT);
                } else if (self.getDistanceTo(nearestTarget) < game.getStaffRange() + nearestTarget.getRadius() && self.getRemainingCooldownTicksByAction()[1] == 0) {
                    move.setAction(ActionType.STAFF);
                } else if (isMayUseFireBall()) {
                    move.setAction(ActionType.FIREBALL);
                } else {
                    move.setAction(ActionType.MAGIC_MISSILE);
                }

                move.setCastAngle(angle);
                move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());
            }

            //Если противник сильно подбит, то бежим к нему на добивание
            if (nearestTarget instanceof Wizard
                    && isFinishHim()
                    && distance > self.getCastRange() - 200) {
                movesHelper.goTo(nearestPoint, true);
            }

            if (game.isSkillsEnabled()
                    && nearestTarget instanceof Wizard
                    && StrategyHelper.haveSkill(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1, self, game)
                    && StrategyHelper.getNearUnit(nearestTarget, DataHelper.enemyUnits, ((Wizard) nearestTarget).getCastRange()) == null
                    && StrategyHelper.countShotsForKill(self, (Wizard) nearestTarget, game) < StrategyHelper.countShotsForKill((Wizard) nearestTarget, self, game)/1.5) {
                movesHelper.goTo(nearestPoint, true);
            }

            if (nearestTarget.equals(DataHelper.enemyBase) && nearestTarget.getLife() < nearestTarget.getMaxLife() / 2) {
                if (self.getDistanceTo(nearestTarget) > nearestTarget.getRadius() + self.getRadius() + 50) {
                    movesHelper.goTo(nearestPoint, true);
                }
                move.setStrafeSpeed(game.getWizardStrafeSpeed());
                if (StrategyHelper.isHaveStatus(self, StatusType.HASTENED)) {
                    move.setStrafeSpeed(move.getStrafeSpeed() + move.getStrafeSpeed() * game.getHastenedMovementBonusFactor());
                }
            }
        } else {
            movesHelper.goTo(nearestPoint, false);
        }
    }

    /**
     * @return {true}, если по близости есть нейтрал и его можно атаковать
     */
    boolean isMayAttackNeutral() {
        if (self.getDistanceTo(DataHelper.enemyBase) < NOT_ATTACK_NEUTRAL_RANGE) {
            return false;
        }
        List<LivingUnit> nearNeutrals = StrategyHelper.getNearLivingUnit(self, self.getCastRange(), DataHelper.getNeutralMinions(), Faction.NEUTRAL);
        List<LivingUnit> nearSelfMinions = StrategyHelper.getNearLivingUnit(self, self.getCastRange(), DataHelper.getSelfMinions(), self.getFaction());

        return nearNeutrals.size() > 0 && nearSelfMinions.size() > 0 && nearNeutrals.size() < nearSelfMinions.size() - 2;
    }

    /**
     * Атакуем ближайшего нейтрала
     */
    void attackNeutral() {
        nearestTarget = StrategyHelper.getNearUnit(self, DataHelper.getNeutralMinions());
        attack();
    }

    /**
     * Выберает наилучшую цель из списка юнитов
     *
     * @param units       список юнитов
     * @param useFatality подбегь ли к цели для атаки (добивание)
     * @return наилучшую цель из списка
     */
    LivingUnit getNearestTarget(LivingUnit[] units, boolean useFatality) {
        LivingUnit result = null;
        for (LivingUnit livingUnit : units) {
            if (livingUnit.getFaction() != self.getFaction()) {
                double distanceCurrent = self.getDistanceTo(livingUnit);
                if (distanceCurrent < self.getCastRange() + livingUnit.getRadius() + (useFatality && !(livingUnit instanceof Minion) ? 300 : 0)) {
                    if (result == null) {
                        result = livingUnit;
                    } else {
                        int healthCurrent = livingUnit.getLife();
                        double distanceNearest = self.getDistanceTo(result);
                        int healthNearest = result.getLife();

                        if ((healthNearest / (double) result.getMaxLife()) * distanceNearest > (healthCurrent / (double) livingUnit.getMaxLife()) * distanceCurrent) {
                            result = livingUnit;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Определяем наилучшую цель, в которую будем стрелять
     *
     * @param useFatality  подбегь ли к цели для атаки (добивание)
     * @return наилучшая цель
     */
    LivingUnit getNearestTarget(boolean useFatality) {
        Building preferableBuilding = (Building) getNearestTarget(world.getBuildings(), useFatality);
        Wizard preferableWizard = (Wizard) getNearestTarget(world.getWizards(), useFatality);
        Minion preferableMinions = (Minion) getNearestTarget(DataHelper.enemyMinion.toArray(new Minion[DataHelper.enemyMinion.size()]), useFatality);
        LivingUnit nearSelfBase = StrategyHelper.getNearUnit(DataHelper.selfBase, DataHelper.enemyUnits, 200.0);

        double buildingCoff = 0.0;
        double wizardCoff = 0.0;
        double minionCoff = 0.0;
        double nearSelfBaseCoff = 0.0;
        if (preferableBuilding != null) {
            buildingCoff = (preferableBuilding.getLife() / ((double) preferableBuilding.getMaxLife())) * self.getDistanceTo(preferableBuilding);
            if (preferableBuilding.getType().equals(BuildingType.FACTION_BASE))
                buildingCoff = buildingCoff/5;
        }
        if (preferableWizard != null) {
            wizardCoff = (preferableWizard.getLife() / ((double) preferableWizard.getMaxLife() * 4)) * self.getDistanceTo(preferableWizard);
        }
        if (preferableMinions != null) {
            minionCoff = (preferableMinions.getLife() / (double) preferableMinions.getMaxLife()) * self.getDistanceTo(preferableMinions);
        }
        if (nearSelfBase != null && self.getDistanceTo(DataHelper.selfBase) < 1000) {
            nearSelfBaseCoff = (nearSelfBase.getLife() / ((double) nearSelfBase.getMaxLife() * 3)) * nearSelfBase.getDistanceTo(DataHelper.selfBase)
                    * (DataHelper.selfBase.getLife() / ((double) DataHelper.selfBase.getMaxLife()));
        }

        LivingUnit nearestUnit = preferableBuilding;
        double currentCoff = buildingCoff;
        if (minionCoff != 0 && (nearestUnit == null || currentCoff > minionCoff)) {
            nearestUnit = preferableMinions;
            currentCoff = minionCoff;
        }
        if (wizardCoff != 0 && (nearestUnit == null || currentCoff > wizardCoff)) {
            nearestUnit = preferableWizard;
            currentCoff = wizardCoff;
        }
        if (nearSelfBaseCoff != 0 && (nearestUnit == null || currentCoff > nearSelfBaseCoff)) {
            nearestUnit = nearSelfBase;
        }

        return nearestUnit;
    }
}
