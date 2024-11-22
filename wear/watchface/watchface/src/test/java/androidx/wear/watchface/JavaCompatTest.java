/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface;

import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.annotation.Px;
import androidx.wear.watchface.complications.data.ComplicationData;

import org.jspecify.annotations.NonNull;

import java.time.ZonedDateTime;

/** Tests that Java interfaces implementing kotlin interfaces with defaults compile. */
public class JavaCompatTest {
    class ComplicationTapFilterImpl implements ComplicationTapFilter {
        @SuppressWarnings("deprecation")
        public boolean hitTest(
                @NonNull ComplicationSlot complicationSlot,
                @NonNull Rect screenBounds,
                @Px int x,
                @Px int y) {
            return true;
        }
    }

    class CanvasComplicationImpl implements CanvasComplication {
        @Override
        public void onRendererCreated(@NonNull Renderer renderer) {
            CanvasComplication.super.onRendererCreated(renderer);
        }

        @Override
        public void render(
                @NonNull Canvas canvas,
                @NonNull Rect bounds,
                @NonNull ZonedDateTime zonedDateTime,
                @NonNull RenderParameters renderParameters,
                int slotId) {}

        @Override
        public void drawHighlight(
                @NonNull Canvas canvas,
                @NonNull Rect bounds,
                int boundsType,
                @NonNull ZonedDateTime zonedDateTime,
                int color) {}

        @Override
        public @NonNull ComplicationData getData() {
            return null;
        }

        @Override
        public void loadData(
                @NonNull ComplicationData complicationData, boolean loadDrawablesAsynchronous) {}
    }
}
