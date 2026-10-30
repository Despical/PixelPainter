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

/**
 * Describes an inclusive, axis-aligned block region in a separately identified world.
 * <p>
 * Both endpoints are part of the region. Overlap checks reserve journal regions,
 * and chunk counting handles negative coordinates using arithmetic shifts.
 *
 * @param minX inclusive minimum X
 * @param minY inclusive minimum Y
 * @param minZ inclusive minimum Z
 * @param maxX inclusive maximum X
 * @param maxY inclusive maximum Y
 * @param maxZ inclusive maximum Z
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /**
     * Creates normalized inclusive bounds from two opposite corners.
     *
     * @param a first corner
     * @param b opposite corner
     * @return bounds containing both corners
     */
    public static Bounds of(Position a, Position b) {
        return new Bounds(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()),
            Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
    }

    /**
     * Checks whether this region shares at least one block with another region.
     *
     * @param other region in the same world
     * @return whether all three inclusive axis spans overlap
     */
    public boolean intersects(Bounds other) {
        return minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
            && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    /**
     * Counts horizontal chunks touched by the inclusive X/Z span.
     *
     * @return chunk count using wide arithmetic
     */
    public long chunks() {
        return ((long) (maxX >> 4) - (minX >> 4) + 1) * ((long) (maxZ >> 4) - (minZ >> 4) + 1);
    }
}
