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
}
