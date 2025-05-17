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

package androidx.wear.phone.interactions.notifications

import android.content.Context
import android.content.pm.PackageManager
import androidx.wear.phone.interactions.WearPhoneInteractionsTestRunner
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [BridgingManager]. */
@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument // Needed because it is defined in the "android" package.
public class BridgingManagerTest {

    private val mContext: Context = mock(Context::class.java)
    private val packageManager: PackageManager = mock(PackageManager::class.java)

    init {
        `when`(mContext.packageName).thenReturn(PACKAGE_NAME)
        `when`(mContext.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true)
    }

    @Test
    public fun disableBridging() {
        val bridgingConfig = BridgingConfig.Builder(mContext, false).build()

        assertThat(bridgingConfig).isEqualTo(BridgingConfig(PACKAGE_NAME, false, HashSet()))

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    @Test
    public fun enableBridging() {
        val bridgingConfig = BridgingConfig.Builder(mContext, true).build()

        assertThat(bridgingConfig).isEqualTo(BridgingConfig(PACKAGE_NAME, true, HashSet()))

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    @Test
    public fun bridgingEnableByDefault() {
        val bridgingConfig = BridgingConfig.Builder(mContext, true).build()

        assertThat(bridgingConfig).isEqualTo(BridgingConfig(PACKAGE_NAME, true, HashSet()))

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    @Test
    public fun addTagsWithoutSettingBridging() {
        val bridgingConfig = BridgingConfig.Builder(mContext, true).addExcludedTag("foo").build()

        assertThat(bridgingConfig)
            .isEqualTo(BridgingConfig(PACKAGE_NAME, true, HashSet(listOf("foo"))))

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    @Test
    public fun disableBridgingWithTagsInSeparateCalls() {
        val bridgingConfig =
            BridgingConfig.Builder(mContext, false)
                .addExcludedTag("foo")
                .addExcludedTag("bar")
                .addExcludedTag("foo")
                .build()

        assertThat(bridgingConfig)
            .isEqualTo(BridgingConfig(PACKAGE_NAME, false, HashSet(Arrays.asList("foo", "bar"))))

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    @Test
    public fun disableBridgingWithTagsInOneCall() {
        val bridgingConfig =
            BridgingConfig.Builder(mContext, false)
                .addExcludedTags(Arrays.asList("foo", "bar", "foo"))
                .build()

        assertThat(bridgingConfig)
            .isEqualTo(BridgingConfig(PACKAGE_NAME, false, HashSet(Arrays.asList("foo", "bar"))))

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    @Test
    public fun disableBridgingWithTagsInMixOfCalls() {
        val bridgingConfig =
            BridgingConfig.Builder(mContext, false)
                .addExcludedTag("123")
                .addExcludedTags(Arrays.asList("foo", "bar", "foo"))
                .addExcludedTags(Arrays.asList("foo", "bar", "abc"))
                .addExcludedTag("aaa")
                .addExcludedTag("foo")
                .build()

        assertThat(bridgingConfig)
            .isEqualTo(
                BridgingConfig(
                    PACKAGE_NAME,
                    false,
                    HashSet(Arrays.asList("foo", "bar", "123", "aaa", "abc")),
                )
            )

        // Test that conversion to and from bundle works as expected.
        assertThat(BridgingConfig.fromBundle(bridgingConfig.toBundle(mContext)))
            .isEqualTo(bridgingConfig)
    }

    private companion object {
        private const val PACKAGE_NAME = "foo_package"
    }
}
