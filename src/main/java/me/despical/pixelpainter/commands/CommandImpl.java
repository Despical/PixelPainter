/*
 * Pixel Painter - Turn your images into blocks.
 * Copyright (C) 2023 Despical
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

package me.despical.pixelpainter.commands;

import me.despical.commons.util.Strings;
import me.despical.pixelpainter.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public interface CommandImpl {

	Main plugin = JavaPlugin.getPlugin(Main.class);
	String prefix = "&7[&f&lPixel Painter&7] &f";
	ChatColor bold = ChatColor.BOLD;

	default void register(Object object) {
		plugin.getCommandFramework().registerCommands(object);
	}

	default void sendMessage(Player player, String message) {
		player.sendMessage(Strings.format(prefix + message));
	}

	default void sendMessage(Player player, String message, Object... params) {
		this.sendMessage(player, String.format(message, params));
	}
}