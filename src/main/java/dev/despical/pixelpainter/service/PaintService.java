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
}
