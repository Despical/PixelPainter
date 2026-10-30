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

/**
 * Signals a failure the sender can correct, carrying its typed {@link PaintError}.
 * <p>
 * Extends {@link IllegalArgumentException} so existing validation callers keep
 * working, while command handling reads {@link #error()} instead of parsing text.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
public final class PaintException extends IllegalArgumentException {
}
