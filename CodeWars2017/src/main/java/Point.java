import model.Unit;

/**
 * @author George Beliy on 23-11-17
 */
class Point extends Unit {
    int x;
    int y;

    Point(double x, double y) {
        super(0, x, y);
        this.x = (int) x;
        this.y = (int) y;
    }

    Point(Unit unit) {
        super(unit.getId(), unit.getX(), unit.getY());
        this.x = (int) unit.getX();
        this.y = (int) unit.getY();
    }

    Double distanceTo(Unit unit) {
        return Math.hypot(Math.abs(getX() - unit.getX()), Math.abs(getY() - unit.getY()));
    }

    Double getPPoint(Double[][] pp) {
        return pp[(int) (getX()/StrategyHelper.CELL_WIDTH)][(int) (getY()/StrategyHelper.CELL_WIDTH)];
    }
}
