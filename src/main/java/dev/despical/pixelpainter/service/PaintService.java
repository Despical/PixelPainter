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

    private void requireIdle() {
        if (active != null) {
            throw new PaintException(PaintError.BUSY);
        }
    }

    /**
     * Cancels the caller's active job, or any job for an administrator.
     *
     * @param player requesting owner or administrator
     * @throws PaintException if the player has no cancellable job
     */
    public void cancel(Player player) {
        if (active == null || (!active.owner.equals(player.getUniqueId()) && !player.hasPermission("pp.admin"))) {
            throw new PaintException(PaintError.NO_JOB);
        }

        active.cancelled = true;

        if (!active.waiting) {
            end("commands.cancelled");
        }
    }

    /**
     * Stops a departing owner's job while retaining durable undo data.
     *
     * @param owner departing player UUID
     */
    public void quit(UUID owner) {
        if (active != null && active.owner.equals(owner)) {
            active.cancelled = true;

            if (!active.waiting) {
                end(null);
            }
        }
    }

    private void tick() {
        if (active == null || active.waiting) {
            return;
        }

        try {
            active.step();
        } catch (PaintException exception) {
            end(exception.error().key());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Painting stopped; any saved undo journal is retained.", exception);
            end("commands.operation-failed");
        }
    }

    private void end(String key) {
        Job job = active;

        if (job == null) {
            return;
        }

        job.tickets.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
        Player player = Bukkit.getPlayer(job.owner);

        if (player != null) {
            player.hideBossBar(job.bar);

            if (key != null) {
                plugin.messages().send(player, key, "id", job.id, "blocks", job.changed, "conflicts", job.conflicts);
            }
        }

        active = null;
    }

    /**
     * Cancels the ticker and releases active chunk tickets and the progress bar.
     */
    public void close() {
        ticker.cancel();
        end(null);
    }

    /**
     * Lists the phases of a world job in execution order.
     */
    private enum Stage {

        CHUNKS, SCAN, SAVE, BUILD, UNDO;

        private String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * Carries the state machine for chunk loading, capture and bounded block writes.
     * <p>
     * A waiting journal callback retains the service slot. A durable journal is
     * published even when cancellation wins the write race. Block-data objects are
     * cached only inside this server-thread job.
     */
    private final class Job {

        private final UUID owner;
        private final World world;
        private final Bounds bounds;
        private final Settings settings;
        private final String id, image;
        private final PaintPlan plan;
        private final Orientation orientation;
        private final Position origin;
        private final PaintRecord undo;
        private final List<Chunk> tickets = new ArrayList<>();
        private final List<PaintRecord.Change> changes = new ArrayList<>();
        private final Map<String, BlockData> data = new HashMap<>();
        private final BossBar bar = BossBar.bossBar(plugin.messages().get("progress.stage-chunks"), 0, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

        private Stage stage = Stage.CHUNKS;
        private int chunkX, chunkZ, cursor, changed, conflicts;
        private boolean waiting, cancelled;

        private Job(Player player, World world, Bounds bounds, Settings settings, String id, String image,
            PaintPlan plan, Orientation orientation, Position origin, PaintRecord undo) {

            this.owner = player.getUniqueId();
            this.world = world;
            this.bounds = bounds;
            this.settings = settings;
            this.id = id;
            this.image = image;
            this.plan = plan;
            this.orientation = orientation;
            this.origin = origin;
            this.undo = undo;
            this.chunkX = bounds.minX() >> 4;
            this.chunkZ = bounds.minZ() >> 4;

            if (settings.progress()) {
                player.showBossBar(bar);
            }
        }

        private void step() {
            Player player = Bukkit.getPlayer(owner);

            if (cancelled || player == null || !player.isOnline()) {
                end("commands.cancelled");

                return;
            }

            if (stage == Stage.CHUNKS) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                chunk.addPluginChunkTicket(plugin);
                tickets.add(chunk);

                if (++chunkX > (bounds.maxX() >> 4)) {
                    chunkX = bounds.minX() >> 4;
                    chunkZ++;
                }

                if (chunkZ > (bounds.maxZ() >> 4)) {
                    stage = undo == null ? Stage.SCAN : Stage.UNDO;
                }

                progress(tickets.size(), (int) bounds.chunks());

                return;
            }

            int budget = settings.blocksPerTick();

            if (stage == Stage.SCAN) {
                while (budget-- > 0 && cursor < plan.size()) {
                    int index = cursor++;
                    String after = plan.block(index);

                    if (after == null) {
                        continue;
                    }

                    Position p = orientation.position(origin, index % plan.width(), index / plan.width(), plan.height());
                    var block = world.getBlockAt(p.x(), p.y(), p.z());

                    if (block.getState(false) instanceof TileState) {
                        throw new PaintException(PaintError.BLOCK_ENTITY);
                    }

                    String before = block.getBlockData().getAsString();

                    if (!before.equals(after)) {
                        changes.add(new PaintRecord.Change(p, before, after));
                    }
                }

                progress(cursor, plan.size());

                if (cursor == plan.size()) {
                    if (changes.isEmpty()) {
                        end("commands.nothing-changed");

                        return;
                    }

                    PaintRecord record = new PaintRecord(id, owner, world.getUID(), image, System.currentTimeMillis(), bounds, changes);

                    stage = Stage.SAVE;
                    progress(0, 1);
                    waiting = true;
                    plugin.io(() -> {
                        store.save(record);

                        return record;
                    }, saved -> {
                        history.put(saved.id(), saved);
                        waiting = false;

                        if (active != this) {
                            return;
                        }

                        if (cancelled) {
                            end("commands.cancelled");
                        } else {
                            stage = Stage.BUILD;
                            cursor = 0;
                        }
                    }, this::failed);
                }

                return;
            }

            List<PaintRecord.Change> targets = undo == null ? changes : undo.changes();

            while (budget-- > 0 && cursor < targets.size()) {
                var change = targets.get(cursor++);
                Position p = change.position();
                var block = world.getBlockAt(p.x(), p.y(), p.z());
                String current = block.getBlockData().getAsString();
                String expected = undo == null ? change.before() : change.after();
                String desired = undo == null ? change.after() : change.before();

                if (current.equals(desired)) {
                    continue;
                }

                if (!matches(current, expected) || block.getState(false) instanceof TileState) {
                    conflicts++;
                    continue;
                }

                block.setBlockData(data.computeIfAbsent(desired, Bukkit::createBlockData), false);
                changed++;
            }

            progress(cursor, targets.size());

            if (cursor == targets.size()) {
                if (undo == null) {
                    end("commands.completed");
                } else if (conflicts > 0) {
                    end("commands.undo-conflicts");
                } else {
                    waiting = true;
                    plugin.io(() -> {
                        store.delete(id);

                        return id;
                    }, removed -> {
                        history.remove(removed);
                        waiting = false;

                        if (active == this) {
                            end("commands.undone");
                        }
                    }, this::failed);
                }
            }
        }

        /**
         * Compares block data by material so state the game rewrites after placement,
         * such as snowy or face properties, is still recognized as the painted block.
         * Blocks that decay on their own, such as mycelium into dirt, also count as a match.
         */
        private static boolean matches(String current, String expected) {
            if (current.equals(expected)) {
                return true;
            }

            String material = material(expected);

            return material(current).equals(material) || BlockPalette.DECAYING.getOrDefault(material, Set.of()).contains(material(current));
        }

        private static String material(String data) {
            int properties = data.indexOf('[');

            return properties < 0 ? data : data.substring(0, properties);
        }

        private void progress(int done, int total) {
            bar.name(plugin.messages().get("progress.bar", "stage", plugin.messages().get("progress.stage-" + stage.key()),
                "image", image, "percent", total == 0 ? 100 : (int) (100L * done / total)));
            bar.progress(total == 0 ? 1 : Math.clamp((float) done / total, 0, 1));
        }

        private void failed(Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Undo journal operation failed.", exception);
            waiting = false;

            if (active == this) {
                end("commands.operation-failed");
            }
        }
    }
}
