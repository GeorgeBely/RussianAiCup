import java.util.List;

public class Node {
    private Position position;
    private List<Position> path;

    public Node(Position position, List<Position> path) {
        this.position = position;
        this.path = path;
    }

    Position getPosition() {
        return position;
    }

    void setPosition(Position position) {
        this.position = position;
    }

    List<Position> getPath() {
        return path;
    }

    void setPath(List<Position> path) {
        this.path = path;
    }
}