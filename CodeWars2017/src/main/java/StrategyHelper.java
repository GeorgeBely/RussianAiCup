import model.TerrainType;
import model.Vehicle;
import model.VehicleType;
import model.WeatherType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author George Beliy on 17-11-17
 */
class StrategyHelper {

    static final int CELL_WIDTH = 32;


    private static final Map<TerrainType, Double> VISION_FACTOR_BY_TERRAIN_TYPE = new HashMap<TerrainType, Double>() {{
        put(TerrainType.FOREST, MyStrategy.game.getForestTerrainVisionFactor());
        put(TerrainType.PLAIN, MyStrategy.game.getPlainTerrainVisionFactor());
        put(TerrainType.SWAMP, MyStrategy.game.getSwampTerrainVisionFactor());
    }};

    private static final Map<WeatherType, Double> VISION_FACTOR_BY_WEATHER_TYPE = new HashMap<WeatherType, Double>() {{
        put(WeatherType.CLEAR, MyStrategy.game.getClearWeatherVisionFactor());
        put(WeatherType.CLOUD, MyStrategy.game.getCloudWeatherVisionFactor());
        put(WeatherType.RAIN, MyStrategy.game.getRainWeatherVisionFactor());
    }};

    static final Map<TerrainType, Double> MOVE_FACTOR_BY_TERRAIN_TYPE = new HashMap<TerrainType, Double>() {{
        put(TerrainType.FOREST, MyStrategy.game.getForestTerrainSpeedFactor());
        put(TerrainType.PLAIN, MyStrategy.game.getPlainTerrainSpeedFactor());
        put(TerrainType.SWAMP, MyStrategy.game.getSwampTerrainSpeedFactor());
    }};

    static final Map<WeatherType, Double> MOVE_FACTOR_BY_WEATHER_TYPE = new HashMap<WeatherType, Double>() {{
        put(WeatherType.CLEAR, MyStrategy.game.getClearWeatherSpeedFactor());
        put(WeatherType.CLOUD, MyStrategy.game.getCloudWeatherSpeedFactor());
        put(WeatherType.RAIN, MyStrategy.game.getRainWeatherSpeedFactor());
    }};

    static final Map<VehicleType, Double> VISION_RANGE_BY_TYPE = new HashMap<VehicleType, Double>() {{
        put(VehicleType.FIGHTER, MyStrategy.game.getFighterVisionRange());
        put(VehicleType.HELICOPTER, MyStrategy.game.getHelicopterVisionRange());
        put(VehicleType.TANK, MyStrategy.game.getTankVisionRange());
        put(VehicleType.IFV, MyStrategy.game.getIfvVisionRange());
        put(VehicleType.ARRV, MyStrategy.game.getArrvVisionRange());
    }};

    static final Map<VehicleType, Double> MAX_SPEED_BY_TYPE = new HashMap<VehicleType, Double>() {{
        put(VehicleType.FIGHTER, MyStrategy.game.getFighterSpeed());
        put(VehicleType.HELICOPTER, MyStrategy.game.getHelicopterSpeed());
        put(VehicleType.TANK, MyStrategy.game.getTankSpeed());
        put(VehicleType.IFV, MyStrategy.game.getIfvSpeed());
        put(VehicleType.ARRV, MyStrategy.game.getArrvSpeed());
    }};


    static int minX(Collection<Vehicle> vehicles) {
        return (int) vehicles.stream().mapToDouble(Vehicle::getX).min().orElse(Double.NaN);
    }
    static int maxX(Collection<Vehicle> vehicles) {
        return (int) vehicles.stream().mapToDouble(Vehicle::getX).max().orElse(Double.NaN);
    }
    static int minY(Collection<Vehicle> vehicles) {
        return (int) vehicles.stream().mapToDouble(Vehicle::getY).min().orElse(Double.NaN);
    }
    static int maxY(Collection<Vehicle> vehicles) {
        return (int) vehicles.stream().mapToDouble(Vehicle::getY).max().orElse(Double.NaN);
    }
    private static int centerX(Collection<Vehicle> vehicles) {
        return (int) vehicles.stream().mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
    }
    private static int centerY(Collection<Vehicle> vehicles) {
        return (int) vehicles.stream().mapToDouble(Vehicle::getY).average().orElse(Double.NaN);
    }
    static Point min(Collection<Vehicle> vehicles) {
        return new Point(minX(vehicles), minY(vehicles));
    }
    static Point max(Collection<Vehicle> vehicles) {
        return new Point(maxX(vehicles), maxY(vehicles));
    }
    static Point minXmaxY(Collection<Vehicle> vehicles) {
        return new Point(minX(vehicles), maxY(vehicles));
    }
    static Point maxXminY(Collection<Vehicle> vehicles) {
        return new Point(maxX(vehicles), minY(vehicles));
    }
    static Point center(Collection<Vehicle> vehicles) {
        return new Point(centerX(vehicles), centerY(vehicles));
    }

