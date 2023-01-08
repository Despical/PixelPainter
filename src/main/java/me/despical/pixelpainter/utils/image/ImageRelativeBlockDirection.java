/*
 * Pixel Painter - Turn your images into blocks.
 * Copyright (C) 2023 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
