/*
 * PixelPainter - Turn images into Minecraft block art.
 * Copyright (C) 2022-2026  Berke Akçen
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

package dev.despical.pixelpainter.core;

import java.util.Arrays;
import java.util.Locale;

/**
 * Maps top-to-bottom image pixels into vertical walls or horizontal floor art.
 * <p>
 * The origin is the bottom-left image block. Columns extend along the selected
 * right vector; source row zero lies at the far end of the image-up vector.
 * Vertical names identify the outward-facing normal of a wall.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public enum Orientation {

    /**
     * North-facing wall, with image right pointing east.
     */
    NORTH(1, 0, 0, 1, 0),

    /**
     * South-facing wall, with image right pointing west.
     */
    SOUTH(-1, 0, 0, 1, 0),

    /**
     * East-facing wall, with image right pointing south.
     */
    EAST(0, 1, 0, 1, 0),

    /**
     * West-facing wall, with image right pointing north.
     */
    WEST(0, -1, 0, 1, 0),

    /**
     * Horizontal art extending right eastward and up northward.
     */
    FLAT_NORTHEAST(1, 0, 0, 0, -1),

    /**
     * Horizontal art extending right eastward and up southward.
     */
    FLAT_SOUTHEAST(1, 0, 0, 0, 1),
}
