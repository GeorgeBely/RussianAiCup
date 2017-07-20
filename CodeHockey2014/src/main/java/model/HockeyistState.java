package model;

/**
 * Состояние хоккеиста.
 */
public enum HockeyistState {
    /**
     * Хоккеист находится на игровом поле.
     */
    ACTIVE,

    /**
     * Хоккеист находится на игровом поле и делает замах клюшкой.
     * <p/>
     * Во время замаха стратегия не может управлять движением хоккеиста, а из действий доступны только
     * {@code ActionType.STRIKE} и {@code ActionType.CANCEL_STRIKE}.
     */
    SWINGING,

    /**
     * Хоккеист находится на игровом поле, но сбит с ног.
     * Стратегия игрока не может им управлять.
     */
    KNOCKED_DOWN,

    /**
     * Хоккеист отдыхает вне игрового поля.
     * Стратегия игрока не может им управлять.
     */
    RESTING
}