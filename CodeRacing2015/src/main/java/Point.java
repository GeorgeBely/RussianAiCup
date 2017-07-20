import model.TileType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Point implements Comparable<Point> {
    private Double x;
    private Double y;

    private List<Point> child;
    private List<Point> path;
    private HashSet visited;
    private TileType type;


    public Point(Double x, Double y, List<Point> child, TileType type) {
        this.x = x;
        this.y = y;
        this.child = child;
        this.path = new ArrayList<>();
        this.visited = new HashSet();
        this.type = type;
    }

    public Point(Double x, Double y) {
        this(x, y, null);
    }

    public Point(Double x, Double y, TileType type) {
        this(x, y, null, type);
    }

    public Point(TileType type) {
        this.type = type;
    }

    public Point(Integer x, Integer y, TileType type) {
        this(x.doubleValue(), y.doubleValue(), type);
    }


    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public List<Point> getChild() {
        return child;
    }

    public void setChild(List<Point> child) {
        this.child = child;
    }

    public List<Point> getPath() {
        return path;
    }

    public void setPath(List<Point> path) {
        this.path = path;
    }

    public HashSet getVisited() {
        return visited;
    }

    public void setVisited(HashSet visited) {
        this.visited = visited;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }

    public String toString() {
        return "x: " + x + "   y: " + y;
    }

    public boolean equals(Point o) {
        return Objects.equals(x, o.getX()) && Objects.equals(y, o.getY());
    }

    @Override
    public int compareTo(Point p) {
        return 1;
    }
}
