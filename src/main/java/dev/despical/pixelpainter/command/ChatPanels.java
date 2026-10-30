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
import dev.despical.pixelpainter.core.PaintError;
import dev.despical.pixelpainter.core.PaintException;
import dev.despical.pixelpainter.image.PaintPlan;
import dev.despical.pixelpainter.storage.PaintRecord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Renders help, source lists, material estimates and undo history as chat panels.
 * <p>
 * Panels share the {@link ChatLayout} separator, centered headings, bullet rows
 * and page controls. Suggestions let players review a command before
 * it changes terrain; run-command controls only navigate pages. Hover descriptions
 * are wrapped for readability and dynamic values are rendered through
 * {@link Messages}.
 * <p>
 * Call on the server thread. Rendering reads current messages and permissions but
 * performs no file I/O, terrain mutation or journal ownership changes.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class ChatPanels {

    private static final int PAGE_SIZE = 8;
    private static final int HOVER_COLUMNS = 38;

    private final Supplier<Messages> messages;
    private final String version;

    /**
     * Creates renderers that read the currently active message set on every call.
     *
     * @param messages supplier of the active messages, so reloads apply immediately
     * @param version plugin version shown in the help subtitle
     */
    public ChatPanels(Supplier<Messages> messages, String version) {
        this.messages = messages;
        this.version = version;
    }

    /**
     * Renders permission-filtered commands, grouped, with suggestions and hover explanations.
     *
     * @param sender command audience
     * @param actions catalog of available commands
     */
    public void help(CommandSender sender, List<CommandAction> actions) {
        Messages messages = this.messages.get();

        ChatLayout.header(sender, messages, "commands.help-title");
        ChatLayout.centered(sender, messages, messages.get("commands.help-subtitle", "version", version), false);
        messages.reply(sender, Component.empty());

        boolean first = true;

        for (CommandAction.Group group : CommandAction.Group.values()) {
            List<CommandAction> visible = actions.stream().filter(a -> a.group() == group)
                .filter(a -> sender.hasPermission("pp." + a.permission())).toList();

            if (visible.isEmpty()) {
                continue;
            }

            if (!first) {
                messages.reply(sender, Component.empty());
            }

            first = false;
            messages.reply(sender, messages.get("commands.help-section", "section",
                messages.text("commands.help-group-" + group.key())));

            for (CommandAction action : visible) {
                String syntax = "/pp " + action.syntax();
                Component command = messages.get("commands.help-command", "command", syntax);

                messages.reply(sender, messages.get("commands.help-row", "command", command,
                        "description", messages.text("commands.help-desc-" + action.name()))
                    .clickEvent(ClickEvent.suggestCommand("/pp " + action.suggestion()))
                    .hoverEvent(HoverEvent.showText(messages.get("commands.help-hover", "command", syntax,
                        "description", ChatLayout.wrap(messages.text("commands.help-detail-" + action.name()), HOVER_COLUMNS)))));
            }
        }

        messages.reply(sender, Component.empty());
    }
}
