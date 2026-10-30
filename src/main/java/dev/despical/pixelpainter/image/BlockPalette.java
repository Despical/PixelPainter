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
import dev.despical.pixelpainter.core.ColorMath;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves configured material colors and performs perceptual nearest-color matching.
 * <p>
 * Loading runs on the server thread because it resolves Bukkit materials and
 * creates canonical block-data strings. Unknown materials are skipped for
 * cross-version palettes; unsafe blocks, invalid colors and unavailable explicit
 * inclusions reject the candidate palette.
 * <p>
 * Matching uses cached Oklab coordinates without world access. The entry list is
 * copied. Treat each entry's coordinate array as read-only when sharing a palette
 * with image workers; subsets reuse those entries.
 * <p>
 * Supported transparent cubes include stable ice and leaves; glass is never used. Leaves use
 * persistent block data to prevent natural decay. Gravity blocks and block
 * entities are rejected before a candidate can reach the painting service.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class BlockPalette {

    /**
     * Associates a material and canonical block data with its representative color.
     *
     * @param material Bukkit material name
     * @param blockData canonical placement data
     * @param rgb packed representative sRGB color
     * @param lab cached Oklab coordinates, treated as read-only
     * @author Despical
     * <p>
     * Created at 15.10.2026
     */
    public record Entry(String material, String blockData, int rgb, double[] lab) {

        /**
         * Creates an entry and computes its cached perceptual coordinates.
         *
         * @param material Bukkit material name
         * @param blockData canonical placement data
         * @param rgb representative sRGB color
         */
        public Entry(String material, String blockData, int rgb) {
            this(material, blockData, rgb, ColorMath.lab(rgb));
        }
    }

    /**
     * Maps blocks that change themselves after placement to the blocks they can become.
     * Undo accepts a decayed block as the painted one; new paintings never use them.
     */
    public static final Map<String, Set<String>> DECAYING = decaying();

    private final List<Entry> entries;

    private static Map<String, Set<String>> decaying() {
        Map<String, Set<String>> map = new HashMap<>();

        map.put("minecraft:mycelium", Set.of("minecraft:dirt"));
        map.put("minecraft:crimson_nylium", Set.of("minecraft:netherrack"));
        map.put("minecraft:warped_nylium", Set.of("minecraft:netherrack"));

        String[][] copper = {
            {"copper_block", "exposed_copper", "weathered_copper", "oxidized_copper"},
            {"cut_copper", "exposed_cut_copper", "weathered_cut_copper", "oxidized_cut_copper"},
            {"chiseled_copper", "exposed_chiseled_copper", "weathered_chiseled_copper", "oxidized_chiseled_copper"}};

        for (String[] stages : copper) {
            for (int i = 0; i < stages.length - 1; i++) {
                Set<String> later = new HashSet<>();

                for (int j = i + 1; j < stages.length; j++) {
                    later.add("minecraft:" + stages[j]);
                }

                map.put("minecraft:" + stages[i], Set.copyOf(later));
            }
        }

        return Map.copyOf(map);
    }
}
