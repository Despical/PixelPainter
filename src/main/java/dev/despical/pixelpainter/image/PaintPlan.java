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

package dev.despical.pixelpainter.image;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores prepared block data and material totals without a live world reference.
 * <p>
 * The block array is a row-major image, with row zero at the top. A null entry
 * preserves the existing block. Construction and array access copy the array;
 * the material-count map is copied and exposed as an unmodifiable view.
 * <p>
 * Plans are created by image workers and consumed by the server-thread painting
 * service. Counts describe selected pixels rather than changes to terrain that
 * may already match the selected material.
 *
 * @param width output columns
 * @param height output rows
 * @param blocks row-major block-data strings or null for transparent pixels
 * @param materials selected material names and their pixel counts
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public record PaintPlan(int width, int height, String[] blocks, Map<String, Integer> materials) {
}
