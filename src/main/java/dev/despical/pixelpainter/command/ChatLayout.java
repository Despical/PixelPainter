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

import dev.despical.pixelpainter.config.Messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;

/**
 * Provides shared chat layout primitives for panel headings, hints and navigation.
 * <p>
 * Text is measured with Minecraft bitmap-font advances so centering depends on the
 * rendered width instead of the character count. Formatting and interactions of the
 * wrapped components are retained; only spacing is added. Console senders receive the
 * plain text path of {@link Messages#reply}.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class ChatLayout {

    private static final String ASCII_ADVANCES =
        "42466662444626266666666666225656766666666466666666666666666464663666665662653666666646666664247";
    private static final int CHAT_WIDTH = 320;
    private static final int SPACE_WIDTH = 8;
    private static final TextColor DISABLED = TextColor.color(0x586174);
    private static final TextColor ENABLED = TextColor.color(0x7ABFCC);

    private ChatLayout() {
    }

    /**
     * Sends the separator, an empty line and the centered bold title to players.
     *
     * @param sender command audience
     * @param messages active messages
     * @param title title message key
     * @param replacements alternating placeholder names and values
     */
    public static void header(CommandSender sender, Messages messages, String title, Object... replacements) {
        if (sender instanceof Player) {
            centered(sender, messages, messages.get("chat.separator"), false);
            messages.reply(sender, Component.empty());
        }

        centered(sender, messages, messages.get(title, replacements), true);
    }
}
