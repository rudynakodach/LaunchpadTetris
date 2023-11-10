package io.github.rudynakodach.Lighting;

public enum LaunchpadColor {
    OFF(0),
    GRAY(2),
    WHITE(3),
    RED(5),
    RED_DIM1(6),
    RED_DIM2(7),
    RED_DIM3(106),
    YELLOW(13),
    ORANGE(84),
    CYAN(37),
    GREEN(21),
    DARK_GREEN(123),
    GREEN_YELLOW(122),
    OLD_YELLOW(74),
    FOREST_GREEN(101),
    BLUE(41),
    DARK_BLUE(45),
    PINK(53),
    PURPLE(69);

    private final int value;

    LaunchpadColor(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
