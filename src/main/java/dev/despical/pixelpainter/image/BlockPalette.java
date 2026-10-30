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

    /**
     * Copies a non-empty list of prepared palette entries.
     *
     * @param entries matching candidates
     * @throws IllegalArgumentException if no candidates are available
     */
    public BlockPalette(List<Entry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Palette has no supported blocks.");
        }

        this.entries = List.copyOf(entries);
    }

    /**
     * Loads and filters material colors using the current server material registry.
     *
     * @param file palette YAML file
     * @param included exact allowlist; empty means all available entries
     * @param excluded exact material denylist
     * @return non-empty validated palette
     * @throws Exception if loading or palette validation fails
     */
    public static BlockPalette load(Path file, Set<String> included, Set<String> excluded) throws Exception {
        var yaml = new YamlConfiguration();

        yaml.load(file.toFile());
        var section = yaml.getConfigurationSection("blocks");

        if (section == null) {
            throw new IllegalArgumentException("palette.yml requires a blocks section.");
        }

        List<Entry> entries = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);

            if (material == null) {
                continue; // A newer palette can be shared with older supported servers.
            }

            if (DECAYING.containsKey(material.getKey().toString()) || isGlass(material)) {
                continue; // Decays on random ticks or is glass; neither is used for paintings.
            }

            if (!material.isBlock() || !material.isSolid() || material.hasGravity()) {
                throw new IllegalArgumentException("Unsafe palette block: " + key);
            }

            BlockData data = Bukkit.createBlockData(material);

            if (!supports(material, data) || data.createBlockState() instanceof TileState) {
                throw new IllegalArgumentException("Unsafe palette block: " + key);
            }

            if (data instanceof Leaves leaves) {
                leaves.setPersistent(true);
            }

            String hex = section.getString(key);

            if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("Expected #RRGGBB for " + key);
            }

            if ((!included.isEmpty() && !included.contains(key)) || excluded.contains(key)) {
                continue;
            }

            entries.add(new Entry(key, data.getAsString(), Integer.parseInt(hex.substring(1), 16)));
        }

        Set<String> supported = entries.stream().map(Entry::material).collect(Collectors.toSet());

        if (!supported.containsAll(included)) {
            throw new IllegalArgumentException("Included materials must be available in the active palette: " + included);
        }

        return new BlockPalette(entries);
    }

    private static boolean isGlass(Material material) {
        String name = material.name();

        return name.equals("GLASS") || name.endsWith("_GLASS");
    }

    private static boolean supports(Material material, BlockData data) {
        if (material.isOccluding() || data instanceof Leaves) {
            return true;
        }

        return switch (material) {
            case GLOWSTONE, SEA_LANTERN, BLUE_ICE, PACKED_ICE -> true;
            default -> false;
        };
    }

    /**
     * Selects a named material family within the configured global filters.
     *
     * @param name all, concrete, wool or terracotta, ignoring case
     * @return this palette for all, or a new family palette
     * @throws IllegalArgumentException if the family is unknown or empty
     */
    public BlockPalette subset(String name) {
        String suffix = switch (name.toLowerCase(Locale.ROOT)) {
            case "all" -> "";
            case "concrete" -> "_CONCRETE";
            case "wool" -> "_WOOL";
            case "terracotta" -> "_TERRACOTTA";
            default -> throw new PaintException(PaintError.PALETTE);
        };

        return suffix.isEmpty() ? this : new BlockPalette(entries.stream().filter(e -> e.material.endsWith(suffix)).toList());
    }

    /**
     * Finds the closest representative color by squared Oklab distance.
     *
     * @param rgb packed sRGB sample
     * @return closest entry, with palette order breaking equal-distance ties
     */
    public Entry nearest(int rgb) {
        double[] lab = ColorMath.lab(rgb);
        Entry best = entries.getFirst();
        double distance = Double.MAX_VALUE;

        for (Entry entry : entries) {
            double candidate = ColorMath.distance(lab, entry.lab);

            if (candidate < distance) {
                best = entry;
                distance = candidate;
            }
        }

        return best;
    }

    /**
     * Returns the unmodifiable candidate list.
     *
     * @return entries; cached coordinate arrays must be treated as read-only
     */
    public List<Entry> entries() {
        return entries;
    }
}
