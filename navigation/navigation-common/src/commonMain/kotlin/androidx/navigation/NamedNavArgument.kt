/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("NamedNavArgumentKt")
@file:JvmMultifileClass

package androidx.navigation

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/** Construct a new [NavArgument] */
public fun navArgument(name: String, builder: NavArgumentBuilder.() -> Unit): NamedNavArgument =
    NamedNavArgument(name, NavArgumentBuilder().apply(builder).build())

/** Construct a named [NavArgument] by using the [navArgument] method. */
public class NamedNavArgument
internal constructor(

    /** The name the argument is associated with */
    public val name: String,

    /** The [NavArgument] associated with the name */
    public val argument: NavArgument,
) {
    /** Provides destructuring access to this [NamedNavArgument]'s [name] */
    public operator fun component1(): String = name

    /** Provides destructuring access to this [NamedNavArgument]'s [argument] */
    public operator fun component2(): NavArgument = argument
}
