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

package me.despical.pixelpainter.utils.data;

import org.bukkit.Location;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class DataHolder {

	public MaterialData materialData;
	public Location location;

	private boolean hasFaces = false;

	public DataHolder(Location location, MaterialData materialData) {
		this.location = location;
		this.materialData = materialData;
	}

	public DataHolder(Location b, MaterialData md, boolean hasFaces) {
		this.location = b;
		this.materialData = md;
		this.hasFaces = hasFaces;
	}

	public boolean hasFaces() {
		return hasFaces;
	}
}