    static void clearPP(Group group) {
        for (int i = 0; i < CELL_WIDTH; i++) {
            System.arraycopy(group.defaultPotentialPoints[i], 0, group.potentialPoints[i],
                    0, CELL_WIDTH);
        }
    }

    static void initDefaultPP(Group group) {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
//                Double factor = group.isAir()
//                        ? MOVE_FACTOR_BY_WEATHER_TYPE.get(MyStrategy.world.getWeatherByCellXY()[i][j])
//                        : MOVE_FACTOR_BY_TERRAIN_TYPE.get(MyStrategy.world.getTerrainByCellXY()[i][j]);
//                group.defaultPotentialPoints[i][j] = (1.0 - factor) * 0.001 + 1;
                group.defaultPotentialPoints[i][j] = 1.0;
            }
        }
    }

    static void updateVehicle(Vehicle vehicle, Map<Long, Vehicle> vehicleMap) {
        if (vehicle.getDurability() != 0) {
            vehicleMap.put(vehicle.getId(), vehicle);
        } else {
            vehicleMap.remove(vehicle.getId());
        }
    }

    static double getVisionRange(Vehicle vehicle) {
        return vehicle.getVisionRange() * (vehicle.getType().equals(VehicleType.FIGHTER) || vehicle.getType().equals(VehicleType.HELICOPTER)
                ? VISION_FACTOR_BY_WEATHER_TYPE.get(MyStrategy.world.getWeatherByCellXY()[(int) vehicle.getX()/CELL_WIDTH][(int) vehicle.getY()/CELL_WIDTH])
                : VISION_FACTOR_BY_TERRAIN_TYPE.get(MyStrategy.world.getTerrainByCellXY()[(int) vehicle.getX()/CELL_WIDTH][(int) vehicle.getY()/CELL_WIDTH]));
    }

    static Point getPriorityPoint(Double[][] pp, Point center, double range, double maxFactor) {
        Integer rI = null;
        Integer rJ = null;

        int centerX = center.x/32;
        int centerY = center.y/32;

        int rangeI = (int) range/32 + 1;
        int dI1 = centerX - rangeI;
        dI1 = dI1 < 0 ? 0 : dI1;
        int dI2 = centerX + rangeI;
        dI2 = dI2 > CELL_WIDTH ? CELL_WIDTH : dI2;
        int dJ1 = centerY - rangeI;
        dJ1 = dJ1 < 0 ? 0 : dJ1;
        int dJ2 = centerY + rangeI;
        dJ2 = dJ2 > CELL_WIDTH ? CELL_WIDTH : dJ2;

        for (int i = dI1; i < dI2; i++) {
            for (int j = dJ1; j < dJ2; j++) {
                if (center.distanceTo(new Point(i * CELL_WIDTH + CELL_WIDTH / 2, j * CELL_WIDTH + CELL_WIDTH / 2)) < range) {
                    if (rI == null || pp[rI][rJ] > pp[i][j]) {
                        rI = i;
                        rJ = j;
                    }
                }
            }
        }
        if (rI == null
                || (pp[rI][rJ] == 1 && center.getPPoint(pp) == 1)
                ) {
            return null;
        }

        return new Point(rI * CELL_WIDTH + CELL_WIDTH/2, rJ * CELL_WIDTH + CELL_WIDTH/2);
    }

    static Map<Long, Vehicle> safeCopyVehicles(Map<Long, Vehicle> map) {
        Map<Long, Vehicle> result = new HashMap<>();
        for (Map.Entry<Long, Vehicle> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
