/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.remote.frontend.layout

import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.ui.geometry.Offset

class RemoteOffset {

    val x: RemoteFloat
    val y: RemoteFloat

    constructor(x: RemoteFloat, y: RemoteFloat) {
        this.x = x
        this.y = y
    }

    constructor(x: Float, y: Float) : this(RemoteFloat(x), RemoteFloat(y))

    constructor(x: Float, y: RemoteFloat) : this(RemoteFloat(x), y)

    constructor(x: RemoteFloat, y: Float) : this(x, RemoteFloat(y))

    fun asOffset(): Offset {
        return Offset(x.internalAsFloat(), y.internalAsFloat())
    }
}
