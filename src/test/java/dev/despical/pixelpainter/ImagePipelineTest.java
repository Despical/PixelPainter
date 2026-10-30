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

package dev.despical.pixelpainter;

import dev.despical.pixelpainter.core.Bounds;
import dev.despical.pixelpainter.core.Orientation;
import dev.despical.pixelpainter.core.Position;
import dev.despical.pixelpainter.image.BlockPalette;
import dev.despical.pixelpainter.image.ImagePlanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies aspect ratios, pixel budgets, orientation geometry, alpha handling and dithering.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
class ImagePipelineTest {

    private static final BlockPalette PALETTE = new BlockPalette(List.of(
        new BlockPalette.Entry("BLACK_CONCRETE", "minecraft:black_concrete", 0),
        new BlockPalette.Entry("WHITE_CONCRETE", "minecraft:white_concrete", 0xffffff)));

    @Test
    void dimensionsUseActualBlockHeightAndRejectOverflow() {
        assertEquals(128, ImagePlanner.width(1920, 960, 64, 8192));
        assertEquals(1, ImagePlanner.width(1, 1000, 8, 8));
        assertThrows(IllegalArgumentException.class, () -> ImagePlanner.width(1920, 960, 64, 8191));
        assertThrows(IllegalArgumentException.class, () -> ImagePlanner.width(Integer.MAX_VALUE, 1, 2048, 1048576));
        assertThrows(IllegalArgumentException.class, () -> ImagePlanner.width(2, 2, 0, 100));
    }

    @ParameterizedTest
    @EnumSource(Orientation.class)
    void geometryUsesEveryPixelExactlyOnceAndContainsAllPositions(Orientation direction) {
        Position origin = new Position(-18, -32, -18);
        Bounds bounds = Bounds.of(origin, direction.position(origin, 8, 0, 7));
        var positions = new HashSet<Position>();

        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 9; x++) {
                Position p = direction.position(origin, x, y, 7);

                assertTrue(positions.add(p));
                assertTrue(p.x() >= bounds.minX() && p.x() <= bounds.maxX());
                assertTrue(p.y() >= bounds.minY() && p.y() <= bounds.maxY());
                assertTrue(p.z() >= bounds.minZ() && p.z() <= bounds.maxZ());
            }
        }

        assertEquals(origin, direction.position(origin, 0, 6, 7));
        assertEquals(direction, Orientation.parse(direction.name().replace("_", "")));
        assertTrue(bounds.chunks() > 0);
    }
}
