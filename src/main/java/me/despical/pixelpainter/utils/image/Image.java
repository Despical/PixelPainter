package me.despical.pixelpainter.utils.image;

import me.despical.pixelpainter.utils.Direction;
import org.bukkit.Location;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class Image {

	protected Direction direction;
	protected Location minCorner;
	protected boolean neg, moving, enableTransparent;
	protected String player;

	public boolean isMovingX(Direction dir) {
		return dir == Direction.UP_EAST || dir == Direction.UP_WEST;
	}

	public boolean isMinUpNeg(Direction dir2) {
		return direction == Direction.UP_NORTH || dir2 == Direction.UP_WEST;
	}

	public Location getBlockAt(int height, int width, int imageHeight) {
		switch (direction) {
			case UP_EAST:
				return minCorner.clone()
						.add(width / 2, (imageHeight - height - 1) / 2, 0);
			case UP_WEST:
				return minCorner.clone()
						.add(-width / 2, (imageHeight - height - 1) / 2, 0);
			case UP_NORTH:
				return minCorner.clone()
						.add(0, (imageHeight - height - 1) / 2, -width / 2);
			case UP_SOUTH:
				return minCorner.clone()
						.add(0, (imageHeight - height - 1) / 2, width / 2);
			case FLAT_NORTHEAST:
				return minCorner.clone()
						.add(width / 2, 0, -(imageHeight - height - 1) / 2);
			case FLAT_NORTHWEST:
				return minCorner.clone()
						.add(-width / 2, 0, -(imageHeight - height - 1) / 2);
			case FLAT_SOUTHEAST:
				return minCorner.clone()
						.add((imageHeight - height - 1) / 2, 0, width / 2);
			case FLAT_SOUTHWEST:
				return minCorner.clone()
						.add(-(imageHeight - height - 1) / 2, 0, width / 2);
			default:
				return null;
		}
	}

}
