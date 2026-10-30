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

package dev.despical.pixelpainter.config;

import dev.despical.pixelpainter.image.BlockPalette;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.util.List;

/**
 * Creates bundled configuration files and publishes validated resource snapshots.
 * <p>
 * A reload first reads settings, messages and the block palette into local
 * candidates. Active references change only after all candidates are valid,
 * so a malformed resource cannot leave the plugin partly reconfigured.
 * <p>
 * Call this manager on the server thread: palette validation resolves Bukkit
 * block data. Existing image preparations and painting jobs keep the settings
 * and colors they captured before a later reload.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class ConfigurationManager {

    private final JavaPlugin plugin;

    private volatile Settings settings;
    private volatile Messages messages;
    private volatile BlockPalette palette;

    /**
     * Creates a manager without writing files or loading configuration.
     *
     * @param plugin owner of the bundled resources and data directory
     */
    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Copies missing bundled resources and loads the first validated snapshot.
     *
     * @throws Exception if resource creation or validation fails
     */
    public void initialize() throws Exception {
        for (String resource : List.of("config.yml", "messages.yml", "palette.yml")) {
            if (!Files.exists(plugin.getDataFolder().toPath().resolve(resource))) {
                plugin.saveResource(resource, false);
            }
        }

        reload();
    }

    /**
     * Loads all candidates before replacing the active configuration references.
     *
     * @throws Exception if any resource cannot be read or validated
     */
    public void reload() throws Exception {
        var folder = plugin.getDataFolder().toPath();

        Settings loadedSettings = Settings.load(folder.resolve("config.yml"));
        Messages loadedMessages = Messages.load(folder.resolve("messages.yml"));
        BlockPalette loadedPalette = BlockPalette.load(folder.resolve("palette.yml"),
            loadedSettings.included(), loadedSettings.excluded()
        );

        settings = loadedSettings;
        messages = loadedMessages;
        palette = loadedPalette;
    }

    /**
     * Returns the current operational limits and presentation settings.
     *
     * @return validated settings after initialization
     */
    public Settings settings() {
        return settings;
    }

    /**
     * Returns the current message templates.
     *
     * @return validated messages after initialization
     */
    public Messages messages() {
        return messages;
    }

    /**
     * Returns the current server-resolved block palette.
     *
     * @return validated palette after initialization
     */
    public BlockPalette palette() {
        return palette;
    }
}
