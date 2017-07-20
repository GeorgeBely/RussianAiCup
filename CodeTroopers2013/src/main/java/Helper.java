import model.*;

import java.util.*;

public class Helper {
    public static Set<Position> stack = new HashSet<>();
    public static List<Position> nodeMass = new ArrayList<>();
    public static Position[] positions = new Position[5];
    public static Position toPosition;
    public static boolean checkUseRequest;

    /**
     * Находит кратчайший путь от позиции заданной первым элементом в "query", до позиции "finalPosition".
     * Используется метод поиска в ширину.
     * @param query очередь вершин, е\которые проверяются на совпадение с finalPosition.
     * @param finishPosition позиция путь к которой мы ищем.
     */
    public static List<Position> findPath(Queue<Node> query, Position finishPosition) {
        Node start = query.poll();
        if (start != null) {
            for (Position link : start.getPosition().getNodes()) {
                if (link != null && !stack.contains(link)) {
                    if (link.equals(finishPosition)) {
                        stack = new HashSet<>();
                        return start.getPath();
                    } else {
                        if (canMovePosition(link)) {
                            stack.add(link);
                            List<Position> linkPath = new ArrayList<>();
                            linkPath.addAll(start.getPath());
                            linkPath.add(link);
                            query.offer(new Node(link, linkPath));
                        }
                    }
                }
            }
        }
        List<Position> path = null;
        if (query.peek() != null)
            path = findPath(query, finishPosition);

        stack = new HashSet<>();
        return path;
    }

    public static List<Position> findPath(Unit unit) {
        Queue<Node> query = new ArrayDeque<>();
        query.offer(new Node(findNode(new Position(MyStrategy.self)), new ArrayList<Position>()));
        return findPath(query, findNode(new Position(unit)));
    }

    public static void createNodes() {
        for (int i = 0; i < MyStrategy.world.getCells().length; i++) {
            for (int j = 0; j < MyStrategy.world.getCells()[1].length; j++) {
                nodeMass.add(new Position(i, j, canMovePosition(new Position(i, j))));
            }
        }

        for (Position node : nodeMass) {
            List<Position> links = new ArrayList<>();
            if (node.getY() > 0)
                links.add(findNode(node.getX(), node.getY()-1));
            if (node.getX() > 0)
                links.add(findNode(node.getX()-1, node.getY()));
            if (node.getX() < MyStrategy.world.getCells().length-1)
                links.add(findNode(node.getX()+1, node.getY()));
            if (node.getY() < MyStrategy.world.getCells()[1].length)
                links.add(findNode(node.getX(), node.getY()+1));
            node.setNodes(links);
        }
    }

    public static Position findNode(Position position) {
        return findNode(position.getX(), position.getY());
    }

    public static Position findNode(int x, int y) {
        for (Position node : nodeMass) {
            if (node.getX() == x && node.getY() == y) {
                return node;
            }
        }
        return null;
    }

    public static void initPositions() {

        positions[0] = new Position(MyStrategy.world.getWidth()/2, MyStrategy.world.getHeight()/2);
        positions[1] = new Position(1, 1);
        positions[2] = new Position(MyStrategy.world.getWidth()-1, 1);
        positions[3] = new Position(MyStrategy.world.getWidth()-1, MyStrategy.world.getHeight()-1);
        positions[4] = new Position(1, MyStrategy.world.getHeight()-1);

        for (int i = 0; i < 5; i++)
            positions[i] = initFreePosition(positions[i]);

        checkUseRequest = true;
        toPosition = positions[0];
        createNodes();
    }

    public static Position initFreePosition(Position position) {
        Position centerPosition = new Position(MyStrategy.world.getWidth()/2, MyStrategy.world.getHeight()/2);
        boolean checkCenter = centerPosition.equals(position);

        while(true) {
            if (canMovePosition(position)) {
                return position;
            } else {
                int offsetX, offsetY;
                if (checkCenter) {
                    offsetX = position.getX() > positions[1].getX() ? -1 : position.getX() < positions[1].getX() ? 1 : 0;
                    offsetY = position.getY() > positions[1].getY() ? -1 : position.getY() < positions[1].getY() ? 1 : 0;
                } else {
                    offsetX = position.getX() > centerPosition.getX() ? -1 : position.getX() < centerPosition.getX() ? 1 : 0;
                    offsetY = position.getY() > centerPosition.getY() ? -1 : position.getY() < centerPosition.getY() ? 1 : 0;
                }
                boolean canMoveX = offsetX != 0 && Helper.canMovePosition(new Position(position.getX() + offsetX, position.getY()));

                if (canMoveX)
                    position = new Position(position.getX() + offsetX, position.getY());
                else
                    position = new Position(position.getX(), position.getY() + offsetY);
            }
        }
    }

    /**
     * Ищет путь от позиции self до позиции target, и возвращает первый элемент(позицию, на которую надо переместиться).
     * @param self боей, для которого ищется путь.
     * @param target искомая позиция
     * @return позицию, в которую необходимо перейти или null, если путь не найден.
     */
    public static Position initPath(Position self, Position target) {
        Queue<Node> query = new ArrayDeque<>();
        query.offer(new Node(findNode(self), new ArrayList<Position>()));
        List<Position> path = findPath(query, findNode(target));
        return path != null ? path.get(0) : null;
    }

    /**
     * @return Возможно ли передвижение в клетку "target"
     */
    public static boolean canMovePosition(Position target) {
        return MyStrategy.world.getCells()[target.getX()][target.getY()].equals(CellType.FREE) && !haveTrooperPosition(target);
    }

    /**
     * @return Находится ли в данной клетке "target" солдат
     */
    private static boolean haveTrooperPosition( Position target) {
        for (Trooper trooper : MyStrategy.world.getTroopers()) {
            if (new Position(trooper).equals(target))
                return true;
        }
        return false;
    }

    /**
     * Меняет позицию, кодка боец достигает текущей.
     */
    public static void nextPosition() {
        checkUseRequest = false;
        for (int i=0; i < positions.length; i++) {
            if (toPosition.equals(positions[i])) {
                toPosition = positions[i < positions.length-1 ? i+1 : 0];
                return;
            }
        }
    }

    /**
     * Проверяет хватает ли действий движения для данной позиции.
     * @param type тип бойца.
     * @return true, усли хватает.
     */
    public static int getMoveCost(TrooperStance type) {
        if (TrooperStance.STANDING.equals(type))
            return MyStrategy.game.getStandingMoveCost();
        else if (TrooperStance.KNEELING.equals(type))
            return MyStrategy.game.getKneelingMoveCost();
        else
            return MyStrategy.game.getProneMoveCost();
    }

    /**
     * Находит бойца по типу.
     * @param type тип бойца.
     * @return бойца у которого данный тип.
     */
    public static Position findTrooperPosition(TrooperType type) {
        for (Trooper trooper : MyStrategy.teamTroopers) {
            if (trooper.getType().equals(type))
                return new Position(trooper);
        }
        return null;
    }

    /**
     * Рассчитывает необходимое колличество очков действия для заданного манёвра.
     *
     * @param unit объект до которого рассчитывается путь.
     * @param range радиус от объекта, при котором можно использовать действие.
     * @param cost количество очков действие необходимое для совершения действия.
     * @return true, если для данного манёвра хватает очков действия.
     */
    public static boolean haveEnoughPointsForMove(Unit unit, double range, double cost) {
        try {
            return MyStrategy.self.getActionPoints() >= cost + (findPath(unit).size() - range)
                    * getMoveCost(MyStrategy.self.getStance());
        } catch (Exception e) {
            return false;
        }
    }

}