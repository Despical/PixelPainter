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

    private PaintRecord read(Path path) throws IOException {
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC) {
                throw new IOException("Unsupported undo journal: " + path.getFileName());
            }

            String id = input.readUTF();

            if (!id.matches("[a-f0-9]{12}") || !path.getFileName().toString().equals(id + ".ppu")) {
                throw new IOException("Invalid undo journal ID.");
            }

            UUID owner = UUID.fromString(input.readUTF()), world = UUID.fromString(input.readUTF());
            String image = input.readUTF();
            long created = input.readLong();
            Bounds bounds = new Bounds(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt());

            if (bounds.minX() > bounds.maxX() || bounds.minY() > bounds.maxY() || bounds.minZ() > bounds.maxZ()) {
                throw new IOException("Invalid undo bounds.");
            }

            int count = input.readInt();
            if (count <= 0 || count > 1048576) {
                throw new IOException("Invalid undo journal size.");
            }

            var changes = new ArrayList<PaintRecord.Change>(count);

            for (int i = 0; i < count; i++) {
                Position position = new Position(input.readInt(), input.readInt(), input.readInt());

                if (position.x() < bounds.minX() || position.x() > bounds.maxX()
                    || position.y() < bounds.minY() || position.y() > bounds.maxY()
                    || position.z() < bounds.minZ() || position.z() > bounds.maxZ()
                ) {
                    throw new IOException("Undo block lies outside journal bounds.");
                }

                changes.add(new PaintRecord.Change(position, input.readUTF(), input.readUTF()));
            }

            if (input.read() != -1) {
                throw new IOException("Trailing data in undo journal.");
            }

            return new PaintRecord(id, owner, world, image, created, bounds, changes);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid undo journal: " + path.getFileName(), exception);
        }
    }

    /**
     * Writes and flushes one journal before its world mutation is allowed.
     *
     * @param record journal payload with a unique, valid identifier
     * @throws IOException if writing, flushing or promoting the file fails
     */
    public void save(PaintRecord record) throws IOException {
        Path temporary = Files.createTempFile(folder, record.id(), ".tmp");

        try {
            try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeInt(MAGIC);
                output.writeUTF(record.id());
                output.writeUTF(record.owner().toString());
                output.writeUTF(record.world().toString());
                output.writeUTF(record.image());
                output.writeLong(record.created());

                Bounds b = record.bounds();
                output.writeInt(b.minX());
                output.writeInt(b.minY());
                output.writeInt(b.minZ());
                output.writeInt(b.maxX());
                output.writeInt(b.maxY());
                output.writeInt(b.maxZ());
                output.writeInt(record.changes().size());

                for (var change : record.changes()) {
                    output.writeInt(change.position().x());
                    output.writeInt(change.position().y());
                    output.writeInt(change.position().z());
                    output.writeUTF(change.before());
                    output.writeUTF(change.after());
                }
            }

            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }

            try {
                Files.move(temporary, folder.resolve(record.id() + ".ppu"), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, folder.resolve(record.id() + ".ppu"));
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
