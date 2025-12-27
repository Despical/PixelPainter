package dev.despical.pixelpainter.commands;

import dev.despical.commons.util.Strings;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public interface CommandImpl {

    String prefix = "&7[&f&lPixel Painter&7] &f";

    default void sendMessage(Player player, String message) {
        player.sendMessage(Strings.format(prefix + message));
    }

    default void sendMessage(Player player, String message, Object... params) {
        this.sendMessage(player, String.format(message, params));
    }
}
