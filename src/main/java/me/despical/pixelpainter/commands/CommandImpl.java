package me.despical.pixelpainter.commands;

import me.despical.commons.util.Strings;
import me.despical.pixelpainter.Main;
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