import model.*;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс с общими методами
 */
class StrategyHelper {

    /** Расстояние на котором мы будем задавать близкие точки от заданной */
    private static final double INDENT_LENGTH = 85.0;

    /** Если дистанция от юнита до стартовой точке меньше этого числа, то считаем, что юнит находится на старте */
    private static final int DISTANCE_NEAR_START_POSITION = 400;

    /**
     * Если до ближаёшей контрольной точки какой-либо линнии расстояние больше данного числа,
     * то на эту линию мы становиться не будет
     */
    private static final int MAX_DISTANCE_TO_NEAR_WAYPOINT_LANE = 1500;

    /** Стартовая позиция */
    private static final Point2D startPoint = new Point2D(100.0D, 3900.0D);


    /**
     * Рассчитывает точки пересечение отрезка проходящего через окружность.
     *
     * @param circle центр окружности
     * @param r  радиус окружности
     * @param a первая точка отрезка
     * @param b вторая точка отрезка
     * @return точки пересечения
     */
    private static List<Point2D> lineCircleIntersection(LivingUnit circle, double r, Point2D a, Unit b) {
        List<Point2D> positions = new ArrayList<>();
        double q = Math.pow(circle.getX(), 2) + Math.pow(circle.getY(), 2) - r*r;
        double k = -2.0 * circle.getX();
        double l = -2.0 * circle.getY();

        double z = a.getX() * b.getY() - b.getX()*a.getY();
        double p = a.getY() - b.getY();
        double s = a.getX() - b.getX();

        if (equalDoubles(s, 0.0, 0.001)) {
            s = 0.001;
        }

        double A = s*s + p*p;
        double B = s*s*k + 2.0*z*p + s*l*p;
        double C = q*s*s + z*z + s*l*z;

        double D = B*B - 4.0*A*C;

        if (D > 0.0) {
            if (D < 0.001) {
                double x = -B / (2.0 * A);
                positions.add(new Point2D(x, (p * x + z) / s));
            } else {
                double x = (-B + Math.sqrt(D)) / (2.0 * A);
                double y = (p * x + z) / s;
                positions.add(new Point2D(x, y));

                x = (-B - Math.sqrt(D)) / (2.0 * A);
                y = (p * x + z) / s;
                positions.add(new Point2D(x, y));
            }
        }

        return positions;
    }

    private static boolean equalDoubles(double n1, double n2, double precision_) {
        return (Math.abs(n1-n2) <= precision_);
    }

    /**
     * Вычесляем колличество юнитов на пути от unit до point
     *
     * @param targets юниты, которые могут помешать
     * @param unit    юнит для которого рассчитываем
     * @param point   точка в которою движется unit
     * @return колличество юнитов, которые мешаю передвижению
     */
    static int countUnitByPath(List<LivingUnit> targets, LivingUnit unit, Point2D point) {
        return getUnitByPath(targets, unit, point).size();
    }



    static List<LivingUnit> getUnitByPath(List<LivingUnit> targets, LivingUnit livingUnit, Point2D point) {
        return getUnitByPath(targets, livingUnit, point, livingUnit.getRadius());
    }

    /**
     * Находим юнитов на пути от unit до point
     *
     * @param targets    юниты, которые могут помешать
     * @param livingUnit юнит для которого рассчитываем
     * @param point      точка в которою движется unit
     * @return юниты, которые мешаю передвижению
     */
    static List<LivingUnit> getUnitByPath(List<LivingUnit> targets, LivingUnit livingUnit, Unit point, Double radius) {
        List<LivingUnit> units = new ArrayList<>();

        Point2D pointLeft = getAnglePoint(livingUnit, radius, livingUnit.getAngle() + livingUnit.getAngleTo(point) + Math.PI/2);
        Point2D pointRight = getAnglePoint(livingUnit, radius, livingUnit.getAngle() + livingUnit.getAngleTo(point) - Math.PI/2);
        Point2D pointToLeft = getAnglePoint(point, radius, livingUnit.getAngle() + livingUnit.getAngleTo(point) + Math.PI/2);
        Point2D pointToRight = getAnglePoint(point, radius, livingUnit.getAngle() + livingUnit.getAngleTo(point) - Math.PI/2);

        targets.stream()
                .filter(unit -> unit.getId() != point.getId())
                .filter(unit -> targets.size() == 1 || !(unit instanceof Wizard && ((Wizard) unit).isMe()))
                .filter(unit -> point.getDistanceTo(unit) - unit.getRadius() <= point.getDistanceTo(livingUnit))
                .filter(unit -> livingUnit.getDistanceTo(unit) - unit.getRadius() - livingUnit.getRadius() <= point.getDistanceTo(livingUnit))
                .forEach(unit -> {
            int unitRadius = ((int) unit.getRadius());
            unitRadius++;

            List<Point2D> intersections = new ArrayList<>();
            intersections.addAll(lineCircleIntersection(unit, unitRadius, pointLeft, pointToLeft));
            intersections.addAll(lineCircleIntersection(unit, unitRadius, pointRight, pointToRight));
            intersections.addAll(lineCircleIntersection(unit, unitRadius, new Point2D(livingUnit), point));
            if (intersections.size() > 0) {
                if (Math.abs(StrictMath.hypot(livingUnit.getSpeedX(), livingUnit.getSpeedY()) - StrictMath.hypot(unit.getSpeedX(), unit.getSpeedY())) > 1
                        || Math.abs(StrictMath.hypot(livingUnit.getSpeedX(), livingUnit.getSpeedY())) < 1)
                    units.add(unit);
            }
        });
        return units;
    }

