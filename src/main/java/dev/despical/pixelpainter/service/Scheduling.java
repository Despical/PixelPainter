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

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Schedules plugin work through the region-aware Paper schedulers.
 * <p>
 * The global, region and entity schedulers exist on Paper and on Folia. On Paper they
 * all run on the single server thread, so the same code path serves both platforms.
 * On Folia each task runs on the thread that owns the target location or player, which
 * is the only thread allowed to read or change that state.
 * <p>
 * Use the global scheduler for plugin state that is not tied to a location, a region
 * scheduler for block access and an entity hop for player-bound actions. Instances are
 * stateless after construction and safe to share between threads.
 *
 * @author Despical
 * <p>
 * Created at 30.10.2026
 */
public final class Scheduling {

    private final Plugin plugin;
    private final boolean folia;

    /**
     * Creates a scheduler facade for an enabled plugin.
     *
     * @param plugin owner of every scheduled task
     */
    public Scheduling(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");

            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    /**
     * Reports whether the server runs Folia's regionized threading.
     *
     * @return whether regions tick on separate threads
     */
    public boolean folia() {
        return folia;
    }

    /**
     * Starts a task that runs every tick on the global region.
     *
     * @param task work that touches only plugin state
     * @return handle used to cancel the repeating task
     */
    public ScheduledTask timer(Runnable task) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> task.run(), 1, 1);
    }

    /**
     * Runs a task on the global region as soon as possible.
     *
     * @param task work that touches only plugin state
     */
    public void global(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    /**
     * Runs a task on the global region after a delay.
     *
     * @param task work that touches only plugin state
     * @param ticks delay in server ticks, at least one
     */
    public void delayed(Runnable task, long ticks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduled -> task.run(), Math.max(1, ticks));
    }

    /**
     * Runs a task on the thread that owns a chunk.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task work that may read and change blocks inside that chunk
     */
    public void region(World world, int chunkX, int chunkZ, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
    }

    /**
     * Runs a task on the thread that owns a player, immediately when already there.
     *
     * @param player player whose state the task reads or changes
     * @param task player-bound work; skipped when the player has left the server
     */
    public void entity(Player player, Runnable task) {
        if (!folia || Bukkit.isOwnedByCurrentRegion(player)) {
            task.run();

            return;
        }

        player.getScheduler().run(plugin, scheduled -> task.run(), null);
    }
}
