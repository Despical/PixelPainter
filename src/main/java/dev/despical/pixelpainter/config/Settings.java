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
}
