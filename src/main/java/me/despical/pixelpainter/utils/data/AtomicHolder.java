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

/**
 * @author Despical
 * <p>
 * Created at 5.08.2022
 */
public class AtomicHolder {

	private boolean bool;
	private String string;
	private int integer;

	public void setBool(boolean bool) {
		this.bool = bool;
	}

	public boolean getBool() {
		return bool;
	}

	public void setString(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}

	public void setInteger(int integer) {
		this.integer = integer;
	}

	public int getInteger() {
		return integer;
	}
}