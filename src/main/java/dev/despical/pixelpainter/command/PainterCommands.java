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

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Private;
import co.aikar.commands.annotation.Subcommand;

import dev.despical.pixelpainter.PixelPainter;
import dev.despical.pixelpainter.command.CommandAction.Group;
import dev.despical.pixelpainter.core.Orientation;
import dev.despical.pixelpainter.core.PaintError;
import dev.despical.pixelpainter.core.PaintException;
import dev.despical.pixelpainter.core.Position;
import dev.despical.pixelpainter.image.BlockPalette;
import dev.despical.pixelpainter.config.Settings;
import dev.despical.pixelpainter.image.ImagePlanner;
import dev.despical.pixelpainter.image.PaintPlan;
import dev.despical.pixelpainter.storage.PaintRecord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Receives ACF invocations and dispatches validated painting requests.
 * <p>
 * Each action passes through shared permission checks and configured error panels.
 * Actions are registered once as handlers, so a new command needs a catalog entry
 * and a handler without touching dispatch. The dispatcher parses arguments,
 * captures request settings and delegates to file/image workers or the
 * server-thread painting service.
 * <p>
 * Per-player tokens suppress stale preparation results after cancellation or
 * disconnect. File operations retain reservations until completion because
 * cancellation cannot roll back a source mutation. Worker callbacks return
 * through the plugin scheduler before accessing player or world state.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
@CommandAlias("pp|pixelpainter")
public final class PainterCommands extends BaseCommand {

    /**
     * Defines the immutable, ordered command catalog shared by dispatch and help.
     */
    public static final List<CommandAction> ACTIONS = List.of(
        new CommandAction("create", "create", "create <image> <direction> <height> [transparent] [palette] [dither]", "create ", Group.PAINT),
        new CommandAction("preview", "howmanyblocks", "preview <image> <height> [palette] [dither] [transparent]", "preview ", Group.PAINT),
        new CommandAction("undo", "undo", "undo [id or image]", "undo ", Group.PAINT),
        new CommandAction("cancel", "cancel", "cancel", "cancel", Group.PAINT),
        new CommandAction("list", "list", "list [page]", "list", Group.IMAGES),
        new CommandAction("specs", "specs", "specs <image>", "specs ", Group.IMAGES),
        new CommandAction("history", "undo", "history [page]", "history", Group.IMAGES),
        new CommandAction("materials", "materials", "materials [palette] [page]", "materials", Group.IMAGES),
        new CommandAction("download", "download", "download <image> <https-url>", "download ", Group.MANAGE),
        new CommandAction("delete", "delete", "delete <image>", "delete ", Group.MANAGE),
        new CommandAction("reload", "reload", "reload", "reload", Group.MANAGE),
        new CommandAction("help", "help", "help [page]", "help", Group.REFERENCE));

    /**
     * Runs one catalog action after permissions have been verified.
     */
    @FunctionalInterface
    private interface Handler {

        void run(CommandSender sender, String[] args) throws Exception;
    }

    private static final long PREPARING_DELAY_TICKS = 60;
    private static final long CONFIRMATION_TICKS = 20L * 60;

    private final PixelPainter plugin;
    private final ChatPanels panels;
    private final Map<String, Handler> handlers = new LinkedHashMap<>();
    private final Map<UUID, UUID> pending = new HashMap<>();
    private final Set<UUID> fileOperations = new HashSet<>();
    private final Map<UUID, Runnable> confirmations = new HashMap<>();

    /**
     * Creates command dispatch and panel renderers for an enabled plugin.
     *
     * @param plugin owning plugin
     */
    public PainterCommands(PixelPainter plugin) {
        this.plugin = plugin;
        panels = new ChatPanels(plugin::messages, plugin.getPluginMeta().getVersion());

        handlers.put("create", (sender, args) -> handleCreate(player(sender), args));
        handlers.put("preview", (sender, args) -> handlePreview(player(sender), args));
        handlers.put("undo", this::handleUndo);
        handlers.put("cancel", this::handleCancel);
        handlers.put("list", this::handleList);
        handlers.put("specs", this::handleSpecs);
        handlers.put("history", (sender, args) -> panels.history(sender, plugin.painter().history(player(sender)), page(args)));
        handlers.put("materials", this::handleMaterials);
        handlers.put("download", this::handleDownload);
        handlers.put("delete", this::handleDelete);
        handlers.put("reload", this::handleReload);
        handlers.put("help", (sender, args) -> {
            page(args);
            panels.help(sender, ACTIONS);
        });
    }

