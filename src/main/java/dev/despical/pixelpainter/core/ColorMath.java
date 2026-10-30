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
 * Converts sRGB samples into Oklab and measures perceptual color distance.
 * <p>
 * Conversion linearizes the three color channels before transforming them.
 * No Bukkit state or mutable shared cache is used, so matching may run on
 * background image workers.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class ColorMath {

    private ColorMath() {
    }

    /**
     * Converts an RGB sample into Oklab coordinates.
     *
     * @param rgb packed sRGB sample; the alpha byte is ignored
     * @return a new three-element array containing lightness, a and b
     */
    public static double[] lab(int rgb) {
        double r = linear((rgb >> 16) & 255), g = linear((rgb >> 8) & 255), b = linear(rgb & 255);
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);

        return new double[] {0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s};
    }

    private static double linear(int channel) {
        double value = channel / 255.0;

        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
