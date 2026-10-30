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

/**
 * World-independent block geometry and perceptual color mathematics.
 * <p>
 * Inclusive bounds support journal reservation and chunk budgets, while orientation
 * vectors map top-down source rows to walls or horizontal art. Positions do not
 * retain Bukkit worlds. ColorMath linearizes sRGB and converts it into Oklab for
 * nearest-color matching. These helpers can run without live server state.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
package dev.despical.pixelpainter.core;