    /**
     * Registers completions from cached names, directions and accessible history.
     *
     * @param manager ACF manager owning these commands
     */
    public void registerCompletions(BukkitCommandManager manager) {
        var completions = manager.getCommandCompletions();

        completions.registerCompletion("images", _ -> plugin.imageNames());
        completions.registerCompletion("directions", _ -> Arrays.stream(Orientation.values())
            .map(o -> o.name().toLowerCase(Locale.ROOT)).toList());
        completions.registerCompletion("heights", _ -> List.of("32", "64", "128", "256"));
        completions.registerCompletion("none", _ -> List.of());
        completions.registerCompletion("booleans", _ -> List.of("true", "false"));
        completions.registerCompletion("palettes", _ -> List.of("all", "concrete", "wool", "terracotta"));
        completions.registerCompletion("history", context -> context.getSender() instanceof Player player
            ? plugin.painter().history(player).stream().map(PaintRecord::id).toList() : List.of());
    }

    /**
     * Opens help for empty input and renders the unknown-command panel otherwise.
     *
     * @param sender command audience
     * @param args arguments after the root command
     */
    @Default
    @CatchUnknown
    public void root(CommandSender sender, String[] args) {
        if (args.length == 0) {
            execute(sender, "help", args);
        } else {
            plugin.messages().send(sender, PaintError.UNKNOWN_COMMAND.key());
        }
    }

    /**
     * Routes creation requests through common validation and permissions.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("create")
    @CommandCompletion("@images @directions @heights @booleans @palettes @booleans @none")
    public void create(CommandSender sender, String[] args) {
        execute(sender, "create", args);
    }

    /**
     * Runs the world-border override offered after a create request crossed the border.
     *
     * @param sender command audience
     */
    @Subcommand("confirm")
    @Private
    public void confirm(CommandSender sender) {
        try {
            Player player = player(sender);

            if (!player.hasPermission("pp.create")) {
                throw new PaintException(PaintError.PERMISSION);
            }

            Runnable confirmation = confirmations.remove(player.getUniqueId());

            if (confirmation == null) {
                plugin.messages().send(player, "commands.confirm-expired");

                return;
            }

            confirmation.run();
        } catch (Exception exception) {
            error(sender, exception);
        }
    }

    /**
     * Routes estimates and the legacy block-count alias.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("preview|howmanyblocks")
    @CommandCompletion("@images @heights @palettes @booleans @booleans @none")
    public void preview(CommandSender sender, String[] args) {
        execute(sender, "preview", args);
    }

    /**
     * Routes owner-aware journal restoration.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("undo")
    @CommandCompletion("@history @none")
    public void undo(CommandSender sender, String[] args) {
        execute(sender, "undo", args);
    }

    /**
     * Routes preparation or active-world-job cancellation.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("cancel")
    @CommandCompletion("@none")
    public void cancel(CommandSender sender, String[] args) {
        execute(sender, "cancel", args);
    }

    /**
     * Routes asynchronous source-file listings.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("list")
    @CommandCompletion("@none")
    public void list(CommandSender sender, String[] args) {
        execute(sender, "list", args);
    }

    /**
     * Routes accessible undo-history listings.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("history")
    @CommandCompletion("@none")
    public void history(CommandSender sender, String[] args) {
        execute(sender, "history", args);
    }

    /**
     * Routes palette-family listings.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("materials")
    @CommandCompletion("@palettes @none")
    public void materials(CommandSender sender, String[] args) {
        execute(sender, "materials", args);
    }

    /**
     * Routes source-dimension requests and their information alias.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("specs|info")
    @CommandCompletion("@images @none")
    public void specs(CommandSender sender, String[] args) {
        execute(sender, "specs", args);
    }

    /**
     * Routes bounded source-image downloads.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("download")
    @CommandCompletion("@none")
    public void download(CommandSender sender, String[] args) {
        execute(sender, "download", args);
    }

    /**
     * Routes source deletion without deleting paintings or journals.
     *
     * @param sender command audience
     * @param args positional arguments after the subcommand
     */
    @Subcommand("delete")
    @CommandCompletion("@images @none")
    public void delete(CommandSender sender, String[] args) {
        execute(sender, "delete", args);
    }
}
