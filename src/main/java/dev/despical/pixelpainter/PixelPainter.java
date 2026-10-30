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

package dev.despical.pixelpainter;

import co.aikar.commands.BukkitCommandManager;

import dev.despical.pixelpainter.command.PainterCommands;
import dev.despical.pixelpainter.config.ConfigurationManager;
import dev.despical.pixelpainter.config.Messages;
import dev.despical.pixelpainter.config.Settings;
import dev.despical.pixelpainter.image.BlockPalette;
import dev.despical.pixelpainter.image.ImageStore;
import dev.despical.pixelpainter.listener.PainterListener;
import dev.despical.pixelpainter.service.PaintService;
import dev.despical.pixelpainter.service.TaskExecutor;
import dev.despical.pixelpainter.storage.UndoStore;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.spi.IIORegistry;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Starts PixelPainter and connects its configuration, commands and painting services.
 * <p>
 * The entry point owns service lifetimes rather than loading YAML or managing
 * worker queues itself. ConfigurationManager creates and validates resources;
 * TaskExecutor handles blocking image/file work and server-thread callbacks;
 * PaintService owns world jobs, chunk tickets and persistent recovery history.
 * <p>
 * Startup proceeds in that order so commands cannot observe missing resources
 * or unrecovered journals. Any initialization failure disables the plugin.
 * Shutdown releases world resources before closing the worker executor, leaving
 * completed undo journals available for recovery on the next startup.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class PixelPainter extends JavaPlugin {

    private ConfigurationManager configuration;
    private TaskExecutor tasks;
    private ImageStore images;
    private PaintService painter;
    private PainterCommands commands;
    private BukkitCommandManager commandManager;
    private List<String> imageNames = List.of();

    private final WebPImageReaderSpi webpReader = new WebPImageReaderSpi();

    @Override
    public void onEnable() {
        try {
            IIORegistry.getDefaultInstance().registerServiceProvider(webpReader);
            initializeConfiguration();
            initializeServices();
            registerCommands();
            registerListeners();

            getLogger().info("PixelPainter ready: " + palette().entries().size() + " block colors, persistent undo enabled.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Cannot enable PixelPainter. Check configuration and undo journals.", exception);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void initializeConfiguration() throws Exception {
        configuration = new ConfigurationManager(this);
        configuration.initialize();
    }

    private void initializeServices() throws Exception {
        var folder = getDataFolder().toPath();
        tasks = new TaskExecutor(this);
        images = new ImageStore(folder.resolve("images"));
        imageNames = images.list();

        var undoStore = new UndoStore(folder.resolve("undo"));
        painter = new PaintService(this, undoStore, undoStore.load());
    }

    private void registerCommands() {
        commandManager = new BukkitCommandManager(this);
        commands = new PainterCommands(this);
        commands.registerCompletions(commandManager);
        commandManager.registerCommand(commands);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PainterListener(this), this);
    }
}
