import model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.StrictMath.abs;

public class ActionService {

    /**
     * @return можно ли ударить по противнику.
     */
    public static boolean canHit(Hockeyist hockeyist) {
        return !hockeyist.isTeammate() &&
                hockeyist.getType() != HockeyistType.GOALIE &&
                hockeyist.getState() != HockeyistState.KNOCKED_DOWN &&
                hockeyist.getState() != HockeyistState.RESTING;
    }

    /**
     * @return ближайшего хоккеиста соперника (который может играть).
     */
    public static Hockeyist getNearestOpponent(Hockeyist self, List<Hockeyist> hockeyists) {
        Hockeyist nearestOpponent = null;
        double nearestOpponentRange = 0.0D;

        for (Hockeyist hockeyist : hockeyists) {
            if (ActionService.canHit(hockeyist)) {
                double opponentRange = self.getDistanceTo(hockeyist);

                if (nearestOpponent == null || opponentRange < nearestOpponentRange) {
                    nearestOpponent = hockeyist;
                    nearestOpponentRange = opponentRange;
                }
            }
        }

        return nearestOpponent;
    }

    /**
     * Определяет какая штанга от вратаря дальше.
     *
     * @param myPlayer мой игрок
     * @return координату ординаты дальней штанги.
     */
    public static double getYBetweenRod(Player myPlayer, Puck puck, World world) {
        double y = puck.getY();
        if (puck.getY() < myPlayer.getNetTop())
            y = myPlayer.getNetTop();
        if (puck.getY() > myPlayer.getNetBottom())
            y = myPlayer.getNetBottom();
        double middle = world.getHeight()/2;

        return middle + (y - middle)/2;
    }

    public static double getLineAtGate(Player player, Integer addRange) {
        return Math.abs(player.getNetBack() - (155 + (addRange != null ? addRange : 0)));
    }

    /**
     * Возвращает ближайшего хоккеиста противника.
     *
     * @param self              текуший хоккеист
     * @param opponentTeam хоккеисты противника
     * @return ближайшего хоккеиста противника.
     */
    public static Hockeyist getNerestOpponent(Unit self, List<Hockeyist> opponentTeam) {
        return getNerestOpponent(self.getX(), self.getY(), opponentTeam);
    }

    public static Hockeyist getNerestOpponent(double x, double y, List<Hockeyist> opponentTeam) {
        Hockeyist nearestHockeyst = null;
        for (Hockeyist opponent : opponentTeam) {
            double distance = opponent.getDistanceTo(x, y);
            if (nearestHockeyst == null || distance < nearestHockeyst.getDistanceTo(x, y))
                nearestHockeyst = opponent;
        }
        return nearestHockeyst;
    }

    /**
     * Возвращает атакующего хоккеиста.
     *
     * @param defenders защитники
     * @param team      команда
     * @return Хоккеиста не являющимся защитником.
     */
    public static Hockeyist getAttackerHockeyist(List<Hockeyist> defenders, List<Hockeyist> team) {
        for (Hockeyist hockeyist : team) {
            boolean isDefender = false;
            for (Hockeyist defender : defenders) {
                if (hockeyist.getId() == defender.getId())
                    isDefender = true;
            }
            if (!isDefender)
                return hockeyist;
        }
        return null;
    }

    /**
     * Считает сколько защитников около вратаря.
     *
     * @param goalie вратарь
     * @param team   команда
     * @return количество защитников.
     */
    public static int getDefenderCount(Hockeyist goalie, List<Hockeyist> team, double distance) {
        int count = 0;
        for (Hockeyist hockeyist : team) {
            if (hockeyist.getDistanceTo(goalie) < distance)
                count++;
        }
        return count;
    }

    public static int getDefenderCount(Hockeyist goalie, List<Hockeyist> team) {
        return getDefenderCount(goalie, team, 200);
    }

    /**
     * Возвращает угол игрока до дальней от вратаря штанги ворот игрока.
     *
     * @param self   хоккеист
     * @param player игрок чьи ворота
     * @return угол до дальней штанги.
     */
    public static double getAngleToNetTop(Unit self, Player player) {
        return self.getAngleTo(getPositionNetTop(self, player));
    }

