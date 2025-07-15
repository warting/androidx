/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.featureflag

import androidx.annotation.RestrictTo

/**
 * Provides centralized access to all feature flags related to the AndroidX PDF library.
 *
 * This object serves as the single source of truth for determining if specific experimental or
 * configurable features across different PDF functionalities are enabled or disabled.
 *
 * The flags within this object are mutable properties, allowing for dynamic configuration and
 * staged rollouts of PDF library behaviors.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object PdfFeatureFlags {
    // Toggles handling of external hardware events like keyboard and mouse shortcuts.
    public var isExternalHardwareInteractionEnabled: Boolean = false

    // Toggles availability of smart action contextual menu components for text selection.
    public var isSmartActionMenuComponentEnabled: Boolean = false
}
