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

import java.util.List;
import java.util.UUID;

/**
 * Represents a durable undo journal written before its painting changes terrain.
 * <p>
 * Owner and world UUIDs identify access and the target world. Inclusive bounds
 * reserve its area against overlapping paintings. The copied change list records
 * both original and intended block properties.
 * <p>
 * A record can describe interrupted or partial work. Recovery checks live block
 * data rather than assuming every change was applied. Block-entity NBT is not
 * stored because creation refuses to overwrite those blocks.
 *
 * @param id twelve-character hexadecimal journal identifier
 * @param owner creating player UUID
 * @param world target world UUID
 * @param image source image name
 * @param created creation timestamp in epoch milliseconds
 * @param bounds inclusive reserved region
 * @param changes original and target block states
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public record PaintRecord(String id, UUID owner, UUID world, String image, long created, Bounds bounds,
    List<Change> changes) {

    /**
     * Copies the change list to isolate the journal payload from caller mutations.
     */
    public PaintRecord {
        changes = List.copyOf(changes);
    }

    /**
     * Records one original and intended block state at a world-relative coordinate.
     *
     * @param position block coordinate
     * @param before canonical original block data
     * @param after canonical painting block data
     * @author Despical
     * <p>
     * Created at 15.10.2026
     */
    public record Change(Position position, String before, String after) {
    }
}
