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

package dev.despical.pixelpainter.command;

import java.util.Locale;

/**
 * Describes a command for permission filtering, grouped help and suggestions.
 *
 * @param name canonical action and help-message suffix
 * @param permission permission suffix below {@code pp}
 * @param syntax displayed syntax without the root command
 * @param suggestion suggested text without the root command
 * @param group help section the command belongs to
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public record CommandAction(String name, String permission, String syntax, String suggestion, Group group) {

    /**
     * Help sections, declared in presentation order.
     */
    public enum Group {

        /** Commands that create, estimate or revert paintings. */
        PAINT,
        /** Commands that browse source images and history. */
        IMAGES,
        /** Commands that change files or configuration. */
        MANAGE,
        /** Reference commands such as help. */
        REFERENCE;

        /**
         * Returns the suffix of the section's heading message.
         *
         * @return lowercase key
         */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
