import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {

    private static final boolean USE_WRITE_FILE = true;

    private static final int FILE_NAME = new Random().nextInt(200000);

    /** Значение угла равное 1 градусу */
    public static final double STRIKE_ANGLE = PI / 180.0D;

    /** Расстояния от края ворот противника */
    private static final int X_ATTACK_POSITION_RANGE = 350;

    /** Дистанция от штанги */
    private static final int Y_ATTACK_POSITION_RANGE = 0;

    /** Дистанция от позиции атаки через которую бежит хоккеист */
    private static final int X_PRE_ATTACK_POSITION_RANGE = 450;
    private static final int Y_PRE_ATTACK_POSITION_RANGE = 50;

    /** Дистанция охраны защитника */
    private static final int SECURITY_DISTANCE = 250;

    /** Время замаха (в тиках) */
    public static final int SWING_TIME_TICKS = 20;


    /** Верхняя позиция атаки */
    private static Position topAttackPosition;

    /** Верхняя позиция до позиции атаки */
    private static Position topPreAttackPosition;

    /** Позиция второго атакующего хоккеиста, если он есть */
    private static Position secondAttackerPosition;

    /** Нижняя позиция атаки */
    private static Position lowerAttackPosition;

    /** Нижня позиция до позиции атаки */
    private static Position lowerPreAttackPosition;

    /** Линия атаки моих ворот по x (на вскидку) */
    private static Position defPosition;

    /** Последняя позиция атаки (не обезательно, чтобы из этой позиции должен быть удар) */
    private static Position lastAttackPosition;

    /** Позиция аттаки к которой бежит хоккеист с шайбой */
    private static Position attackPosition;

    /** Последний хоккеист владевший шайбой */
    private static Hockeyist lastUsePuckHockeyist;

    /** Список моих защитников */
    private static List<Hockeyist> defenders;

    /** Текущий хоккеист */
    private Hockeyist self;

    /** Игровой мир */
    private World world;

    /** Объект игры */
    private Game game;

    /** Объект действия */
    private Move move;

    /** Мой игрок */
    private Player myPlayer;

    /** Противник */
    private Player opponentPlayer;

    /** Хоккеисты моей команды на поле, кроме защитника */
    private List<Hockeyist> team = new ArrayList<>();

    /** Хоккеисты моей команды на скамейке запасных */
    private List<Hockeyist> restings = new ArrayList<>();

    /** Хоккеисты противника */
    private List<Hockeyist> opponentTeam = new ArrayList<>();

    /** Вратарь мой */
    private Hockeyist goalie;

    /** Второй атакующий */
    private static Hockeyist secondAttacker;

    /** Вратарь противника */
    private Hockeyist opponentGoalie;

    /** Шайба */
    private Puck puck;


    @Override
    public void move(Hockeyist self, World world, Game game, Move move) {

        init(self, world, game, move);

        if (justScoredGoal())
            return;

        if (justMissedGoal())
            return;

        if (self.getState() == HockeyistState.SWINGING) {
            if (puck.getOwnerHockeyistId() != self.getId()) {
                move.setAction(ActionType.CANCEL_STRIKE);
                return;
            }
        }

        if (ActionService.isHockeyistInList(self, defenders)) {
            if (puck.getOwnerHockeyistId() == self.getId()) {
                Hockeyist attacker = ActionService.getAttackerHockeyist(defenders, team);
                if (attacker != null)
                    defenders.add(attacker);
                Hockeyist selfInList = ActionService.getHockeyistInList(self.getId(), defenders);
                defenders.remove(selfInList);
                if (moveToAttackPosition())
                    return;
            }

            if (attackOpponentDefender())
                return;

            if (strikeDefenderPuck())
                return;

            if (opponentAttackDefender())
                return;

            if (moveToSecurity()) {
                if (staminaHockeyist())
                    return;
                return;
            }

            if (nearestOpponent(true))
                return;

            moveToPuck();
            return;
        }

        if (self.getState() == HockeyistState.SWINGING) {
            if (self.getSwingTicks() < SWING_TIME_TICKS) {
                move.setAction(ActionType.SWING);
                return;
            }
//            if (ActionService.getAngleToNetTop(self, opponentPlayer, game.getGoalNetHeight()) > STRIKE_ANGLE * 3)
//                move.setAction(ActionType.CANCEL_STRIKE);
//            else
                strike();
            return;
        }

        if (havePuck())
            return;

        if (puck.getOwnerPlayerId() == myPlayer.getId()) {
            if (staminaHockeyist())
                return;

            Hockeyist hockeyist = ActionService.getNearestOpponent(lastUsePuckHockeyist, opponentTeam);
            if (hockeyist != null) {
                if (self.getDistanceTo(hockeyist) > game.getStickLength())
                    moveToPosition(hockeyist.getX(), hockeyist.getY(), 0, 10);
                else
                    nearestOpponent(hockeyist, true);
                return;
            }
        }

        if (secondAttacker != null && self.getId() == secondAttacker.getId()) {
            if (strikeDefenderPuck())
                return;

            if (self.getDistanceTo(puck) < 250 && puck.getOwnerPlayerId() != myPlayer.getId() && self.getDistanceTo(secondAttackerPosition) < 250) {
                moveToPuck();
                return;
            }

            if (staminaHockeyist())
                return;

            if (self.getDistanceTo(secondAttackerPosition) < 100) {
                standAndView(puck);
            } else {
                moveToPosition(secondAttackerPosition, 150, 0);
            }
            return;
        }

        if (staminaHockeyist())
            return;

        if (nearestOpponent(false))
            return;

        moveToPuck();
    }

    /**
     * Инициализация параметров.
     */
    private void init(Hockeyist self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        this.myPlayer = world.getMyPlayer();
        this.opponentPlayer = world.getOpponentPlayer();
        this.puck = world.getPuck();
        this.team = new ArrayList<>();
        this.restings = new ArrayList<>();
        this.opponentTeam = new ArrayList<>();
        if (defenders == null)
            defenders = new ArrayList<>();

        Long lastUsePuckId = puck.getOwnerHockeyistId();
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate()) {
                if (hockeyist.getType() == HockeyistType.GOALIE)
                    goalie = hockeyist;
                else if (HockeyistState.RESTING.equals(hockeyist.getState()))
                    restings.add(hockeyist);
                else if (!ActionService.isHockeyistInList(hockeyist, defenders))
                    team.add(hockeyist);

                if (defenders.isEmpty() && hockeyist.getType().equals(HockeyistType.DEFENCEMAN))
                    defenders.add(hockeyist);
            } else {
                if (hockeyist.getType() != HockeyistType.GOALIE)
                    opponentTeam.add(hockeyist);
                else
                    opponentGoalie = hockeyist;
            }
            if (lastUsePuckId != -1 && hockeyist.getId() == lastUsePuckId)
                lastUsePuckHockeyist = hockeyist;
        }

        if (world.getHockeyists().length > 6) {
            if (world.getPuck().getOwnerPlayerId() == myPlayer.getId()) {
                for (Hockeyist hockeyist : team)
                    if (puck.getOwnerHockeyistId() != hockeyist.getId())
                        secondAttacker = hockeyist;

            } else if (secondAttacker == null || puck.getOwnerPlayerId() == opponentPlayer.getId()) {
                for (Hockeyist hockeyist : team)
                    if (hockeyist.getType().equals(HockeyistType.VERSATILE))
                        secondAttacker = hockeyist;

                if (secondAttacker == null)
                    secondAttacker = ActionService.getNerestOpponent(goalie, team);
            } else {
                for (Hockeyist hockeyist : team)
                    if (hockeyist.getId() == secondAttacker.getId())
                        secondAttacker = hockeyist;
            }
            team.remove(secondAttacker);
        }
        if (defenders.isEmpty()) {
            Hockeyist defender = null;
            for (Hockeyist hockeyist : team) {
                if (defender == null || defender.getDistanceTo(goalie) > hockeyist.getDistanceTo(goalie))
                    defender = hockeyist;
            }
            defenders.add(defender);
            team.remove(defender);
        }

        defPosition = new Position(Math.abs(myPlayer.getNetBack() - 300), world.getHeight() / 2);

        double xAttackPosition = Math.abs(opponentPlayer.getNetBack() - X_ATTACK_POSITION_RANGE);
        topAttackPosition = new Position(xAttackPosition, opponentPlayer.getNetTop() - Y_ATTACK_POSITION_RANGE);
        lowerAttackPosition = new Position(xAttackPosition, opponentPlayer.getNetBottom() + Y_ATTACK_POSITION_RANGE);

        double xPreAttackPosition = xAttackPosition - X_PRE_ATTACK_POSITION_RANGE * (xAttackPosition > defPosition.getX() ? 1 : -1);
        topPreAttackPosition = new Position(xPreAttackPosition, opponentPlayer.getNetTop() - Y_ATTACK_POSITION_RANGE);
        lowerPreAttackPosition = new Position(xPreAttackPosition, opponentPlayer.getNetBottom() + Y_ATTACK_POSITION_RANGE);

        double xSecondPlAttackPosition = xAttackPosition - 450 * (xAttackPosition > defPosition.getX() ? 1 : -1);
        double y = puck.getY();
        if (puck.getY() < myPlayer.getNetTop())
            y = myPlayer.getNetTop();
        if (puck.getY() > myPlayer.getNetBottom())
            y = myPlayer.getNetBottom();
        secondAttackerPosition = new Position(xSecondPlAttackPosition, y);

        if (puck.getOwnerPlayerId() != myPlayer.getId() || puck.getOwnerPlayerId() == -1) {
            attackPosition = null;
            lastAttackPosition = null;
        }
    }

    /**
     * @return {true}, если будет произведена замена хоккеистов.
     */
    private boolean staminaHockeyist() {
        if (world.getHockeyists().length > 12) {
            if ((self.getStamina() < 100 && puck.getOwnerHockeyistId() != self.getId())
                        || myPlayer.isJustScoredGoal() || myPlayer.isJustMissedGoal()){
                Hockeyist maxRestingsHockeyist = null;
                for (Hockeyist hockeyist : restings) {
                    if (maxRestingsHockeyist == null || maxRestingsHockeyist.getStamina() < hockeyist.getStamina())
                        maxRestingsHockeyist = hockeyist;
                }
                if (maxRestingsHockeyist != null && maxRestingsHockeyist.getStamina() > 500
                        && maxRestingsHockeyist.getStamina() > self.getStamina()) {
                    if (abs(myPlayer.getNetBack() - self.getX()) < abs(opponentPlayer.getNetBack() - self.getX())
                            && self.getY() < game.getSubstitutionAreaHeight() + 120
                            && hypot(abs(self.getSpeedX()), abs(self.getSpeedY())) < game.getMaxSpeedToAllowSubstitute()) {
                        move.setAction(ActionType.SUBSTITUTE);
                        move.setTeammateIndex(maxRestingsHockeyist.getTeammateIndex());
                        return true;
                    } else {
                        double x = self.getX();
                        if (abs(myPlayer.getNetBack() - self.getX()) > abs(opponentPlayer.getNetBack() - self.getX()))
                            x = defPosition.getX();
                        moveToPosition(new Position(x, game.getSubstitutionAreaHeight()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return {true}, если защитник поедет бить зашитника противника.
     */
    private boolean attackOpponentDefender() {
        if (puck.getOwnerPlayerId() == myPlayer.getId()) {
            int defenderCount = ActionService.getDefenderCount(opponentGoalie, opponentTeam, 300);
            if (defenderCount > 0) {
                Hockeyist hockeyist = ActionService.getNerestOpponent(opponentGoalie, opponentTeam);
                if (hockeyist != null) {
                    if (self.getDistanceTo(hockeyist) > game.getStickLength())
                        moveToPosition(hockeyist.getX(), hockeyist.getY(), 0, 10);
                    else
                        nearestOpponent(hockeyist, true);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Если защитника атакуют, то он бежит отбивать шайбу.
     * @return {true}, если защитника атакуют.
     */
    private boolean opponentAttackDefender() {
        final Hockeyist attackerHockeyist = ActionService.getNerestOpponent(self, opponentTeam);
        if (ActionService.getDefenderCount(goalie, opponentTeam, 250) >= 1
                && lastUsePuckHockeyist != null && lastUsePuckHockeyist.getPlayerId() == opponentPlayer.getId()
                && attackerHockeyist.getId() != lastUsePuckHockeyist.getId()) {
            if (HockeyistState.KNOCKED_DOWN.equals(attackerHockeyist.getState())
                    || !ActionService.isEnemyOnTheWay(self, puck, new ArrayList<Hockeyist>(){{add(attackerHockeyist);}},
                                                    attackerHockeyist.getRadius() * 2)) {
                moveToPuck();
            } else {
                if (nearestOpponent(attackerHockeyist, true))
                    return true;
                else
                    moveToPosition(attackerHockeyist);
            }
            return true;
        }
        return false;
    }

    /**
     * Защитник бъёт по шайбе.
     * @return {true}, если будет проводиться удар.
     */
    private boolean strikeDefenderPuck() {
        Position netTop = ActionService.getPositionNetTop(puck, myPlayer);
        double puckAngleToNet = ActionService.getAngleToNetTop(puck, myPlayer);
        double puckAngleToLine = puck.getAngleTo(new Position(myPlayer.getNetFront(), puck.getY()));

        if (self.getDistanceTo(puck) < game.getStickLength()
                && lastUsePuckHockeyist != null
                && puck.getOwnerPlayerId() != myPlayer.getId()
                && ((puck.getOwnerPlayerId() == -1
                        && hypot(abs(puck.getSpeedX()), abs(puck.getSpeedY())) > 1.5
                        && abs(abs(puckAngleToNet) - abs(puckAngleToLine))/STRIKE_ANGLE > 15
                        && !ActionService.isEnemyOnTheWay(puck, netTop, new ArrayList<Hockeyist>(){{add(goalie);}}, goalie.getRadius() * 2)
                        && lastUsePuckHockeyist.getDistanceTo(new Position(myPlayer.getNetFront(), lastUsePuckHockeyist.getY())) < 600
                    ) || (puck.getOwnerPlayerId() == opponentPlayer.getId()
                        && self.getDistanceTo(lastUsePuckHockeyist) < 175)
                   )) {
            if (abs(self.getAngleTo(puck)) < game.getStickSector()/2)
                strike();
            move.setTurn(self.getAngleTo(puck));
            return true;
        }
        return false;
    }

    /**
     * Действия при забитии гола.
     */
    private boolean justScoredGoal() {
        if (myPlayer.isJustScoredGoal()) {
            if (USE_WRITE_FILE)
                ActionService.writeOfFile(myPlayer, opponentPlayer, FILE_NAME);

            if (staminaHockeyist())
                return true;

            defenders = null;
            if (self.getDistanceTo(goalie) > 10) {
                moveToPosition(goalie);
            } else {
                move.setSpeedUp(0D);
                move.setAction(ActionType.NONE);
            }
            return true;
        }
        return false;
    }

    /**
     * Действия при пропуске гола. Лупим противника.
     */
    private boolean justMissedGoal() {
        if (myPlayer.isJustMissedGoal()) {
            if (USE_WRITE_FILE)
                ActionService.writeOfFile(myPlayer, opponentPlayer, FILE_NAME);

            if (staminaHockeyist())
                return true;

            defenders = null;
            if (nearestOpponent(true))
                return true;
            Hockeyist enemyHockeyst = ActionService.getNearestOpponent(self, opponentTeam);
            if (enemyHockeyst != null)
                moveToPosition(enemyHockeyst);
            return true;
        }
        return false;
    }

    /**
     * Удар.
     */
    private void strike() {
        move.setAction(ActionType.STRIKE);
    }

    /**
     * Действия при наличии шайбы. Удар по воротам, подбег к месту удара, пасы и т.д
     */
    private boolean havePuck() {
        if (puck.getOwnerHockeyistId() == self.getId()) {
            Position netTop = ActionService.getPositionNetTop(self, opponentPlayer);
            if (!ActionService.isEnemyOnTheWay(self, netTop, new ArrayList<Hockeyist>(){{add(opponentGoalie);}},
                    opponentGoalie.getRadius() * 2)) {
                double angleToNet = ActionService.getAngleToNetTop(self, opponentPlayer);
                double angleToLine = self.getAngleTo(new Position(opponentPlayer.getNetFront(), self.getY()));
                if (abs(abs(angleToNet) - abs(angleToLine))/STRIKE_ANGLE > 15
                        && self.getDistanceTo(new Position(opponentPlayer.getNetFront(), self.getY())) < 600) {
                    move.setTurn(angleToNet);
                    if (abs(angleToNet) < STRIKE_ANGLE) {
                        move.setAction(ActionType.SWING);
                    }
                    return true;
                }
            }


            if (moveToAttackPosition()) {
//                if (passToPlayer(secondAttacker, false))
//                    return true;

//                if (passToPlayer(defenders.get(0), false))
//                    return true;

//                if (passToPlayer(secondAttacker, true))
//                    return true;

//                if (passToPlayer(defenders.get(0), true))
//                    return true;

                return true;
            }
        }
        return false;
    }

    /**
     * Отдаёт пас указанному хоккеисту.
     *
     * @param hockeyist хоккеист, которому будет отдан пас.
     * @param pasOfWall если значение {true}, то пас может быть отдан через стенку игрового мира.
     * @return {true}, если пас будет отдан.
     */
    private boolean passToPlayer(Hockeyist hockeyist, boolean pasOfWall) {
        if (hockeyist == null)
            return true;
        if (ActionService.getAngleTo(hockeyist, puck) < 30
                && hockeyist.getDistanceTo(ActionService.getNerestOpponent(hockeyist, opponentTeam)) > 400
                && self.getDistanceTo(ActionService.getNerestOpponent(self, opponentTeam)) < 200
                && self.getDistanceTo(new Position(opponentPlayer.getNetFront(), self.getY())) < 600
                && self.getDistanceTo(hockeyist) > 300
                && world.getHockeyists().length > 6) {
            if (!ActionService.isEnemyOnTheWay(self, hockeyist, opponentTeam, opponentTeam.get(0).getRadius() * 2)) {
                if (ActionService.getAngleTo(self, hockeyist) > game.getPassSector()) {
                    move.setTurn(self.getAngleTo(hockeyist));
                    move.setAction(ActionType.TAKE_PUCK);
                    return true;
                } else {
                    passTo(hockeyist);
                    return true;
                }
            } else if (pasOfWall) {
                if (passOfWall(self, secondAttacker))
                    return true;
            }
        }
        return false;
    }

    /**
     * Отдаёт пас через верхнюю или нижнюю стенку мира
     *
     * @param self      хоккеист отдающий пас
     * @param receiving хоккеист, которому отдают пас.
     * @return {true}, если будет отдан пас.
     */
    private boolean passOfWall(Hockeyist self, Hockeyist receiving) {
        Hockeyist nearestOpponent = ActionService.getNearestOpponent(self, opponentTeam);
        double line = self.getY() > nearestOpponent.getY() ? world.getHeight() : 0;
        double x = ActionService.getXPositionToPass(self, receiving, line, opponentPlayer.getNetBack());

        if (!ActionService.isEnemyOnTheWay(self, new Position(x, line), opponentTeam, 55) &&
                !ActionService.isEnemyOnTheWay(receiving, new Position(x, line), opponentTeam, 55)) {
            move.setTurn(self.getAngleTo(x, line));
            if (abs(self.getAngleTo(x, line)) < STRIKE_ANGLE) {
                strike();
            }
            return true;
        }
        return false;
    }

    /**
     * Лупит клюшкой по ближайшему стойщиму на ногах хоккеисту противника.
     */
    private boolean nearestOpponent(boolean dontOnlyHavePuck) {
        return nearestOpponent(ActionService.getNearestOpponent(self, opponentTeam), dontOnlyHavePuck);
    }

    /**
     * Лупит клюшкой по указанному хоккеисту.
     */
    private boolean nearestOpponent(Hockeyist nearestOpponent, boolean dontOnlyHavePuck) {
        if (nearestOpponent != null) {
            if (self.getDistanceTo(nearestOpponent) < game.getStickLength()) {
                if (puck.getOwnerHockeyistId() == nearestOpponent.getId() || dontOnlyHavePuck) {
                    if (abs(self.getAngleTo(nearestOpponent)) < 0.5D * game.getStickSector()) {
                        strike();
                    }
                    move.setTurn(self.getAngleTo(nearestOpponent));
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Движение к шайбе.
     */
    private void moveToPuck() {
        moveTo(1, puck.getX(), puck.getY(), false);
    }

    /**
     * Движение к позиции атаки.
     * @return {false}, если хоккеист уже на позиции.
     */
    private boolean moveToAttackPosition() {
        double x, y;
        Position newAttackPosition;
        double xAttackPosition = topAttackPosition.getX();
        double yTopAttackPosition = topAttackPosition.getY();
        double yLowerAttackPosition = lowerAttackPosition.getY();

        if (attackPosition == null) {
            if (abs(self.getX() - xAttackPosition) > abs(self.getX() - topPreAttackPosition.getX())) {
                if (self.getDistanceTo(new Position(topPreAttackPosition.getX(), self.getY())) > 300)
                    x = topPreAttackPosition.getX();
                else
                    x = defPosition.getX();
            } else
                x = xAttackPosition;

            if (x == topPreAttackPosition.getX()) {
                if (self.getDistanceTo(topPreAttackPosition) > self.getDistanceTo(lowerPreAttackPosition))
                    y = yTopAttackPosition - Y_PRE_ATTACK_POSITION_RANGE;
                else
                    y = yLowerAttackPosition + Y_PRE_ATTACK_POSITION_RANGE;
            } else if (x == defPosition.getX()) {
                y = defPosition.getY();
            } else {
                if (self.getDistanceTo(topAttackPosition) > self.getDistanceTo(lowerAttackPosition))
                    y = yLowerAttackPosition;
                else
                    y = yTopAttackPosition;
            }
            attackPosition = new Position(x, y);
        }
        newAttackPosition = attackPosition;

        if (self.getDistanceTo(newAttackPosition) < 100) {
            if (newAttackPosition.getX() == xAttackPosition) {
                attackPosition = null;
                lastAttackPosition = null;
                return false;
            } else if (newAttackPosition.getX() == topPreAttackPosition.getX()) {
                attackPosition = new Position(xAttackPosition, attackPosition.getY());
                lastAttackPosition = attackPosition;
                moveToPosition(attackPosition, 50);
                return true;
            } else {
                Hockeyist nearestHockeyist = ActionService.getNerestOpponent(topPreAttackPosition, opponentTeam);
                double distanceToTop = nearestHockeyist.getDistanceTo(topPreAttackPosition);
                nearestHockeyist = ActionService.getNerestOpponent(lowerPreAttackPosition, opponentTeam);
                double distanceToLower = nearestHockeyist.getDistanceTo(lowerPreAttackPosition);


                double attackPositionY;
                double attackPositionX = topPreAttackPosition.getX();
                if (lastAttackPosition != null) {
                   if (lastAttackPosition.getY() == yLowerAttackPosition)
                       attackPositionY = yTopAttackPosition;
                    else
                       attackPositionY = yLowerAttackPosition;
                } else if (distanceToLower > distanceToTop)
                    attackPositionY = yLowerAttackPosition + 0;
                else
                    attackPositionY = yTopAttackPosition - 0;

                attackPosition = new Position(attackPositionX, attackPositionY);
                lastAttackPosition = new Position(defPosition);
                moveToPosition(attackPosition, 50);
                return true;
            }
        }

        moveToPosition(newAttackPosition, 50);
        return true;
    }

    /**
     * Движется к месту защиты.
     */
    private boolean moveToSecurity() {
        int secureDistance = SECURITY_DISTANCE;
        if (defenders.size() >= 2 && self.getId() == defenders.get(0).getId())
            secureDistance = 150;
        if (world.getHockeyists().length == 6 && ActionService.getDefenderCount(opponentGoalie, opponentTeam) > 0)
            secureDistance += 300;

        double rodY = ActionService.getYBetweenRod(myPlayer, puck, world);
        Integer addRange = defenders.size() > 1 && self.getId() == defenders.get(1).getId() ? 30 : 0;
        double rodX = ActionService.getLineAtGate(myPlayer, addRange);

        if (self.getDistanceTo(puck) > secureDistance ||
                puck.getOwnerPlayerId() == myPlayer.getId() ||
                self.getDistanceTo(rodX, rodY) > world.getWidth()/3) {
            moveToPosition(rodX, rodY, 50, 75, 200/self.getDistanceTo(rodX, rodY));
            return true;
        }
        return false;
    }

    private void moveToPosition(Unit unit) {
        moveToPosition(unit.getX(), unit.getY(), 0);
    }

    private void moveToPosition(Unit unit, double distanceBack) {
        moveToPosition(unit.getX(), unit.getY(), distanceBack);
    }

    private void moveToPosition(Unit unit, double distanceBack, double distanceStop) {
        moveToPosition(unit.getX(), unit.getY(), distanceBack, distanceStop);
    }

    private void moveToPosition(double x, double y, double distanceBack) {
        moveToPosition(x, y, distanceBack, 100);
    }

    private void moveToPosition(double x, double y, double distanceBack, double distanceStop) {
        moveToPosition(x, y, distanceBack, distanceStop, 1);
    }

    /**
     * Движение к заданной позиции.
     */
    private void moveToPosition(double x, double y, double distanceBack, double distanceStop, double divideSpeedUp) {
        double distance = self.getDistanceTo(x, y);
        double speed = hypot(self.getSpeedX(), self.getSpeedY());

        boolean back = true;
        if (distance > distanceBack || abs(self.getAngleTo(x, y)) < 90 * STRIKE_ANGLE) {
            back = false;
        }

        if (distance < distanceStop) {
            if (speed < 1)
                standAndView(puck);
            else
                moveTo((back ? 1 : -1)/divideSpeedUp, x, y, back);
        } else {
            moveTo((back ? -1 : 1)/divideSpeedUp, x, y, back);
        }
    }

    /**
     * Передвижение в заданной координате.
     *
     * @param speed скорость
     */
    private void moveTo(double speed, double x, double y, boolean back) {
        move.setSpeedUp(speed);
        move.setTurn(back ? 0 - self.getAngleTo(x, y) : self.getAngleTo(x, y));
        move.setAction(ActionType.TAKE_PUCK);
    }

    /**
     * Стоять и смотреть в сторону объекта.
     */
    private void standAndView(Unit unit) {
        standAndView(unit.getX(), unit.getY());
    }

    /**
     * Стоять и смотреть в сторону переданной координаты.
     */
    private void standAndView(double x, double y) {
        move.setSpeedUp(0);
        move.setTurn(self.getAngleTo(x, y));
        move.setAction(ActionType.TAKE_PUCK);
    }

    /**
     * Отправляет пас указанному объекту.
     */
    private void passTo(Unit unit) {
        passTo(unit.getX(), unit.getY());
    }

    /**
     * Отправляет пас в указанные координаты
     */
    private void passTo(double x, double y) {
        move.setAction(ActionType.PASS);
        move.setPassPower(self.getDistanceTo(x, y)/500);
        move.setPassAngle(self.getAngleTo(x, y));
    }

}