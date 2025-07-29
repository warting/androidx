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

package androidx.transition;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.view.View;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@RequiresApi(21)
class ViewUtilsApi21 extends ViewUtilsApi19 {

    /**
     * False when linking of the hidden setAnimationMatrix method has previously failed.
     */
    private static boolean sTryHiddenSetAnimationMatrix = true;
    /**
     * False when linking of the hidden transformMatrixToGlobal method has previously failed.
     */
    private static boolean sTryHiddenTransformMatrixToGlobal = true;
    /**
     * False when linking of the hidden transformMatrixToLocal method has previously failed.
     */
    private static boolean sTryHiddenTransformMatrixToLocal = true;

    @Override
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public void transformMatrixToGlobal(@NonNull View view, @NonNull Matrix matrix) {
        if (sTryHiddenTransformMatrixToGlobal) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                Api29Impl.transformMatrixToGlobal(view, matrix);
            } catch (NoSuchMethodError e) {
                sTryHiddenTransformMatrixToGlobal = false;
            }
        }
    }

    @Override
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public void transformMatrixToLocal(@NonNull View view, @NonNull Matrix matrix) {
        if (sTryHiddenTransformMatrixToLocal) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                Api29Impl.transformMatrixToLocal(view, matrix);
            } catch (NoSuchMethodError e) {
                sTryHiddenTransformMatrixToLocal = false;
            }
        }
    }

    @Override
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public void setAnimationMatrix(@NonNull View view, @Nullable Matrix matrix) {
        if (sTryHiddenSetAnimationMatrix) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                Api29Impl.setAnimationMatrix(view, matrix);
            } catch (NoSuchMethodError e) {
                sTryHiddenSetAnimationMatrix = false;
            }
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void transformMatrixToGlobal(View view, Matrix matrix) {
            view.transformMatrixToGlobal(matrix);
        }

        static void transformMatrixToLocal(View view, Matrix matrix) {
            view.transformMatrixToLocal(matrix);
        }

        static void setAnimationMatrix(View view, Matrix matrix) {
            view.setAnimationMatrix(matrix);
        }
    }
}
