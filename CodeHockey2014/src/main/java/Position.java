import model.Unit;

public class Position extends Unit {
    public Position() {
        super(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public Position(Position position) {
        super(0, 0, 0, position.getX(), position.getY(), 0, 0, 0, 0);
    }

    public Position(double x, double y) {
        super(0, 0, 0, x, y, 0, 0, 0, 0);
    }

}
