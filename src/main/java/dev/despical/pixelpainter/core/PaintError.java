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

package dev.despical.pixelpainter.core;

import java.util.Locale;

/**
 * Enumerates user-correctable failures and the message each one resolves to.
 * <p>
 * Replaces free-form error strings so every failure that can reach a command
 * sender is checked by the compiler and mapped to a bundled panel message.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public enum PaintError {

    /** The sender lacks the command permission. */
    PERMISSION("permission-denied"),
    /** The command requires a player sender. */
    PLAYER("player-only"),
    /** The argument count is wrong. */
    USAGE("invalid-syntax"),
    /** The subcommand does not exist. */
    UNKNOWN_COMMAND("unknown-command"),
    /** A positive whole number was expected. */
    NUMBER("invalid-number"),
    /** A true or false option was expected. */
    BOOLEAN("invalid-boolean"),
    /** The direction name is unknown. */
    DIRECTION("invalid-direction"),
    /** The palette name is unknown. */
    PALETTE("invalid-palette"),
    /** The image file name is unsafe or malformed. */
    FILENAME("invalid-filename"),
    /** The requested page does not exist. */
    PAGE("invalid-page"),
    /** No image matches the name. */
    MISSING_IMAGE("missing-image"),
    /** More than one image matches the name. */
    AMBIGUOUS_IMAGE("ambiguous-image"),
    /** An image with this name exists already. */
    IMAGE_EXISTS("image-exists"),
    /** The file is not a readable PNG, JPEG or WebP image. */
    IMAGE_FORMAT("image-format"),
    /** The file is larger than the configured limit. */
    FILE_SIZE("file-size"),
    /** The decoded image is larger than the configured limit. */
    SOURCE_SIZE("source-size"),
    /** The painting exceeds the height or block limit. */
    SIZE("size"),
    /** The download host is not allowed. */
    DOWNLOAD_HOST("download-host"),
    /** The download failed. */
    DOWNLOAD_FAILED("download-failed"),
    /** An image request of the player is still being prepared. */
    PREPARING("preparing-busy"),
    /** A file operation of the player is still running. */
    FILE_BUSY("file-busy"),
    /** The worker queue rejected the task. */
    QUEUE_FULL("queue-full"),
    /** Another world operation is running. */
    BUSY("busy"),
    /** The player has no job to cancel. */
    NO_JOB("no-job"),
    /** No accessible journal matches. */
    MISSING_HISTORY("missing-history"),
    /** The undo storage limit is reached. */
    HISTORY_FULL("history-full"),
    /** The world is unavailable. */
    WORLD("world"),
    /** The painting leaves the world bounds. */
    BOUNDS("bounds"),
    /** The painting crosses the world border, which a confirmed request may ignore. */
    BORDER("border"),
    /** The painting spans too many chunks. */
    CHUNKS("chunks"),
    /** The area overlaps a saved painting. */
    OVERLAP("overlap"),
    /** The area contains a block entity. */
    BLOCK_ENTITY("block-entity");

    private final String key;

    PaintError(String key) {
        this.key = "commands." + key;
    }

    /**
     * Returns the message key whose {@code -title} companion forms the panel.
     *
     * @return full message key
     */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
