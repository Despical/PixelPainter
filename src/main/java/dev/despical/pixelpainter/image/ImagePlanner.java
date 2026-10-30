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

    /**
     * Produces a copied row-major block plan and material counts on an image worker.
     *
     * @param source decoded source image
     * @param height requested output block rows
     * @param maxBlocks maximum output area
     * @param palette previously resolved, non-empty palette
     * @param transparent whether low-alpha pixels preserve terrain
     * @param alphaThreshold skip pixels below this cutoff; zero-alpha pixels are always skipped
     * @param dither whether to diffuse color error into neighboring pixels
     * @param background packed sRGB compositing background when transparency is disabled
     * @return prepared materials without world mutation
     * @throws InterruptedException if the worker is interrupted between rows
     * @throws IllegalArgumentException if output dimensions exceed the budget
     */
    public static PaintPlan plan(BufferedImage source, int height, int maxBlocks, BlockPalette palette,
        boolean transparent, int alphaThreshold, boolean dither, int background) throws InterruptedException {

        int width = width(source.getWidth(), source.getHeight(), height, maxBlocks);
        var scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = scaled.createGraphics();

        try {
            if (!transparent) {
                graphics.setColor(new Color(background));
                graphics.fillRect(0, 0, width, height);
                graphics.setComposite(AlphaComposite.SrcOver);
            }

            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        String[] blocks = new String[width * height];
        Map<String, Integer> materials = new LinkedHashMap<>();
        Map<Integer, BlockPalette.Entry> cache = new HashMap<>();

        double[][] errors = new double[height + 1][(width + 2) * 3];

        for (int y = 0; y < height; y++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            for (int x = 0; x < width; x++) {
                int argb = scaled.getRGB(x, y);

                if (transparent && ((argb >>> 24) == 0 || (argb >>> 24) < alphaThreshold)) {
                    continue;
                }

                int offset = (x + 1) * 3;
                int r = channel(argb >> 16, errors[y][offset]);
                int g = channel(argb >> 8, errors[y][offset + 1]);
                int b = channel(argb, errors[y][offset + 2]);
                int rgb = (r << 16) | (g << 8) | b;
                BlockPalette.Entry entry = cache.computeIfAbsent(rgb, palette::nearest);

                blocks[y * width + x] = entry.blockData();
                materials.merge(entry.material(), 1, Integer::sum);

                if (dither) {
                    int[] channels = {r, g, b};

                    for (int c = 0; c < 3; c++) {
                        double error = channels[c] - ((entry.rgb() >> (16 - c * 8)) & 255);

                        errors[y][offset + 3 + c] += error * 7 / 16;
                        errors[y + 1][offset - 3 + c] += error * 3 / 16;
                        errors[y + 1][offset + c] += error * 5 / 16;
                        errors[y + 1][offset + 3 + c] += error / 16;
                    }
                }
            }
        }

        return new PaintPlan(width, height, blocks, materials);
    }

    private static int channel(int value, double error) {
        return Math.clamp((int) Math.round((value & 255) + error), 0, 255);
    }
}
