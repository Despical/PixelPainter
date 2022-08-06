package me.despical.pixelpainter.utils.image;

import org.bukkit.block.BlockFace;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public enum ImageRelativeBlockDirection {

	FRONT, SIDE, BACK, TOP, BOTTOM;

	public BlockFace convertToBlockFace() {
		switch (this) {
			case FRONT:
				return BlockFace.EAST;
			case BACK:
				return BlockFace.WEST;
			case SIDE:
				return BlockFace.NORTH;
			case TOP:
				return BlockFace.UP;
			case BOTTOM:
				return BlockFace.DOWN;
			default:
				return null;
		}
	}
}