    /**
     * Находим всех юнитов на расстоянии {range} от {unit}
     *
     * @param unit        юнит для которого ищем близкие юниты
     * @param range       дистанция меньше которой должено быть расстояние от {unit} до {livingUnit}
     * @param livingUnits список юнитов, которые могут быть рядом
     * @param faction     из какой фракции выбирать
     * @return список близких юнитов
     */
    static List<LivingUnit> getNearLivingUnit(Unit unit, Double range, List<LivingUnit> livingUnits, Faction faction) {
        return livingUnits.stream()
                .filter(livingUnit -> livingUnit.getFaction().equals(faction))
                .filter(livingUnit -> livingUnit.getDistanceTo(unit) < range)
                .collect(Collectors.toList());
    }

    /**
     * @param points список контрольных точек
     * @return ближайшую контрольную точку из списка для {unit}
     */
    static Integer getNearPointIndex(LivingUnit unit, Point2D[] points) {
        Integer index = null;
        Double distance = null;

        int i = 0;
        for (Point2D point : points) {
            Double distanceToPoint = point.getDistanceTo(unit);
            if (distance == null || distance > distanceToPoint) {
                distance = distanceToPoint;
                index = i;
            }
            i++;
        }
        return index;
    }

    static LivingUnit getNearUnit(LivingUnit unit, List<LivingUnit> units) {
        return getNearUnit(unit, units, 4000.0);
    }

    /**
     * @param units список контрольных точек
     * @return ближайшую контрольную точку из списка для {unit}
     */
    static LivingUnit getNearUnit(LivingUnit unit, List<LivingUnit> units, Double radius) {
        LivingUnit nearPoint = null;
        Double distance = null;

        for (LivingUnit livingUnit : units) {
            if (livingUnit.getId() != unit.getId()) {
                Double distanceToPoint = livingUnit.getDistanceTo(unit);
                if (distanceToPoint < radius && (distance == null || distance > distanceToPoint)) {
                    distance = distanceToPoint;
                    nearPoint = livingUnit;
                }
            }
        }
        return nearPoint;
    }

    /**
     * Определяет точку сзади переданного юнита
     *
     * @param distance дистанция от юнита до точки
     */
    static Point2D getBackPoint(Unit unit, Double distance) {
        return getAnglePoint(unit, distance, unit.getAngle());
    }

    static Point2D getAnglePoint(Unit unit, Double distance, Double angleA) {
        Double angleB = (Math.PI * (90 - ((angleA * 180) / Math.PI))) / 180.0;
        Double angleC = (Math.PI * 90) / 180.0;
        Double distanceA = distance * (Math.sin(angleA)/Math.sin(angleC));
        Double distanceB = distance * (Math.sin(angleB)/Math.sin(angleC));

        return new Point2D(unit.getX() - distanceB, unit.getY() - distanceA);
    }

    /**
     * @return {true} если волшебник под статусом {statusType}
     */
    static boolean isHaveStatus(LivingUnit unit, StatusType statusType) {
        for (Status status : unit.getStatuses()) {
            if (status.getType().equals(statusType)) {
                return true;
            }
        }
        return false;
    }

    private static List<Point2D> getCrossPoints(Unit waypoint, Double indentX, Double indentY) {
        List<Point2D> crossPoints = new ArrayList<>();

        if (indentY != 0 && indentX != 0) {
            crossPoints.add(new Point2D(waypoint.getX() - indentX, waypoint.getY() + indentY));
            crossPoints.add(new Point2D(waypoint.getX() + indentX, waypoint.getY() - indentY));
        }
        crossPoints.add(new Point2D(waypoint.getX() - indentX, waypoint.getY() - indentY));
        crossPoints.add(new Point2D(waypoint.getX() + indentX, waypoint.getY() + indentY));

        return crossPoints;
    }

