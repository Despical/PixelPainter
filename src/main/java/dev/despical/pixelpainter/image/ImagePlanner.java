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
}
