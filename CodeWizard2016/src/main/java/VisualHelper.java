import model.*;

import java.awt.*;
import java.util.*;

public class VisualHelper {


    private VisualClient visualClient;


    private AttackHelper attackHelper;
    private MovesHelper movesHelper;
    private SaveHelper saveHelper;
    private BonusHelper bonusHelper;
    private SkillHelper skillHelper;


    /** Создаём singleton объект данного класса */
    private static VisualHelper helper;
    static synchronized VisualHelper getInstance() {
        if (helper == null) {
            helper = new VisualHelper();
            helper.visualClient = new VisualClient();
            helper.attackHelper = AttackHelper.getInstance();
            helper.movesHelper = MovesHelper.getInstance();
            helper.saveHelper = SaveHelper.getInstance();
            helper.bonusHelper = BonusHelper.getInstance();
            helper.skillHelper = SkillHelper.getInstance();
        }
        return helper;
    }


    void print(World world, Wizard self, Game game) {
        visualClient.beginPost();


        for (Building building : DataHelper.buildings.values()) {
            printCircle(building, building.getAttackRange(), Color.BLACK);
        }

        for (Point2D point2D : DataHelper.getWaypointsTop()) {
            printCircle(point2D, DataHelper.WAYPOINT_RADIUS, Color.PINK);
        }
        for (Point2D point2D : DataHelper.getWaypointsBottom()) {
            printCircle(point2D, DataHelper.WAYPOINT_RADIUS, Color.YELLOW);
        }
        for (Point2D point2D : DataHelper.getWaypointsMiddle()) {
            printCircle(point2D, DataHelper.WAYPOINT_RADIUS, Color.red);
        }


        printCircle(movesHelper.nextWaypoint, DataHelper.WAYPOINT_RADIUS, Color.CYAN);
        printCircle(movesHelper.prevWaypoint, DataHelper.WAYPOINT_RADIUS, Color.PINK);

        if (movesHelper.currentMovePoint != null) {
            printWay(self, movesHelper.currentMovePoint);
        }

        if (attackHelper.attackPoint != null && attackHelper.nearestTarget != null) {
            printCircle(attackHelper.attackPoint, attackHelper.nearestTarget.getRadius(), Color.RED);
        }

        printCircle(self, self.getVisionRange(), Color.GREEN);

        for (Projectile projectile : world.getProjectiles()) {
            Unit ownerUnit = DataHelper.getProjectileOwnerPoint(projectile.getId());
            Double distance;
            if (ownerUnit != null && ownerUnit instanceof Wizard) {
                distance = ((Wizard) ownerUnit).getCastRange() - projectile.getDistanceTo(ownerUnit);
            } else if (ownerUnit != null && ownerUnit instanceof Minion) {
                distance = game.getFetishBlowdartAttackRange() - projectile.getDistanceTo(ownerUnit);
            } else {
                distance = 600 - projectile.getDistanceTo(ownerUnit);
            }
            VisualHelper.getInstance().printWay( new Point2D(projectile, game, false), StrategyHelper.getAnglePoint(projectile, distance, projectile.getAngle() + Math.PI));
        }


        visualClient.text(self.getX(), self.getY(), "" + self.getRemainingCooldownTicksByAction()[2], Color.BLACK);
        visualClient.text(self.getX() - self.getRadius(), self.getY() + self.getRadius(), skillHelper.wizardType.toString(), Color.BLACK);

        visualClient.endPost();

    }

    private boolean printCircle(LivingUnit unit, Double radius, Color color) {
        visualClient.circle(unit.getX(), unit.getY(), radius, color);
        return true;
    }


    private boolean printWay(LivingUnit a, LivingUnit b) {
        Point2D pointLeft = StrategyHelper.getAnglePoint(a, a.getRadius(), a.getAngle() + a.getAngleTo(b) + Math.PI/2);
        Point2D pointRight = StrategyHelper.getAnglePoint(a, a.getRadius(), a.getAngle() + a.getAngleTo(b) - Math.PI/2);
        Point2D pointToLeft = StrategyHelper.getAnglePoint(b, a.getRadius(), a.getAngle() + a.getAngleTo(b) + Math.PI/2);
        Point2D pointToRight = StrategyHelper.getAnglePoint(b, a.getRadius(), a.getAngle() + a.getAngleTo(b) - Math.PI/2);

        visualClient.line(pointLeft.getX(), pointLeft.getY(), pointToLeft.getX(), pointToLeft.getY(), Color.BLACK);
        visualClient.line(pointRight.getX(), pointRight.getY(), pointToRight.getX(), pointToRight.getY(), Color.BLACK);
        visualClient.circle(b.getX(), b.getY(), a.getRadius(), Color.BLACK);

        return true;
    }



}
