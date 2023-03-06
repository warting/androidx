/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.Notification;

import androidx.annotation.RestrictTo;

/**
 * Interface implemented by notification compat builders that support
 * an accessor for {@link Notification.Builder}. {@link Notification.Builder}
 * was introduced in HoneyComb.
 *
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public interface NotificationBuilderWithBuilderAccessor {
    Notification.Builder getBuilder();
}
