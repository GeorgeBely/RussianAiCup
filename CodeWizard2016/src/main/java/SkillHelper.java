import model.*;

import java.util.Random;


/**
 * Класс для работы со скилами
 */
class SkillHelper {

    /** Создаём singleton объект данного класса */
    private static SkillHelper helper;
    static synchronized SkillHelper getInstance() {
        if (helper == null) {
            helper = new SkillHelper();
            helper.movesHelper = MovesHelper.getInstance();
        }
        return helper;
    }

    /** Данные которые пришли нам в данный тик */
    private Wizard self;
    private World world;
    private Game game;
    private Move move;

    /** Хелпер для работы с движением */
    private MovesHelper movesHelper;


    WizardType wizardType;

    private int useXp = 0;

    private int currentSkillIndex = 0;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        if (wizardType == null) {
            if (game.isRawMessagesEnabled()) {
                if (self.getId() == 1) {
                    wizardType = WizardType.SAVER_FROST;
                } else if (self.getId() == 2) {
                    wizardType = WizardType.DAMAGER_FIREBALL;
                } else if (self.getId() == 3) {
                    wizardType = WizardType.UNIVERSAL_RANGE;
                } else if (self.getId() == 4) {
                    wizardType = WizardType.SAVER_SHIELD;
                } else if (self.getId() == 5) {
                    wizardType = WizardType.UNIVERSAL_FROST;
                }
            } else {
                wizardType = WizardType.values()[new Random().nextInt(3)];
            }
        }
    }

    boolean isAvailableSkillUp() {
        return self.getLevel() > self.getSkills().length && self.getXp() - useXp > game.getLevelUpXpValues()[self.getLevel() - 1];
    }

    void skillUp() {
        move.setSkillToLearn(getSkill());
        useXp += game.getLevelUpXpValues()[self.getLevel() - 1];
        currentSkillIndex++;
    }


    private SkillType getSkill() {
        return wizardType.skills.get(currentSkillIndex);
    }

    boolean isMayBuff() {
        if (StrategyHelper.haveSkill(SkillType.SHIELD, self, game) && self.getMana() > game.getShieldManacost()
                && self.getRemainingCooldownTicksByAction()[6] == 0) {
            if (!StrategyHelper.isHaveStatus(self, StatusType.SHIELDED)) {
                return true;
            }

            for (Wizard wizard : DataHelper.selfWizards) {
                if (wizard.getDistanceTo(self) < self.getVisionRange() && !StrategyHelper.isHaveStatus(wizard, StatusType.SHIELDED))
                    return true;
            }
        }
        if (StrategyHelper.haveSkill(SkillType.HASTE, self, game) && self.getMana() > game.getHasteManacost()
                && self.getRemainingCooldownTicksByAction()[5] == 0) {
            if (!StrategyHelper.isHaveStatus(self, StatusType.HASTENED)) {
                return true;
            }
            for (Wizard wizard : DataHelper.selfWizards) {
                if (wizard.getDistanceTo(self) < self.getVisionRange() && !StrategyHelper.isHaveStatus(wizard, StatusType.HASTENED))
                    return true;
            }
        }
        return false;
    }

    void buff() {
        buffSkill(SkillType.HASTE);
        buffSkill(SkillType.SHIELD);
    }

    private void buffSkill(SkillType skill) {
        if (StrategyHelper.haveSkill(skill, self, game) && self.getMana() > (SkillType.SHIELD.equals(skill) ? game.getShieldManacost() : game.getHasteManacost())) {
            Wizard buffWizard = null;
            for (Wizard wizard : DataHelper.selfWizards) {
                if (wizard.getDistanceTo(self) < self.getVisionRange()
                        && !StrategyHelper.isHaveStatus(wizard, SkillType.SHIELD.equals(skill) ? StatusType.SHIELDED : StatusType.HASTENED)) {
                    if (buffWizard == null || self.getDistanceTo(buffWizard) > self.getDistanceTo(wizard)) {
                        buffWizard = wizard;
                    }
                }
            }
            if (buffWizard != null) {
                Double angle = self.getAngleTo(buffWizard);
                if (StrictMath.abs(angle) > game.getStaffSector() / 2.0D) {
                    move.setTurn(angle);
                }
                Double distance = self.getDistanceTo(buffWizard);
                if (distance > self.getCastRange()) {
                    movesHelper.nextWaypoint = new Point2D(buffWizard);
                    movesHelper.goTo(movesHelper.getNextWaypoint(), false);
                }
                if (StrictMath.abs(angle) <= Math.PI/12 && distance <= self.getCastRange()) {
                    move.setAction(SkillType.SHIELD.equals(skill) ? ActionType.SHIELD : ActionType.HASTE);
                    move.setStatusTargetId(buffWizard.getId());
                    return;
                }
            }
            if (!StrategyHelper.isHaveStatus(self, SkillType.SHIELD.equals(skill) ? StatusType.SHIELDED : StatusType.HASTENED)) {
                move.setAction(SkillType.SHIELD.equals(skill) ? ActionType.SHIELD : ActionType.HASTE);
            }
        }
    }

    int getDamage() {
        return StrategyHelper.getDamage(self, game);
    }

}
