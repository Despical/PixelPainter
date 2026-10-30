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

    @Test
    void northWallReadsImageTopToBottomWithoutMirroring() {
        assertEquals(new Position(12, 23, 30), Orientation.NORTH.position(new Position(10, 20, 30), 2, 0, 4));
        assertEquals(new Position(10, 20, 30), Orientation.NORTH.position(new Position(10, 20, 30), 0, 3, 4));
        assertEquals(Orientation.NORTH, Orientation.parse("Up_North"));
        assertEquals(4, new Bounds(-17, 0, -17, -16, 0, -16).chunks());
    }

    @Test
    void alphaMasksPreserveTerrainAndBackgroundIsExplicit() throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);

        image.setRGB(0, 0, 0xff000000);
        image.setRGB(1, 0, 0xffffffff);
        image.setRGB(0, 1, 0x7fffffff);
        image.setRGB(1, 1, 0x00ffffff);
        var transparent = ImagePlanner.plan(image, 2, 4, PALETTE, true, 128, false, 0xffffff);

        assertEquals("minecraft:black_concrete", transparent.block(0));
        assertEquals("minecraft:white_concrete", transparent.block(1));
        assertNull(transparent.block(2));
        assertNull(transparent.block(3));
        assertEquals(2, transparent.painted());
        var opaque = ImagePlanner.plan(image, 2, 4, PALETTE, false, 128, false, 0xffffff);

        assertEquals(4, opaque.painted());
        assertEquals("minecraft:white_concrete", opaque.block(3));
        var zeroThreshold = ImagePlanner.plan(image, 2, 4, PALETTE, true, 0, false, 0xffffff);

        assertNull(zeroThreshold.block(3));
    }

    @Test
    void diffusionAddsTonalDetailWithoutLeakingAcrossTransparentHoles() throws Exception {
        var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                image.setRGB(x, y, x == 16 ? 0 : 0xff808080);
            }
        }

        var plan = ImagePlanner.plan(image, 32, 1024, PALETTE, true, 128, true, 0xffffff);

        assertEquals(992, plan.painted());
        assertEquals(2, plan.materials().size());

        for (int y = 0; y < 32; y++) {
            assertNull(plan.block(y * 32 + 16));
        }

        String[] exposed = plan.blocks();

        exposed[0] = "corrupted";
        assertNotEquals("corrupted", plan.block(0));
        assertThrows(UnsupportedOperationException.class, () -> plan.materials().clear());
    }
}
