import model.ActionType;
import model.Move;
import model.VehicleType;


class MoveAction {

    boolean selected;
    Point point;
    boolean moved;

    int id;
    int priority;
    int createTick;
    boolean notReplace;

    ActionType action;
    int group;
    double left;
    double top;
    double right;
    double bottom;
    double x;
    double y;
    double angle;
    double factor;
    double maxSpeed;
    double maxAngularSpeed;
    VehicleType vehicleType;
    long facilityId = -1L;
    long vehicleId = -1L;



    MoveAction() {
    }

    MoveAction(int priority, int createTick, int group) {
        this.priority = priority;
        this.createTick = createTick;
        this.group = group;
    }

    void update(MoveAction moveAction) {
        this.point = moveAction.point;
        this.id = moveAction.id;
        this.priority = moveAction.priority;
        this.createTick = moveAction.createTick;
        this.action = moveAction.action;
        this.group = moveAction.group;
        this.left = moveAction.left;
        this.top = moveAction.top;
        this.right = moveAction.right;
        this.bottom = moveAction.bottom;
        this.x = moveAction.x;
        this.y = moveAction.y;
        this.angle = moveAction.angle;
        this.factor = moveAction.factor;
        this.maxSpeed = moveAction.maxSpeed;
        this.maxAngularSpeed = moveAction.maxAngularSpeed;
        this.vehicleType = moveAction.vehicleType;
        this.facilityId = moveAction.facilityId;
        this.vehicleId = moveAction.vehicleId;
    }


    void accept(Move move) {
        if (selected) {
            move.setAction(action);
            move.setGroup(group);
            move.setX(x);
            move.setY(y);
            move.setAngle(angle);
            move.setFactor(factor);
            move.setMaxSpeed(maxSpeed);
            move.setMaxAngularSpeed(maxAngularSpeed);
            move.setVehicleType(vehicleType);
            move.setFacilityId(facilityId);
            move.setVehicleId(vehicleId);
        } else {
            if (left == 0 && right == 0 && top == 0 && bottom == 0) {
                move.setGroup(group);
            } else {
                move.setLeft(left);
                move.setTop(top);
                move.setRight(right);
                move.setBottom(bottom);
            }
            move.setAction(ActionType.CLEAR_AND_SELECT);

            selected = true;
        }
    }
}
