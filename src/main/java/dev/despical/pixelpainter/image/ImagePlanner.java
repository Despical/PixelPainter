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

import dev.despical.pixelpainter.core.PaintError;
import dev.despical.pixelpainter.core.PaintException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resizes decoded images and selects block materials independently of Bukkit worlds.
 * <p>
 * Output height is measured in actual block rows, and width preserves aspect
 * ratio within a checked pixel budget. Transparent pixels may preserve terrain;
 * opaque rendering composites the source over the configured background.
 * Optional Floyd-Steinberg diffusion improves detail with a limited palette.
 * <p>
 * Planning allocates task-local buffers and checks interruption at each image row.
 * The palette already contains server-resolved block-data strings.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class ImagePlanner {

    private ImagePlanner() {
    }

    /**
     * Calculates aspect-preserving width before allocating output buffers.
     *
     * @param sourceWidth positive source width
     * @param sourceHeight positive source height
     * @param height requested positive output rows
     * @param maxBlocks permitted output area
     * @return rounded width, with a minimum of one
     * @throws IllegalArgumentException if dimensions are invalid or exceed the budget
     */
    public static int width(int sourceWidth, int sourceHeight, int height, int maxBlocks) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || height <= 0) {
            throw new PaintException(PaintError.SIZE);
        }

        long width = Math.max(1L, Math.round(sourceWidth * (double) height / sourceHeight));

        if (width > Integer.MAX_VALUE || width * height > maxBlocks) {
            throw new PaintException(PaintError.SIZE);
        }

        return (int) width;
    }
}
