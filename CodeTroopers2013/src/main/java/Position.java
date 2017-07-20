import model.Trooper;
import model.Unit;

import java.util.List;

public class Position {
    private int x;
    private int y;
    public boolean value;
    public List<Position> nodes;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position(int x, int y, boolean value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public Position(Unit unit) {
        this.x = unit.getX();
        this.y = unit.getY();
    }


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    boolean isValue() {
        return value;
    }

    List<Position> getNodes() {
        return nodes;
    }

    void setNodes(List<Position> nodes) {
        this.nodes = nodes;
    }

    public int getDistanceTo(Trooper trooper) {
        return Math.abs(this.x - trooper.getX()) + Math.abs(this.y - trooper.getY());
    }

    @Override
    public boolean equals(Object pos) {
        Position position = (Position) pos;
        return this.getX() == position.getX() && this.getY() == position.getY();
    }
}