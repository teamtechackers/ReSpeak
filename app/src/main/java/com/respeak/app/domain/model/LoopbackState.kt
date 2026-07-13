/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.respeak.app.domain.model

sealed class LoopbackState {
    object Idle : LoopbackState()
    data class Active(val startTime: Long, val deviceName: String) : LoopbackState()
    object NoHeadphones : LoopbackState()
    object PermissionDenied : LoopbackState()
    object FocusLost : LoopbackState()
}