    public static Position getPositionNetTop(Unit self, Player player) {
        double netX = 0.5D * (player.getNetBack() + player.getNetFront());
        double netY = 0.5D * (player.getNetBottom() + player.getNetTop());

        double distY = 0;
        double speedY = self.getSpeedY() * (-1);
        for (int i = 0; i < MyStrategy.SWING_TIME_TICKS; i++) {
            speedY = speedY - speedY/50;
            distY += speedY;
        }

        double distX = 0;
        double speedX = self.getSpeedX() * (-1);
        for (int i = 0; i < MyStrategy.SWING_TIME_TICKS; i++) {
            speedX = speedX - speedX/50;
            distX += speedX;
        }

        if (self.getY() < netY)
            netY = player.getNetBottom();
        else
            netY = player.getNetTop();

        netY += distY;
        netX += distX;

        return new Position(netX, netY);
    }


    /**
     * Вычисляет угол хоккеиста относительно ворот игрока.
     *
     * @param hockeyist  хоккеист
     * @param player     игрок
     * @return {true}, если хоккеист находится на таком угле, с которого можно забить гол.
     */
    public static boolean isAngleToAttackHockeiyst(Hockeyist hockeyist, Player player, double goalNetHeight) {
        double angleToNet = ActionService.getAngleToNetTop(hockeyist, player);
        double angleToLine = hockeyist.getAngleTo(new Position(player.getNetFront(), hockeyist.getY()));
        return abs(abs(angleToNet) - abs(angleToLine)) / MyStrategy.STRIKE_ANGLE > 20;
    }


    public static double getAngleTo(Unit a, Unit b) {
        return Math.abs(a.getAngleTo(b)) / MyStrategy.STRIKE_ANGLE;
    }

    /**
     * Возвращает хоккеиста из списка. Сверяет по id.
     *
     * @param hockeyist хоккеист
     * @param list      список хоккеистов.
     * @return хоккеиста из списка.
     */
    public static boolean isHockeyistInList(Hockeyist hockeyist, List<Hockeyist> list) {
        return getHockeyistInList(hockeyist.getId(), list) != null;
    }

    public static Hockeyist getHockeyistInList(Long id, List<Hockeyist> list) {
        for (Hockeyist hockey : list) {
            if (hockey.getId() == id)
                return hockey;
        }
        return null;
    }

    /**
     * Возвращает хоккеиста из списка, который не является переданным. Сверяет по id.
     *
     * @param hockeyist хоккеист
     * @param list      список хоккеистов.
     * @return хоккеиста из списка.
     */
    public static Hockeyist getHockeyistInListNotTransmitted(Hockeyist hockeyist, List<Hockeyist> list) {
        for (Hockeyist hockey : list) {
            if (hockey.getId() != hockeyist.getId())
                return hockey;
        }
        return null;
    }

    public static double getXPositionToPass(Hockeyist attacker, Hockeyist receiving, double line, double opponentGoalLine) {
        double x1 = Math.min(attacker.getX(), receiving.getX());
        double Xcp = (attacker.getX() + receiving.getX())/2 - x1;
        double offset = getOffsetToPass(attacker, receiving, line);

        if (Math.abs(line - attacker.getY()) > Math.abs(line - receiving.getY()))
            offset = 0 - offset;
        if (Math.abs(opponentGoalLine - attacker.getX()) < Math.abs(opponentGoalLine - receiving.getX()))
            offset = 0 - offset;

        return receiving.getX() + Xcp + offset;
    }

    public static double getOffsetToPass(Hockeyist attacker, Hockeyist receiving, double line) {
        double y1 = Math.min(Math.abs(line - attacker.getY()), Math.abs(line - receiving.getY()));
        double y2 = Math.max(Math.abs(line - attacker.getY()), Math.abs(line - receiving.getY()));
        double x1 = Math.min(attacker.getX(), receiving.getX());
        double Xcp = (attacker.getX() + receiving.getX())/2 - x1;
        double n = (2*Xcp)/(2*y1 + 2*Xcp - 100 - y2);

        return n*100;
    }

