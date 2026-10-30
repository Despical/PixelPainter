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

package dev.despical.pixelpainter.storage;

import dev.despical.pixelpainter.core.Bounds;
import dev.despical.pixelpainter.core.Position;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists versioned binary undo journals and reloads interrupted operations.
 * <p>
 * Saving writes a sibling temporary file, flushes its contents and requests an
 * atomic move into place, falling back to an ordinary move when necessary.
 * Interrupted temporary files are ignored at load.
 * <p>
 * Loading validates format markers, filename/ID relationships, block counts,
 * bounds and trailing data. Corrupt journals fail the load and remain on disk.
 * Blocking operations run on workers, except initial loading during startup.
 * The caller serializes operations targeting the same journal identifier.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class UndoStore {

    private static final int MAGIC = 0x50505532;
    private final Path folder;

    /**
     * Creates and resolves the journal directory.
     *
     * @param folder persistent undo directory
     * @throws IOException if the directory cannot be created or resolved
     */
    public UndoStore(Path folder) throws IOException {
        Files.createDirectories(folder);
        this.folder = folder.toRealPath();
    }

    /**
     * Loads complete journals while ignoring unfinished temporary files.
     *
     * @return copied list of decoded journals
     * @throws IOException if a journal is malformed, unreadable or exceeds recovery limits
     */
    public List<PaintRecord> load() throws IOException {
        List<PaintRecord> records = new ArrayList<>();
        int total = 0;

        try (var paths = Files.list(folder)
        ) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".ppu")).sorted().toList()) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Undo journal must be a regular file: " + path.getFileName());
                }

                PaintRecord record = read(path);
                total += record.changes().size();

                if (records.size() >= 256 || total > 4194304) {
                    throw new IOException("Undo journal hard limit exceeded. Preserve journals before maintenance.");
                }

                records.add(record);
            }
        }

        return List.copyOf(records);
    }
}
