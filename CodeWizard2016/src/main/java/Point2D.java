import model.*;

import java.util.Random;


/**
 * Вспомогательный класс для хранения позиций на карте.
 */
final class Point2D extends LivingUnit {
    private final double x;
    private final double y;

    Point2D(double x, double y) {
        super(new Random().nextInt(10000), x, y, 0, 0, -(Math.PI * 90) / 180.0, Faction.OTHER, 35, 0, 0, new Status[0]);
        this.x = x;
        this.y = y;
    }

    Point2D(Unit unit) {
        this(unit.getX(), unit.getY());
    }

    Point2D(Projectile projectile, Game game, boolean addRadius) {
        super(projectile.getId(), projectile.getX(), projectile.getY(), projectile.getSpeedX(), projectile.getSpeedY(),
                projectile.getAngle(), Faction.OTHER,
                (ProjectileType.MAGIC_MISSILE.equals(projectile.getType()) ? game.getMagicMissileRadius() :
                    ProjectileType.FIREBALL.equals(projectile.getType()) ? game.getFireballExplosionMinDamageRange()/2 :
                    ProjectileType.FROST_BOLT.equals(projectile.getType()) ? game.getFrostBoltRadius() : game.getDartRadius()) * (addRadius ? 1.5 : 1),
                0, 0, new Status[0]);
        x = getX();
        y = getY();
    }


    double getDistanceTo(Point2D point) {
        return getDistanceTo(point.x, point.y);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Point2D && ((Point2D) o).getX() == x && ((Point2D) o).getY() == y;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
