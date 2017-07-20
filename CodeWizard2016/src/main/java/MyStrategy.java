import model.*;


/**
 * Основной класс. Логика стратегии
 */
public final class MyStrategy implements Strategy {

    private MovesHelper movesHelper = MovesHelper.getInstance();
    private AttackHelper attackHelper = AttackHelper.getInstance();
    private SaveHelper saveHelper = SaveHelper.getInstance();
    private BonusHelper bonusHelper = BonusHelper.getInstance();
    private SkillHelper skillHelper = SkillHelper.getInstance();


    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        DataHelper.init(world, self);
        skillHelper.initializeTick(self, world, game, move);
        movesHelper.initializeTick(self, world, game, move);
        attackHelper.initializeTick(self, world, game, move);
        saveHelper.initializeTick(self, world, game, move);
        bonusHelper.initializeTick(self, world, game, move);


        if (game.isSkillsEnabled()) {
            if (skillHelper.isAvailableSkillUp()) {
                skillHelper.skillUp();
            }
        }
        boolean haveAction = false;

        if (saveHelper.safeWizard()) {
            saveHelper.runBack();
            haveAction = true;
        }
        if (skillHelper.isMayBuff()) {
            skillHelper.buff();
            haveAction = true;
        }
        if (!haveAction && attackHelper.isMayAttack()) {
            if ((attackHelper.nearestTarget instanceof Wizard || attackHelper.nearestTarget instanceof Building)
                    && attackHelper.isFinishHim()) {
                attackHelper.attack();
                haveAction = true;
            }
        }
        if (bonusHelper.nearestBonus != null && bonusHelper.isHaveBonus(bonusHelper.nearestBonus)
                && self.getDistanceTo(bonusHelper.nearestBonus) < self.getVisionRange()) {
            bonusHelper.runToBonus();
            haveAction = true;
        }
        if (!haveAction && saveHelper.saveBase()) {
            saveHelper.goToBase();
            haveAction = true;
        }
        if (haveAction) {
            bonusHelper.goToBonus = false;
        }
        if (!haveAction && bonusHelper.isGoToBonus()) {
            bonusHelper.runToBonus();
            haveAction = true;
        }
        if (!haveAction && attackHelper.isMayAttack()) {
            attackHelper.attack();
            haveAction = true;
        }
        if (!haveAction && attackHelper.isMayAttackNeutral()) {
            attackHelper.attackNeutral();
            haveAction = true;
        }
        if (!haveAction) {
            movesHelper.moving();
        }


        VisualHelper.getInstance().print(world, self, game);
    }

}