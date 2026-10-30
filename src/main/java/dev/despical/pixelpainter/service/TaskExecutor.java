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
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs bounded image and file work and returns results to the global region thread.
 * <p>
 * Two workers share a queue with eight waiting tasks. Saturation reports a
 * configured queue-full failure instead of allocating an unbounded backlog.
 * Worker tasks must not access live Bukkit worlds or send player messages.
 * <p>
 * Success and failure callbacks are scheduled on the global region thread while the
 * plugin is enabled. Shutdown stops accepting work and gives pending file
 * operations up to ten seconds to finish. A completed journal remains usable
 * on disk even when shutdown prevents delivery of its callback.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class TaskExecutor implements AutoCloseable {

    private final JavaPlugin plugin;
    private final ThreadPoolExecutor workers;

    /**
     * Creates the bounded executor without starting any work.
     *
     * @param plugin owner used to schedule callbacks
     */
    public TaskExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), runnable -> {
            Thread thread = new Thread(runnable, "PixelPainter-IO");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Submits a task and delivers its result on the global region thread.
     *
     * @param <T> result type
     * @param task blocking work without live world access
     * @param completed global-region success callback
     * @param failed global-region error callback; rejection is delivered immediately
     */
    public <T> void submit(Callable<T> task, Consumer<T> completed, Consumer<Exception> failed) {
        try {
            workers.execute(() -> {
                try {
                    T result = task.call();

                    dispatch(() -> completed.accept(result));
                } catch (Exception exception) {
                    dispatch(() -> failed.accept(exception));
                }
            });
        } catch (RejectedExecutionException exception) {
            failed.accept(new PaintException(PaintError.QUEUE_FULL, exception));
        }
    }

    private void dispatch(Runnable callback) {
        if (!plugin.isEnabled()) {
            return;
        }

        try {
            Bukkit.getGlobalRegionScheduler().execute(plugin, callback);
        } catch (IllegalPluginAccessException ignored) {
            // Shutdown may finish between the enabled check and scheduling.
        }
    }

    /**
     * Stops submissions and waits for pending work before interrupting workers.
     */
    @Override
    public void close() {
        workers.shutdown();

        try {
            if (!workers.awaitTermination(10, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException exception) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
