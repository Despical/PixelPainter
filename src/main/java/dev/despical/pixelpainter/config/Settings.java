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

package dev.despical.pixelpainter.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Captures validated operational limits and presentation settings for one load.
 * <p>
 * The loader bounds numeric inputs, checks value types and normalizes material
 * and host names. Collections returned by the loader are immutable. Loading a
 * legacy material allowlist creates a one-time sibling backup file.
 * <p>
 * This value contains no live Bukkit state. Preparations and world jobs retain
 * their captured settings when a reload publishes another instance.
 *
 * @param maxHeight maximum output block rows
 * @param maxBlocks maximum output pixel area
 * @param maxSourcePixels maximum input image area before decode
 * @param maxFileBytes maximum source or download byte count
 * @param blocksPerTick server-thread pixel or block processing budget
 * @param maxChunks maximum chunks reserved by one operation
 * @param maxHistory maximum retained undo journals
 * @param maxStoredBlocks maximum total journal changes
 * @param alphaThreshold alpha cutoff for preserving terrain
 * @param background packed sRGB background for opaque compositing
 * @param progress whether new jobs display a bossbar
 * @param included material allowlist; empty means all available entries
 * @param excluded material denylist
 * @param downloadHosts exact trusted HTTPS host names
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public record Settings(int maxHeight, int maxBlocks, int maxSourcePixels, int maxFileBytes, int blocksPerTick,
    int maxChunks, int maxHistory, int maxStoredBlocks, int alphaThreshold, int background,
    boolean progress, Set<String> included, Set<String> excluded, Set<String> downloadHosts) {

    /**
     * Loads bounded settings and translates the legacy material allowlist if present.
     *
     * @param path configuration file
     * @return validated settings with copied collections
     * @throws Exception if YAML, value types, ranges or legacy backup creation fail
     */
    public static Settings load(Path path) throws Exception {
        var yaml = new YamlConfiguration();
        yaml.load(path.toFile());

        boolean legacy = yaml.contains("Custom-Materials-Enabled");
        if (legacy && !Files.exists(path.resolveSibling("config-v1.backup.yml"))) {
            Files.copy(path, path.resolveSibling("config-v1.backup.yml"));
        }

        Set<String> included = names(yaml, "palette.include", true);
        if (legacy && yaml.getBoolean("Custom-Materials-Enabled") && included.isEmpty()) {
            included = names(yaml, "Custom-Materials", true);
        }

        String hex = yaml.getString("images.background", "#FFFFFF");
        if (!hex.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("images.background must be #RRGGBB.");
        }

        if (yaml.contains("progress.enabled") && !yaml.isBoolean("progress.enabled")) {
            throw new IllegalArgumentException("progress.enabled must be true or false.");
        }

        return new Settings(number(yaml, "limits.max-height", 512, 1, 2048),
            number(yaml, "limits.max-blocks", 131072, 1, 1048576),
            number(yaml, "limits.max-source-pixels", 16777216, 1, 67108864),
            number(yaml, "limits.max-file-mb", 16, 1, 64) * 1024 * 1024,
            number(yaml, "performance.blocks-per-tick", 500, 1, 5000),
            number(yaml, "limits.max-chunks", 128, 1, 512),
            number(yaml, "undo.max-history", 32, 1, 256),
            number(yaml, "undo.max-stored-blocks", 524288, 1, 4194304),
            number(yaml, "images.alpha-threshold", 128, 0, 255), Integer.parseInt(hex.substring(1), 16),
            yaml.getBoolean("progress.enabled", true), Set.copyOf(included), Set.copyOf(names(yaml, "palette.exclude", true)),
            Set.copyOf(names(yaml, "downloads.allowed-hosts", false)));
    }
}
