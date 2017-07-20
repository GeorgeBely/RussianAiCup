import model.*;
import java.util.ArrayList;
import java.util.List;

public final class MyStrategy implements Strategy {
    public static List<Trooper> teamTroopers;
    public static List<Trooper> enemyTroopers;
    public static Trooper self;
    public static World world;
    public static Game game;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        teamTroopers = new ArrayList<>();
        enemyTroopers = new ArrayList<>();
        MyStrategy.self = self;
        MyStrategy.world = world;
        MyStrategy.game = game;

        for (Trooper trooper : world.getTroopers())
            if (trooper.isTeammate())
                teamTroopers.add(trooper);
            else
                enemyTroopers.add(trooper);

        if (Helper.nodeMass.isEmpty())
            Helper.initPositions();

        for (Player player : world.getPlayers()) {
            if (!"ManGeorge".equals(player.getName()) && !"MyStrategy".equals(player.getName()) && player.getApproximateX() != -1) {
                Helper.positions[1] = new Position(player.getApproximateX(), player.getApproximateY());
                Helper.toPosition = Helper.positions[1];
                Helper.checkUseRequest = true;
            }
        }

        if (actionMedikit(move))
            return;

        if (actionThrowGrenade(move))
            return;

        if (actionFieldRation(move))
            return;

        if (actionMedic(move))
            return;

        if (actionShoot(move))
            return;

        if (actionSniper(move))
            return;

        if (actionCommander(move))
            return;

        if (actionTakeBonus(move))
            return;

        if (actionMovePoint(move))
            return;

