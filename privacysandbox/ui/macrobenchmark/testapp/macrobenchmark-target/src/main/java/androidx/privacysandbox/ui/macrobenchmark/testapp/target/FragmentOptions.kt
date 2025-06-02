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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target

import android.os.Bundle
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.FragmentOption
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.UiFrameworkOption
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.ViewabilityOption
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.ZOrderOption

data class FragmentOptions(
    @FragmentOption val cujType: Int,
    @MediationOption val mediation: Int,
    @AdType val adType: Int,
    @ZOrderOption val isZOrderOnTop: Boolean,
    @ViewabilityOption val drawViewability: Boolean,
    @UiFrameworkOption val uiFramework: Int,
) {

    private var fragment: BaseFragment = deriveFragment()

    companion object {
        const val KEY_FRAGMENT = "fragment"
        const val FRAGMENT_RESIZE = "resize" // default
        const val FRAGMENT_RESIZE_HIDDEN = "resize-hidden"
        const val FRAGMENT_SCROLL = "scroll"
        const val FRAGMENT_POOLING_CONTAINER = "pooling-container"
        const val FRAGMENT_OCCLUSIONS_HIDDEN = "occlusions-hidden"

        const val KEY_MEDIATION = "mediation"
        const val MEDIATION_TYPE_NON_MEDIATED = "non-mediated" // default
        const val MEDIATION_TYPE_IN_APP = "in-app"
        const val MEDIATION_TYPE_IN_RUNTIME = "in-runtime"
        const val MEDIATION_TYPE_IN_RUNTIME_WITH_OVERLAY = "in-runtime-with-overlay"
        const val MEDIATION_TYPE_REFRESHABLE = "refreshable"

        const val KEY_AD_TYPE = "ad-type"
        const val AD_TYPE_NON_WEBVIEW = "non-webview" // default
        const val AD_TYPE_BASIC_WEBVIEW = "basic-webview"
        const val AD_TYPE_WEBVIEW_FROM_ASSETS = "webview-from-assets"
        const val AD_TYPE_VIDEO = "video"

        const val KEY_Z_ORDER = "z-order"
        const val Z_ORDER_ABOVE = "above" // default
        const val Z_ORDER_BELOW = "below"

        const val KEY_DRAW_VIEWABILITY = "draw-viewability" // default is false

        const val KEY_UI_FRAMEWORK = "ui-framework"
        const val UI_FRAMEWORK_VIEW = "view" // default
        const val UI_FRAMEWORK_COMPOSE = "compose"

        const val LOAD_SDK_COMPLETE = "loadSdkComplete"

        fun createFromIntentExtras(extras: Bundle): FragmentOptions {
            val fragmentExtra = extras.getString(KEY_FRAGMENT)
            val fragment =
                when (fragmentExtra) {
                    FRAGMENT_RESIZE -> FragmentOption.RESIZE
                    FRAGMENT_RESIZE_HIDDEN -> FragmentOption.RESIZE_HIDDEN
                    FRAGMENT_POOLING_CONTAINER -> FragmentOption.POOLING_CONTAINER
                    FRAGMENT_SCROLL -> FragmentOption.SCROLL
                    FRAGMENT_OCCLUSIONS_HIDDEN -> FragmentOption.OCCLUSIONS_HIDDEN
                    else -> FragmentOption.RESIZE
                }
            val mediationExtra = extras.getString(KEY_MEDIATION)
            val mediation =
                when (mediationExtra) {
                    MEDIATION_TYPE_NON_MEDIATED -> MediationOption.NON_MEDIATED
                    MEDIATION_TYPE_IN_APP -> MediationOption.IN_APP_MEDIATEE
                    MEDIATION_TYPE_IN_RUNTIME -> MediationOption.SDK_RUNTIME_MEDIATEE
                    MEDIATION_TYPE_IN_RUNTIME_WITH_OVERLAY ->
                        MediationOption.SDK_RUNTIME_MEDIATEE_WITH_OVERLAY
                    MEDIATION_TYPE_REFRESHABLE -> MediationOption.REFRESHABLE_MEDIATION
                    else -> MediationOption.NON_MEDIATED
                }
            val adTypeExtra = extras.getString(KEY_AD_TYPE)
            val adType =
                when (adTypeExtra) {
                    AD_TYPE_NON_WEBVIEW -> AdType.BASIC_NON_WEBVIEW
                    AD_TYPE_BASIC_WEBVIEW -> AdType.BASIC_WEBVIEW
                    AD_TYPE_WEBVIEW_FROM_ASSETS -> AdType.WEBVIEW_FROM_LOCAL_ASSETS
                    AD_TYPE_VIDEO -> AdType.NON_WEBVIEW_VIDEO
                    else -> AdType.BASIC_NON_WEBVIEW
                }
            val zOrderExtra = extras.getString(KEY_Z_ORDER)
            val zOrder =
                when (zOrderExtra) {
                    Z_ORDER_ABOVE -> ZOrderOption.Z_ABOVE
                    Z_ORDER_BELOW -> ZOrderOption.Z_BELOW
                    else -> ZOrderOption.Z_ABOVE
                }
            val viewabilityOption = extras.getBoolean(KEY_DRAW_VIEWABILITY, false)
            val drawViewability =
                if (viewabilityOption) {
                    ViewabilityOption.DRAW
                } else {
                    ViewabilityOption.DO_NOT_DRAW
                }
            val uiFrameworkExtra = extras.getString(KEY_UI_FRAMEWORK)
            val uiFramework =
                when (uiFrameworkExtra) {
                    UI_FRAMEWORK_VIEW -> UiFrameworkOption.VIEW
                    UI_FRAMEWORK_COMPOSE -> UiFrameworkOption.COMPOSE
                    else -> UiFrameworkOption.VIEW
                }
            return FragmentOptions(
                fragment,
                mediation,
                adType,
                zOrder,
                drawViewability,
                uiFramework,
            )
        }
    }

    /**
     * Returns the fragment that this [FragmentOptions] describes. This is dictated by the fragment
     * option and the UI framework option.
     */
    fun getFragment(): BaseFragment {
        return fragment
    }

    private fun deriveFragment(): BaseFragment {
        return when (uiFramework) {
            UiFrameworkOption.VIEW ->
                when (cujType) {
                    FragmentOption.SCROLL -> ScrollFragment()
                    FragmentOption.RESIZE -> ResizeFragment()
                    FragmentOption.POOLING_CONTAINER -> PoolingContainerFragment()
                    FragmentOption.RESIZE_HIDDEN -> ResizeHiddenFragment()
                    FragmentOption.OCCLUSIONS_HIDDEN -> OcclusionFragment()
                    else -> ResizeFragment()
                }
            UiFrameworkOption.COMPOSE ->
                when (cujType) {
                    FragmentOption.SCROLL -> ScrollComposeFragment()
                    FragmentOption.RESIZE -> ResizeComposeFragment()
                    FragmentOption.POOLING_CONTAINER -> LazyListFragment()
                    else -> ResizeComposeFragment()
                }
            else -> ResizeFragment()
        }
    }
}
