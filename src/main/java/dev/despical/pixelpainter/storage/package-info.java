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

/**
 * Versioned undo journals and checked restart recovery.
 * <p>
 * Records copy original and intended block properties with owner/world UUIDs and
 * reserved bounds. Binary files are validated for IDs, size, bounds and trailing
 * content. Saves flush a temporary file before promotion; unfinished temporary
 * files are ignored, while corrupt complete journals fail startup and remain on
 * disk. Worker I/O does not read live Bukkit world objects.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
package dev.despical.pixelpainter.storage;