    public static boolean equalDoubles(double n1, double n2, double precision_) {
        return (Math.abs(n1-n2) <= precision_);
    }

    /**
     * Проверяет находится ли противник на пути между двумя объектами.
     *
     * @param a первый объект
     * @param b второй объект
     * @param opponentTeam команда противника.
     * @param r рудиус действия противника.
     * @return {true}, если противник из списка находится на пути.
     */
    public static boolean isEnemyOnTheWay(Unit a, Unit b, List<Hockeyist> opponentTeam, double r) {
        for (Unit opponent : opponentTeam) {
            if (lineCircleIntersection(opponent, r, a, b).size() > 0)
                return true;
        }
        return false;
    }

    /**
     * Рассчитывает точки пересечение отрезка проходящего через окружность.
     *
     * @param circle центр окружности
     * @param r  радиус окружности
     * @param a первая точка отрезка
     * @param b вторая точка отрезка
     * @return точки пересечения
     */
    public static List<Position> lineCircleIntersection(Unit circle, double r, Unit a, Unit b) {
        List<Position> positions = new ArrayList<>();
        double q = Math.pow(circle.getX(), 2) + Math.pow(circle.getY(), 2) - r*r;
        double k = -2.0 * circle.getX();
        double l = -2.0 * circle.getY();

        double z = a.getX()*b.getY() - b.getX()*a.getY();
        double p = a.getY() - b.getY();
        double s = a.getX() - b.getX();

        if (equalDoubles(s, 0.0, 0.001)) {
            s = 0.001;
        }

        double A = s*s + p*p;
        double B = s*s*k + 2.0*z*p + s*l*p;
        double C = q*s*s + z*z + s*l*z;

        double D = B*B - 4.0*A*C;

        if (D > 0.0) {
            if (D < 0.001) {
                double x = -B / (2.0 * A);
                positions.add(new Position(x, (p * x + z) / s));
            } else {
                double x = (-B + Math.sqrt(D)) / (2.0 * A);
                double y = (p * x + z) / s;
                positions.add(new Position(x, y));

                x = (-B - Math.sqrt(D)) / (2.0 * A);
                y = (p * x + z) / s;
                positions.add(new Position(x, y));
            }
        }

        return positions;
    }


    /**
     * Проверяет на единственное пересечение отрезка с окружностью
     *
     * @param circle центр окружности
     * @param r  радиус окружности
     * @param a первая точка отрезка
     * @param b вторая точка отрезка
     * @return единственную точку пересечения
     */
    public static Position segmentCircleIntersection(Unit circle, double r, Unit a, Unit b) {
        double d1 = Math.hypot(a.getX() - circle.getX(), a.getY() - circle.getY());
        double d2 = Math.hypot(b.getX() - circle.getX(), b.getY() - circle.getY());
        if (d1 > r && d2 > r) {
            return null;
        } if (d1 < r && d2 < r) {
            return null;
        }

        List<Position> positions = lineCircleIntersection(circle, r, a, b);
        if (positions.size() < 1)
            return null;

        double xmin = Math.min(a.getX(), b.getX());
        double xmax = Math.max(a.getX(), b.getX());
        double ymin = Math.min(a.getY(), b.getY());
        double ymax = Math.max(a.getY(), b.getY());

        for (Position position : positions) {
            if (position.getX() >= xmin && position.getX() <= xmax && position.getY() >= ymin && position.getY() <= ymax)
                return position;
        }

        return null;

    }

    /**
     * Записывает в текущий каталог файл со счётом.
     *
     * @param myPlayer       мой игрок.
     * @param opponentPlayer игрок соперника.
     * @param fileName       имя вайла.
     */
    public static void writeOfFile(Player myPlayer, Player opponentPlayer, int fileName) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("./" + fileName + "|43.txt", false);
            fileWriter.append("m:" + myPlayer.getGoalCount() + "\np:" + opponentPlayer.getGoalCount());
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {fileWriter.close();} catch (IOException e) {e.printStackTrace();}
            }
        }
    }
}