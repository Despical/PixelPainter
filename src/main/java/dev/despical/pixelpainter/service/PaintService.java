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

package dev.despical.pixelpainter.service;

import dev.despical.pixelpainter.core.PaintError;
import dev.despical.pixelpainter.core.PaintException;
import dev.despical.pixelpainter.PixelPainter;
import dev.despical.pixelpainter.config.Settings;
import dev.despical.pixelpainter.core.Bounds;
import dev.despical.pixelpainter.core.Orientation;
import dev.despical.pixelpainter.core.Position;
import dev.despical.pixelpainter.image.BlockPalette;
import dev.despical.pixelpainter.image.PaintPlan;
import dev.despical.pixelpainter.storage.PaintRecord;
import dev.despical.pixelpainter.storage.UndoStore;

import net.kyori.adventure.bossbar.BossBar;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Coordinates one bounded world operation and its persistent undo history.
 * <p>
 * All public operations and the job ticker run on the server thread. Creation
 * validates bounds, budgets, history capacity and journal overlap. Chunk tickets
 * are acquired incrementally before a scan captures ordinary block data. A worker
 * saves the complete journal before painting writes are allowed.
 * <p>
 * Restoration changes only blocks that still match the intended output. Later
 * edits and block entities are preserved; conflicts retain the journal. Cancel,
 * disconnect and shutdown release world resources while retaining any durable
 * record needed to restore partial work.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class PaintService {

    private final PixelPainter plugin;
    private final UndoStore store;
    private final Map<String, PaintRecord> history = new LinkedHashMap<>();
    private final BukkitTask ticker;
    private Job active;

    /**
     * Publishes recovered journals and registers the server-thread ticker.
     *
     * @param plugin owning enabled plugin
     * @param store persistent journal storage
     * @param restored startup recovery records
     */
    public PaintService(PixelPainter plugin, UndoStore store, List<PaintRecord> restored) {
        this.plugin = plugin;
        this.store = store;
        restored.forEach(record -> history.put(record.id(), record));
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    /**
     * Checks whether a world job, including pending journal I/O, occupies the service.
     *
     * @return whether a job is active
     */
    public boolean busy() {
        return active != null;
    }

    /**
     * Checks whether the active job holds resources in a world.
     *
     * @param world queried world UUID
     * @return whether that world is in use
     */
    public boolean usesWorld(UUID world) {
        return active != null && active.world.getUID().equals(world);
    }

    /**
     * Lists journals accessible to a player in newest-first order.
     *
     * @param player owner or administrator with pp.admin
     * @return copied list of accessible records
     */
    public List<PaintRecord> history(Player player) {
        return history.values().stream().filter(r -> r.owner().equals(player.getUniqueId()) || player.hasPermission("pp.admin"))
            .sorted(Comparator.comparingLong(PaintRecord::created).reversed()).toList();
    }

    /**
     * Validates and queues a prepared image for incremental capture and painting.
     *
     * @param player requesting player; command layer checks creation permission
     * @param image source name
     * @param plan prepared pixel materials
     * @param orientation image-to-world mapping
     * @param origin bottom-left image block
     * @param worldId world captured when preparation started
     * @param settings captured request limits
     * @param ignoreBorder whether a painting may extend past the world border
     * @throws PaintException if world, bounds, capacity or overlap checks fail
     */
    public void create(Player player, String image, PaintPlan plan, Orientation orientation, Position origin, UUID worldId,
        Settings settings, boolean ignoreBorder) {
        requireIdle();

        if (history.size() >= settings.maxHistory()) {
            throw new PaintException(PaintError.HISTORY_FULL);
        }

        World world = Bukkit.getWorld(worldId);

        if (world == null || !player.getWorld().getUID().equals(worldId)) {
            throw new PaintException(PaintError.WORLD);
        }

        Bounds bounds = Bounds.of(origin, orientation.position(origin, plan.width() - 1, 0, plan.height()));

        validate(world, bounds, settings, ignoreBorder);

        for (PaintRecord record : history.values()) {
            if (record.world().equals(worldId) && record.bounds().intersects(bounds)) {
                throw new PaintException(PaintError.OVERLAP);
            }
        }

        int stored = history.values().stream().mapToInt(r -> r.changes().size()).sum();

        if ((long) stored + plan.painted() > settings.maxStoredBlocks()) {
            throw new PaintException(PaintError.HISTORY_FULL);
        }

        String id;
        do {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (history.containsKey(id));
        active = new Job(player, world, bounds, settings, id, image, plan, orientation, origin, null);
        plugin.messages().send(player, "commands.queued", "image", image, "id", id);
    }

    /**
     * Queues restoration of the latest accessible or explicitly requested journal.
     *
     * @param player owner or administrator; command layer checks undo permission
     * @param requested identifier or source name, or null for the latest accessible journal
     * @throws PaintException if no journal matches or restoration cannot start
     */
    public void undo(Player player, String requested) {
        requireIdle();
        PaintRecord record = history(player).stream().filter(r -> requested == null || r.id().equalsIgnoreCase(requested)
            || r.image().equalsIgnoreCase(requested)).findFirst().orElseThrow(() -> new PaintException(PaintError.MISSING_HISTORY));
        World world = Bukkit.getWorld(record.world());

        if (world == null) {
            throw new PaintException(PaintError.WORLD);
        }

        validate(world, record.bounds(), plugin.settings(), true);
        active = new Job(player, world, record.bounds(), plugin.settings(), record.id(), record.image(),
            null, null, null, record);
        plugin.messages().send(player, "commands.undo-started", "id", record.id());
    }

    private void validate(World world, Bounds bounds, Settings settings, boolean ignoreBorder) {
        if (bounds.minY() < world.getMinHeight() || bounds.maxY() >= world.getMaxHeight()
            || Math.abs((long) bounds.minX()) > 29999984 || Math.abs((long) bounds.maxX()) > 29999984
            || Math.abs((long) bounds.minZ()) > 29999984 || Math.abs((long) bounds.maxZ()) > 29999984) {
            throw new PaintException(PaintError.BOUNDS);
        }

        if (bounds.chunks() > settings.maxChunks()) {
            throw new PaintException(PaintError.CHUNKS);
        }

        var border = world.getWorldBorder();
        double half = border.getSize() / 2;
        var center = border.getCenter();

        if (!ignoreBorder && (bounds.minX() < center.getX() - half || bounds.maxX() + 1 > center.getX() + half
            || bounds.minZ() < center.getZ() - half || bounds.maxZ() + 1 > center.getZ() + half)) {
            throw new PaintException(PaintError.BORDER);
        }
    }
}
