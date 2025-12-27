package dev.despical.pixelpainter.utils;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public enum Direction {

    UP_SOUTH("South"),
    UP_NORTH("North"),
    UP_EAST("East"),
    UP_WEST("West"),
    FLAT_NORTHEAST("FlatNorthEast"),
    FLAT_SOUTHEAST("FlatSouthEast"),
    FLAT_NORTHWEST("FlatNorthWest"),
    FLAT_SOUTHWEST("FlatSouthWest");

    private final String name;

    Direction(String dir) {
        this.name = dir;
    }

    public static Direction getDirection(String directionName) {
        for (Direction direction : Direction.values()) {
            if (direction.name.equalsIgnoreCase(directionName)) {
                return direction;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }
}
