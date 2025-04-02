/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.savedstate.SavedState

/** A [Navigator] that only supports creating destinations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Navigator.Name("NoOp")
public class NoOpNavigator() : Navigator<NavDestination>("NoOp") {
    override fun createDestination(): NavDestination = NavDestination(this)

    override fun navigate(
        destination: NavDestination,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination = destination

    override fun popBackStack(): Boolean = true
}
