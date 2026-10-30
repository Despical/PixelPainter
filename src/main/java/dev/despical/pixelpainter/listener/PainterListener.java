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

package dev.despical.pixelpainter.listener;

import dev.despical.pixelpainter.PixelPainter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Releases painting resources when a player disconnects and protects the active world.
 * <p>
 * A departing player's world job and cancellable image preparations are stopped while
 * durable undo data is retained. An explicit world unload is cancelled while the active
 * painting job still holds chunk tickets in that world.
 * <p>
 * Handlers run on the server thread and only delegate to the painting service and
 * command dispatcher.
 *
 * @author Despical
 * <p>
 * Created at 30.10.2026
 */
public final class PainterListener implements Listener {

    private final PixelPainter plugin;

    /**
     * Creates a listener bound to an enabled plugin.
     *
     * @param plugin owning plugin that provides the painting services
     */
    public PainterListener(PixelPainter plugin) {
        this.plugin = plugin;
    }

    /**
     * Stops the departing owner's world job and cancellable image preparations.
     *
     * @param event player disconnect notification
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.painter().quit(event.getPlayer().getUniqueId());
        plugin.commands().quit(event.getPlayer().getUniqueId());
    }

    /**
     * Prevents an explicit unload from invalidating the active painting world.
     *
     * @param event proposed world unload
     */
    @EventHandler(ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (plugin.painter().usesWorld(event.getWorld().getUID())) {
            event.setCancelled(true);
        }
    }
}
