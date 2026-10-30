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

    /**
     * Renders source files as creation-command suggestions.
     *
     * @param sender command audience
     * @param names source filenames
     * @param page one-based page
     */
    public void images(CommandSender sender, List<String> names, int page) {
        Messages messages = this.messages.get();

        paged(sender, "images", "/pp list ", names, page, name -> row(messages.get("commands.images-row", "image", name),
            ClickEvent.suggestCommand("/pp create " + name + " north 64"), messages.get("commands.images-hover")));
    }

    /**
     * Renders previously authorized records with undo-command suggestions.
     *
     * @param sender command audience
     * @param records accessible records supplied by the service
     * @param page one-based page
     */
    public void history(CommandSender sender, List<PaintRecord> records, int page) {
        Messages messages = this.messages.get();

        paged(sender, "history", "/pp history ", records, page, record -> row(messages.get("commands.history-row",
                "image", record.image(), "id", record.id(), "blocks", record.changes().size()),
            ClickEvent.suggestCommand("/pp undo " + record.id()), messages.get("commands.history-hover",
                "id", record.id(), "world", record.world(), "origin", record.bounds().minX() + ", "
                    + record.bounds().minY() + ", " + record.bounds().minZ())));
    }

    /**
     * Renders a palette's material names with navigation.
     *
     * @param sender command audience
     * @param palette selected family used by navigation commands
     * @param names supported material names
     * @param page one-based page
     */
    public void materials(CommandSender sender, String palette, List<String> names, int page) {
        Messages messages = this.messages.get();

        paged(sender, "materials", "/pp materials " + palette + " ", names, page,
            name -> prefixed(messages.get("commands.materials-row", "material", name)));
    }

    /**
     * Shows dimensions, transparency counts and the eight most-used materials.
     *
     * @param sender command audience
     * @param image source name
     * @param plan prepared estimate; no world preview is created
     */
    public void plan(CommandSender sender, String image, PaintPlan plan) {
        Messages messages = this.messages.get();

        ChatLayout.header(sender, messages, "commands.plan-title");
        ChatLayout.centered(sender, messages, messages.get("commands.plan-description"), false);
        messages.reply(sender, Component.empty());
        messages.reply(sender, prefixed(messages.get("commands.plan-row", "label", messages.get("commands.plan-size"),
            "value", messages.get("commands.plan-size-value", "width", plan.width(), "height", plan.height()))));
        messages.reply(sender, prefixed(messages.get("commands.plan-row", "label", messages.get("commands.plan-painted"),
            "value", plan.painted())));
        messages.reply(sender, prefixed(messages.get("commands.plan-row", "label", messages.get("commands.plan-skipped"),
            "value", plan.size() - plan.painted())));
        messages.reply(sender, Component.empty());

        plan.materials().entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
            .limit(PAGE_SIZE)
            .forEach(entry -> messages.reply(sender, prefixed(messages.get("commands.plan-material-row",
                "material", entry.getKey(), "count", entry.getValue()))));

        if (plan.materials().size() > PAGE_SIZE) {
            messages.reply(sender, prefixed(messages.get("commands.plan-more", "count", plan.materials().size() - PAGE_SIZE)));
        }

        messages.reply(sender, Component.empty());
    }

    private Component prefixed(Component content) {
        return messages.get().get("chat.table-row-prefix").append(content);
    }

    private Component row(Component content, ClickEvent click, Component hover) {
        return prefixed(content).clickEvent(click).hoverEvent(HoverEvent.showText(hover));
    }

    private <T> void paged(CommandSender sender, String key, String command, List<T> entries, int page,
        Function<T, Component> render) {

        Messages messages = this.messages.get();
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        if (page < 1 || page > pages) {
            messages.send(sender, PaintError.PAGE.key(), "pages", pages);

            return;
        }

        ChatLayout.header(sender, messages, "commands." + key + "-title");
        ChatLayout.centered(sender, messages, messages.get("commands." + key + "-description"), false);
        messages.reply(sender, Component.empty());

        if (entries.isEmpty()) {
            messages.reply(sender, messages.get("commands.empty"));
        }

        entries.stream().skip((long) (page - 1) * PAGE_SIZE).limit(PAGE_SIZE)
            .forEach(entry -> messages.reply(sender, render.apply(entry)));

        ChatLayout.navigation(sender, messages, command, page, pages);
    }
}
