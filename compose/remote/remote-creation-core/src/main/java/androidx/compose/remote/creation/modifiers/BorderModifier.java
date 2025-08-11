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
package androidx.compose.remote.creation.modifiers;

import androidx.compose.remote.core.operations.layout.modifiers.ShapeType;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Border modifier, takes a color and a shape */
public class BorderModifier implements RecordingModifier.Element {
    float mWidth;
    float mRoundedCorner;
    int mColor;
    int mShapeType = ShapeType.RECTANGLE;

    /**
     * Border modifier
     *
     * @param width border width
     * @param roundedCorner rounded corner dimension
     * @param color int color
     * @param shape Shape.RECTANGLE/CIRCLE/ROUNDED_RECTANGLE
     */
    public BorderModifier(float width, float roundedCorner, int color, int shape) {
        mWidth = width;
        mRoundedCorner = roundedCorner;
        mColor = color;
        mShapeType = shape;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getRoundedCorner() {
        return mRoundedCorner;
    }

    public int getColor() {
        return mColor;
    }

    public int getShapeType() {
        return mShapeType;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        writer.addModifierBorder(mWidth, mRoundedCorner, mColor, mShapeType);
    }
}