        move.setAction(ActionType.END_TURN);
    }

    /**
     * Реализует действия лечения для медика, если есть в команде персонаж с не полным здоровьем.
     *
     * @return true, если совершено действие.
     */
    private boolean actionMedic(Move move) {
        if (self.getType().equals(TrooperType.FIELD_MEDIC) && self.getActionPoints() >= game.getFieldMedicHealCost() && teamTroopers.size() != 1) {
            Trooper helpTrooper = null;
            for (Trooper trooper : teamTroopers) {
                if (trooper.getHitpoints() < trooper.getMaximalHitpoints()) {
                    if (helpTrooper == null || self.getDistanceTo(trooper) < self.getDistanceTo(helpTrooper))
                        helpTrooper = trooper;
                }
            }
            if (helpTrooper != null){
                if (Math.abs(helpTrooper.getX() - self.getX()) + Math.abs(helpTrooper.getY() - self.getY()) <= 1) {
                    move.setAction(ActionType.HEAL);
                    move.setX(helpTrooper.getX());
                    move.setY(helpTrooper.getY());
                    return true;
                } else {
                    if (self.getActionPoints() >= game.getStandingMoveCost())
                        if (actionPath(move, new Position(helpTrooper)))
                            return true;
                }
            }
        }
        return false;
    }

    /**
     * Вызывает самолёт разведчик, для получения данных о местонождении противника, и отряд двигается туда.
     *
     * @return true, если имеется такая возможность.
     */
    private boolean actionCommander(Move move) {
        if (TrooperType.COMMANDER.equals(self.getType())) {
            if (!Helper.checkUseRequest && self.getActionPoints() >= game.getCommanderRequestEnemyDispositionCost()) {
                move.setAction(ActionType.REQUEST_ENEMY_DISPOSITION);
                Helper.checkUseRequest = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Если снайпер сможет стрелять из положения ниже текущего, то изменяет положение на более низкое.
     *
     * @return true, если имеется такая возможность.
     */
    private boolean actionSniper(Move move) {
        if (self.getType().equals(TrooperType.SNIPER) && !self.getStance().equals(TrooperStance.PRONE)
                && self.getActionPoints() >= game.getStanceChangeCost()) {
            for (Trooper trooper : enemyTroopers) {
                if (world.isVisible(self.getShootingRange(), self.getX(), self.getY(),
                        TrooperStance.PRONE, trooper.getX(), trooper.getY(), trooper.getStance())) {
                    move.setAction(ActionType.LOWER_STANCE);
                    move.setX(self.getX());
                    move.setY(self.getY());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Использование аптечки.
     *
     * @return true, если совершено действие.
     */
    private boolean actionMedikit(Move move) {
        if (self.isHoldingMedikit() && self.getActionPoints() >= game.getMedikitUseCost()) {
            for (Trooper trooper : teamTroopers) {
                if (trooper.getHitpoints() < trooper.getMaximalHitpoints() - game.getMedikitHealSelfBonusHitpoints()) {
                    if (self.getDistanceTo(trooper) <= 1) {
                        move.setAction(ActionType.USE_MEDIKIT);
                        move.setX(self.getX());
                        move.setY(self.getY());
                        return true;
                    } else if (self.getDistanceTo(trooper) >= 3 && Helper.haveEnoughPointsForMove(trooper, 0, game.getMedikitUseCost())) {
                        if (actionPath(move, new Position(trooper)))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Использование гранаты.
     *
     * @return true, если совершено действие.
     */
    private boolean actionThrowGrenade(Move move) {
        for (Trooper trooper : enemyTroopers)
            if (self.isHoldingGrenade() && self.getActionPoints() >= game.getGrenadeThrowCost()) {
                if (self.getDistanceTo(trooper) <= game.getGrenadeThrowRange()) {
                    move.setAction(ActionType.THROW_GRENADE);
                    move.setX(trooper.getX());
                    move.setY(trooper.getY());
                    return true;
                } else {
                    if (Helper.haveEnoughPointsForMove(trooper, game.getGrenadeThrowRange(), game.getGrenadeThrowCost()))
                        if (actionMove(move, new Position(trooper)))
                            return true;
                }
            }
        return false;
    }

    /**
     * Использование сух пойка(используется, когда есть враг в поле досигаемости).
     *
     * @return true, если совершено действие.
     */
    private boolean actionFieldRation(Move move) {
        if (self.isHoldingFieldRation() && self.getActionPoints() >= game.getFieldRationEatCost() &&
                self.getActionPoints() < self.getInitialActionPoints()-game.getFieldRationBonusActionPoints()) {
            for (Trooper trooper : enemyTroopers) {
                if (world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(),
                        trooper.getX(), trooper.getY(), trooper.getStance())) {
                    move.setAction(ActionType.EAT_FIELD_RATION);
                    move.setX(self.getX());
                    move.setY(self.getY());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Выстрел в противника. Если возможно, то съедание сух пойка.
     *
     * @return true, если совершено действие.
     */
    private boolean actionShoot(Move move) {
        Trooper shotEnemyTrooper = null;
        Trooper notShotEnemyTrooper = null;

        for (Trooper trooper : enemyTroopers) {
            boolean canShoot = world.isVisible(self.getShootingRange(),
                    self.getX(), self.getY(), self.getStance(),
                    trooper.getX(), trooper.getY(), trooper.getStance());

            if (canShoot) {
                if(shotEnemyTrooper == null || shotEnemyTrooper.getHitpoints() > trooper.getHitpoints())
                    shotEnemyTrooper = trooper;
            } else {
                if (notShotEnemyTrooper == null || self.getDistanceTo(notShotEnemyTrooper) > self.getDistanceTo(trooper))
                    notShotEnemyTrooper = trooper;
            }
        }

        if (shotEnemyTrooper != null) {
            if (actionStance(move, true))
                return true;

            if (self.getActionPoints() >= self.getShootCost()) {
                move.setAction(ActionType.SHOOT);
                move.setX(shotEnemyTrooper.getX());
                move.setY(shotEnemyTrooper.getY());
            }
            return true;
        } else if (notShotEnemyTrooper != null) {
            List<Position> path = Helper.findPath(notShotEnemyTrooper);
            if (path != null && path.size() < 10 && actionPath(move, new Position(notShotEnemyTrooper)))
                return true;
        }

        return false;
    }

    /**
     * Устанавливает положение бойца. Опускается на последних действиях(стоя -> сидя -> лёжа),
     * если есть противник в поле досигаемости. Поднимается, если противника нет.
     *
     * @return true, если совершено действие.
     */
    private boolean actionStance(Move move, boolean canShoot) {
        if (self.getActionPoints() >= self.getInitialActionPoints() && !self.getStance().equals(TrooperStance.STANDING) && !canShoot) {
            move.setAction(ActionType.RAISE_STANCE);
            move.setX(self.getX());
            move.setY(self.getY());
            return true;
        }

        if (canShoot && self.getActionPoints() == game.getStanceChangeCost()) {
            move.setAction(ActionType.LOWER_STANCE);
            move.setX(self.getX());
            move.setY(self.getY());
            return true;
        }
        return false;
    }

    /**
     * Боец подбирает бонусы, которых у него нет.
     *
     * @return true, если совершено действие.
     */
    private boolean actionTakeBonus(Move move) {
        for (Bonus bonus : world.getBonuses()) {
            if (self.getDistanceTo(bonus) < 2) {
                if ((bonus.getType().equals(BonusType.MEDIKIT) && !self.isHoldingMedikit()) ||
                        (bonus.getType().equals(BonusType.FIELD_RATION) && !self.isHoldingFieldRation()) ||
                        (bonus.getType().equals(BonusType.GRENADE) && !self.isHoldingGrenade()))
                    if (self.getDistanceTo(bonus) > 1) {
                        if (actionPath(move, new Position(bonus)))
                            return true;
                    } else {
                        if (actionMove(move, new Position(bonus)))
                            return true;
                    }
            }
        }
        return false;
    }

    /**
     *
     * @return true, если совершено действие.
     */
    private boolean actionMovePoint(Move move) {
        if (Helper.findTrooperPosition(TrooperType.FIELD_MEDIC) != null) {
            for (Trooper trooper : teamTroopers)
                if (trooper.getHitpoints() < trooper.getMaximalHitpoints() && self.getDistanceTo(trooper) > 1)
                    if (self.equals(trooper) || actionPath(move, new Position(trooper)))
                        return true;
        }


        if (actionStance(move, false))
            return true;

        if (Math.abs(self.getX()-Helper.toPosition.getX()) < 2 && Math.abs(self.getY()-Helper.toPosition.getY()) < 2)
            Helper.nextPosition();

        if (self.getType().equals(TrooperType.SCOUT))
            if (actionPath(move, Helper.findTrooperPosition(TrooperType.SOLDIER)))
                return true;

        if (self.getType().equals(TrooperType.FIELD_MEDIC))
            if (actionPath(move, Helper.findTrooperPosition(TrooperType.SOLDIER)))
                return true;

        if (self.getType().equals(TrooperType.COMMANDER))
            if (actionPath(move, Helper.findTrooperPosition(TrooperType.FIELD_MEDIC)))
                return true;

        if (self.getType().equals(TrooperType.SNIPER))
            if (actionPath(move, Helper.findTrooperPosition(TrooperType.FIELD_MEDIC)))
                return true;

        if (self.getActionPoints() >= self.getInitialActionPoints()/3)
            if (actionPath(move, Helper.toPosition))
                return true;
        return false;
    }

    /**
     *
     * @return true, если совершено действие.
     */
    private boolean actionPath(Move move, Position target) {
        if (target == null)
            return false;
        if (target.getDistanceTo(self) <=  1)
            return true;
        Position movePosition = Helper.initPath(new Position(self), target);
        return movePosition != null && actionMove(move, movePosition);
    }

    /**
     *
     * @return
     */
    private boolean actionMove(Move move, Position target) {
        if (self.getActionPoints() >= Helper.getMoveCost(self.getStance())) {
            move.setAction(ActionType.MOVE);
            move.setX(target.getX());
            move.setY(target.getY());
            return true;
        }
        return false;
    }
}