    private static List<Point2D> getAboutPoint(Unit waypoint, Double distance, Double step) {
        List<Point2D> viewList = new ArrayList<>();

        for (double i = 0.0; i <= distance; i += distance * step) {
            viewList.addAll(getCrossPoints(waypoint, distance, i));
            if (i != distance)
                viewList.addAll(getCrossPoints(waypoint, i, distance));
        }
        return viewList;
    }

    private static List<Point2D> getPointsAboutWaypoint(Unit waypoint) {
        List<Point2D> viewList = getAboutPoint(waypoint, INDENT_LENGTH, 0.25);
        viewList.addAll(getAboutPoint(waypoint, INDENT_LENGTH * 2, 0.125));
        viewList.addAll(getAboutPoint(waypoint, INDENT_LENGTH * 3, 0.25));
        viewList.addAll(getAboutPoint(waypoint, INDENT_LENGTH * 4, 0.5));
        return viewList;
    }

    /**
     * Определяет наилучшую точку аокруг заданной, к которой будет перемещаться волшебник
     *
     * @param blockUnits список объектов, которые могут помешать движению
     * @return точка, к которой будем перемещаться
     */
    private static Point2D getViewPoint(LivingUnit self, List<LivingUnit> blockUnits, Point2D originalPoint,
                                        boolean useEnemyUnit) {
        LivingUnit nearEnemy = getNearUnit(self, DataHelper.enemyUnits);
        Comparator<Point2D> comparator = (p1, p2) -> {
            if (originalPoint.equals(p1)) {
                return -1;
            }
            if (originalPoint.equals(p2)) {
                return 1;
            }
            if (originalPoint.equals(startPoint) && startPoint.getDistanceTo(self) < 400 && nearEnemy != null && nearEnemy.getDistanceTo(self) < 400) {
                if (nearEnemy.getDistanceTo(p1) < nearEnemy.getDistanceTo(p2)) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                if (originalPoint.getDistanceTo(p1) < originalPoint.getDistanceTo(p2)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };
        List<Point2D> points = new ArrayList<>();
        points.add(originalPoint);
        points.addAll(getPointsAboutWaypoint(self));
        Collections.sort(points, comparator);

        Optional<Point2D> optional = points.stream()
                .filter(point -> StrategyHelper.countUnitByPath(blockUnits, self, point) == 0)
                .filter(point -> point.getX() > self.getRadius())
                .filter(point -> point.getY() > self.getRadius())
                .filter(point -> point.getX() < 4000 - self.getRadius())
                .filter(point -> point.getY() < 4000 - self.getRadius())
                .filter(point -> originalPoint.getDistanceTo(self) - self.getRadius() > originalPoint.getDistanceTo(point))
                .filter(point -> !useEnemyUnit || DataHelper.enemyUnits.isEmpty() || StrategyHelper.getNearUnit(point, DataHelper.enemyUnits).getDistanceTo(point) >= self.getDistanceTo(point))
                .findFirst();

        if (optional.isPresent()) {
            return optional.get();
        }
        return originalPoint;
    }

    static Point2D getPriorityPoint(List<LivingUnit> blockUnits, Point2D pointA, LivingUnit self) {
        return getPriorityPoint(blockUnits, pointA, self, false);
    }

    static Point2D getPriorityPoint(List<LivingUnit> blockUnits, Point2D pointA, LivingUnit self,
                                    boolean useEnemyUnit) {
        if (StrategyHelper.countUnitByPath(blockUnits, self, pointA) == 0) {
            if (DataHelper.enemyUnits == null || DataHelper.enemyUnits.isEmpty()) {
                return pointA;
            } else if (getNearUnit(pointA, DataHelper.enemyUnits) != null && getNearUnit(pointA, DataHelper.enemyUnits).getDistanceTo(pointA) > self.getDistanceTo(pointA)) {
                return pointA;
            }
        }

        return StrategyHelper.getViewPoint(self, blockUnits, pointA, useEnemyUnit);
    }

    /**
     * @return линию на которой {unit} или {null} если линия не определена
     */
    static LaneType getLane(LivingUnit unit) {
        if (DataHelper.selfBase.getDistanceTo(unit) < 150) {
            return null;
        }
        if (unit.getX() > 3500 || unit.getY() > 3500 || (unit.getX() > 3200 && unit.getY() > 3200)) {
            return LaneType.BOTTOM;
        }
        if (unit.getX() < 500 || unit.getY() < 500 || (unit.getX() < 800 && unit.getY() < 800)) {
            return LaneType.TOP;
        }
        if (Math.abs(4000 - unit.getX() - unit.getY()) < 500 || (unit.getX() > 1600 && unit.getX() < 2400 && unit.getY() > 1600 && unit.getY() < 2400)) {
            return LaneType.MIDDLE;
        }
        return null;
    }

    static boolean onRiver(LivingUnit unit) {
        return Math.abs(unit.getX() - unit.getY()) < 600;
    }


    private static int countUnitsByLane(LaneType laneType, Faction faction, List<LivingUnit> units) {
        if (laneType == null || faction == null || units == null || units.isEmpty())
            return 0;

        return (int) units.stream()
                .filter(unit -> !(unit instanceof Wizard && ((Wizard) unit).isMe()))
                .filter(unit -> startPoint.getDistanceTo(unit) > DISTANCE_NEAR_START_POSITION)
                .filter(unit -> unit.getFaction().equals(faction))
                .filter(unit -> laneType.equals(getLane(unit)))
                .count();
    }

    /**
     * Выбирает оптимальную линию, по наличию на ней волшебников, чем меньше тем лучше
     * @return линию, по которой будем двигаться
     */
    static LaneType selectMoveLine(World world, Wizard self) {
        int countTop = calculateWeightLane(LaneType.TOP, world, self);
        int countMiddle = calculateWeightLane(LaneType.MIDDLE, world, self);
        int countBottom = calculateWeightLane(LaneType.BOTTOM, world, self);

        if (countBottom < countMiddle && countBottom < countTop)
            return LaneType.BOTTOM;
        if (countTop < countMiddle && countTop <= countBottom)
            return LaneType.TOP;
        return LaneType.MIDDLE;
    }

    /**
     * @return вес линнии. Чем больше вес тем хуже туда идти.
     */
    private static int calculateWeightLane(LaneType laneType, World world, Wizard self) {
        int count = 0;
        int countOur = countUnitsByLane(laneType, self.getFaction(), Arrays.asList(world.getWizards()));
        int countEnemy = countUnitsByLane(laneType, self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY, Arrays.asList(world.getWizards()));


        if (countOur == 1 && countEnemy == 1) {
            count -= 3;
        } else if (countOur == 0 && countEnemy == 0) {
            count -= 2;
        } else if (countOur == 0 && countEnemy == 1) {
            count -= 1;
        } else if (countOur > countEnemy) {
            count += 1;
        } else if (countEnemy == countOur) {
            count += 2;
        } else {
            count += 3;
        }

        if (DataHelper.enemyMinionPositionIndex != null && DataHelper.enemyMinionPositionIndex.containsKey(laneType)) {
            if (DataHelper.enemyMinionPositionIndex.get(laneType) < DataHelper.getMinSafeLaneWaypointIndex(laneType)) {
                count -= (DataHelper.getMinSafeLaneWaypointIndex(laneType) - DataHelper.enemyMinionPositionIndex.get(laneType));
            }
        }

        if (getNearUnit(self, Arrays.asList(DataHelper.laneWaypoints.get(laneType))).getDistanceTo(self) > MAX_DISTANCE_TO_NEAR_WAYPOINT_LANE) {
            count += 10;
        }

        if ((BonusHelper.BONUS_BOTTOM.getDistanceTo(self) < 400 || BonusHelper.BONUS_TOP.getDistanceTo(self) < 400)
                && DataHelper.enemyMinionPositionIndex.get(laneType) != null) {
            if (DataHelper.enemyMinionPositionIndex.get(laneType) < getMinSafePosition(laneType)
                    || (Objects.equals(DataHelper.enemyMinionPositionIndex.get(laneType), getMinSafePosition(laneType)) && DataHelper.enemyMiddleTower != null)) {
                count += 5;
            }
        }

        return count;
    }

    /**
     * @return индекс средней контрольной точки на линии {laneType}
     */
    static Integer getMinSafePosition(LaneType laneType) {
        return getWaypointIndex(DataHelper.minSaveIndexLaneWaypoint.get(laneType), DataHelper.laneWaypoints.get(laneType));
    }

    /**
     * @param waypoint  чекпоинт
     * @param waypoints список чекпоинтов
     * @return индекс чекпоинта из списка
     */
    static int getWaypointIndex(Point2D waypoint, Point2D[] waypoints) {
        int i = 0;
        for (Point2D point : waypoints) {
            if (point.equals(waypoint))
                return i;
            i++;
        }
        return 0;
    }


    static int getDamage(Wizard wizard, Game game) {
        Integer damage = game.getMagicMissileDirectDamage();
        if (game.isSkillsEnabled()) {
            if (haveSkill(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2, wizard, game))
                damage += game.getMagicalDamageBonusPerSkillLevel() * 2;
            else if (haveSkill(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1, wizard, game))
                damage += game.getMagicalDamageBonusPerSkillLevel();

            if (haveSkill(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2, wizard, game) || isSelfInAuraSkillRange(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2, wizard, game))
                damage += game.getMagicalDamageBonusPerSkillLevel() * 2;
            else if (haveSkill(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1, wizard, game) || isSelfInAuraSkillRange(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1, wizard, game))
                damage += game.getMagicalDamageBonusPerSkillLevel();
        }
        if (StrategyHelper.isHaveStatus(wizard, StatusType.EMPOWERED)) {
            damage = (int) (damage * game.getEmpoweredDamageFactor());
        }

        return damage;
    }

    private static int getLifeWithArmor(Wizard wizard, Game game) {
        Integer life = wizard.getLife();
        if (game.isSkillsEnabled()) {
            int count = 0;

            if (haveSkill(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2, wizard, game))
                count += game.getMagicalDamageAbsorptionPerSkillLevel() * 2;
            else if (haveSkill(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1, wizard, game))
                count += game.getMagicalDamageAbsorptionPerSkillLevel();

            if (haveSkill(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2, wizard, game) || isSelfInAuraSkillRange(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2, wizard, game))
                count += game.getMagicalDamageAbsorptionPerSkillLevel() * 2;
            else if (haveSkill(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1, wizard, game) || isSelfInAuraSkillRange(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1, wizard, game))
                count += game.getMagicalDamageAbsorptionPerSkillLevel();

            life += count * (life / game.getMagicMissileDirectDamage());
        }
        if (StrategyHelper.isHaveStatus(wizard, StatusType.SHIELDED)) {
            life += (int) (life * 0.25);
        }

        return life;
    }

    static double getSpeedWithMovementBonus(Wizard wizard, Game game, Double defaultSpeed) {
        Double speed = defaultSpeed;

        if (game.isSkillsEnabled()) {
            if (haveSkill(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2, wizard, game))
                speed += game.getMovementBonusFactorPerSkillLevel() * 2;
            else if (haveSkill(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1, wizard, game))
                speed += game.getMovementBonusFactorPerSkillLevel();

            if (haveSkill(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2, wizard, game) || isSelfInAuraSkillRange(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2, wizard, game))
                speed += game.getMovementBonusFactorPerSkillLevel() * 2;
            else if (haveSkill(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1, wizard, game) || isSelfInAuraSkillRange(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1, wizard, game))
                speed += game.getMovementBonusFactorPerSkillLevel();
        }

        if (StrategyHelper.isHaveStatus(wizard, StatusType.HASTENED)) {
            speed += speed * game.getHastenedMovementBonusFactor();
        }

        return speed;
    }

    private static boolean isSelfInAuraSkillRange(SkillType auraType, Wizard useWizard, Game game) {
        for (Wizard wizard : DataHelper.selfWizards) {
            if (wizard.getId() != useWizard.getId()
                    && wizard.getFaction().equals(useWizard.getFaction())
                    && wizard.getDistanceTo(useWizard) < game.getAuraSkillRange() && haveSkill(wizard, auraType))
                return true;
        }
        return false;
    }


    static boolean haveSkill(SkillType skill, Wizard wizard, Game game) {
        return game.isSkillsEnabled() && haveSkill(wizard, skill);
    }

    private static boolean haveSkill(Wizard wizard, SkillType skill) {
        return Arrays.asList(wizard.getSkills()).contains(skill);
    }

    static int countShotsForKill(Wizard attacker, Wizard victim, Game game) {
        double count = getLifeWithArmor(victim, game)/getDamage(attacker, game);

        return ((int) count) + (count % 1 > 0 ? 1 : 0);
    }

    static Minion getMinionById(Minion[] minions, Long id) {
        return (Minion) getUnitById(Arrays.stream(minions).collect(Collectors.toList()), id);
    }

    static Wizard getWizardById(Wizard[] wizards, Long id) {
        return (Wizard) getUnitById(Arrays.stream(wizards).collect(Collectors.toList()), id);
    }

    private static Unit getUnitById(List<Unit> units, Long id) {
        for (Unit unit : units) {
            if (unit.getId() == id) {
                return unit;
            }
        }
        return null;
    }
}
