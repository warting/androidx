/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("MathAssertions")

package androidx.xr.runtime.testing.math

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat

/**
 * Asserts that two [Vector3]s are equal.
 *
 * @param actual the actual [Vector3] to compare
 * @param expected the expected [Vector3] to compare
 */
@JvmOverloads
public fun assertVector3(actual: Vector3, expected: Vector3, EPSILON: Float = 1e-5f) {
    assertThat(actual.x).isWithin(EPSILON).of(expected.x)
    assertThat(actual.y).isWithin(EPSILON).of(expected.y)
    assertThat(actual.z).isWithin(EPSILON).of(expected.z)
}

/**
 * Asserts that two [Quaternion]s are equal.
 *
 * @param actual the actual [Quaternion] to compare
 * @param expected the expected [Quaternion] to compare
 */
@JvmOverloads
public fun assertRotation(actual: Quaternion, expected: Quaternion, EPSILON: Float = 1e-5f) {
    val dot = Math.abs(actual.toNormalized().dot(expected.toNormalized()))
    assertThat(dot).isWithin(EPSILON).of(1.0f)
}

/**
 * Asserts that two [Pose]s are equal.
 *
 * @param actual the actual [Pose] to compare
 * @param expected the expected [Pose] to compare
 */
@JvmOverloads
public fun assertPose(actual: Pose, expected: Pose, EPSILON: Float = 1e-5f) {
    assertVector3(actual.translation, expected.translation, EPSILON)
    assertRotation(actual.rotation, expected.rotation, EPSILON)
}
