package me.despical.pixelpainter.utils;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public enum Direction {

	UP_SOUTH("South"), UP_NORTH("North"), UP_EAST("East"), UP_WEST("West"),
	FLAT_NORTHEAST("FlatNorthEast"), FLAT_SOUTHEAST("FlatSouthEast"), FLAT_NORTHWEST("FlatNorthWest"), FLAT_SOUTHWEST("FlatSouthWest");

	String direction;

	Direction(String dir) {
		this.direction = dir;
	}

	public static Direction getDirection(String direction) {
		for (Direction dir : Direction.values()) {
			if (dir.getName().equalsIgnoreCase(direction))
				return dir;
		}

		return null;
	}

	public String getName() {
		return direction;
	}
}