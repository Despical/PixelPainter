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

    /**
     * Delegates an all-or-nothing resource reload on the server thread.
     *
     * @throws Exception if a resource cannot be loaded or fails validation
     */
    public void reloadSettings() throws Exception {
        configuration.reload();
    }

    /**
     * Submits blocking work through the bounded executor.
     *
     * @param <T> result type
     * @param task worker operation without live Bukkit world access
     * @param completed server-thread success handler
     * @param failed server-thread failure or queue-rejection handler
     */
    public <T> void io(Callable<T> task, Consumer<T> completed, Consumer<Exception> failed) {
        tasks.submit(task, completed, failed);
    }

    /**
     * Refreshes cached source names after a worker finishes scanning the image folder.
     *
     * @param completed server-thread callback receiving the refreshed names
     * @param failed server-thread failure handler
     */
    public void refreshImages(Consumer<List<String>> completed, Consumer<Exception> failed) {
        io(images::list, names -> {
            imageNames = names;

            completed.accept(names);
        }, failed);
    }

    /**
     * Releases world resources and commands before closing the background workers.
     */
    @Override
    public void onDisable() {
        IIORegistry.getDefaultInstance().deregisterServiceProvider(webpReader);

        if (painter != null) {
            painter.close();
        }

        if (commandManager != null) {
            commandManager.unregisterCommands();
        }

        if (tasks != null) {
            tasks.close();
        }
    }

    /**
     * Returns the operational settings currently published on the server thread.
     *
     * @return validated settings after successful initialization
     */
    public Settings settings() {
        return configuration.settings();
    }

    /**
     * Returns the current configured message templates.
     *
     * @return validated message resources
     */
    public Messages messages() {
        return configuration.messages();
    }

    /**
     * Returns the current palette of server-resolved block colors.
     *
     * @return validated block palette
     */
    public BlockPalette palette() {
        return configuration.palette();
    }

    /**
     * Returns the service responsible for local source files and downloads.
     *
     * @return image store after successful initialization
     */
    public ImageStore images() {
        return images;
    }

    /**
     * Returns the service that owns painting and terrain restoration.
     *
     * @return server-thread painting service
     */
    public PaintService painter() {
        return painter;
    }

    /**
     * Returns the command dispatcher that tracks per-player preparations.
     *
     * @return command dispatcher after successful initialization
     */
    public PainterCommands commands() {
        return commands;
    }

    /**
     * Returns cached source names without starting file I/O during completion.
     *
     * @return sorted source-image names
     */
    public List<String> imageNames() {
        return imageNames;
    }
}
