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
 * Permission-checked command dispatch and Adventure chat presentation.
 * <p>
 * ACF handlers parse positional options and capture request settings before
 * calling image workers or the painting service. Per-player preparation tokens
 * suppress stale results after cancellation. Panels read current messages and
 * render suggestions, hover details and page navigation on the server thread.
 * Journal access is filtered by the service before records reach a panel.
 *
 * @author Despical
 * <p>
 * Created at 15.10.2026
 */
package dev.despical.pixelpainter.command;
