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

/**
 * Bounded worker execution and incremental world operations with durable recovery.
 * <p>
 * TaskExecutor returns image/file results to the server thread while the plugin is
 * enabled and closes its bounded queue during shutdown. One server-thread painting
 * job acquires chunk tickets, scans target blocks and waits for
 * a durable undo journal before writing terrain. Painting and restoration process
 * bounded work each tick. Undo preserves later edits and block entities; conflicts
 * retain recovery records. Cancellation and disconnect stop further writes while
 * keeping durable recovery data, and shutdown releases tickets and progress UI.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
package dev.despical.pixelpainter.service;
