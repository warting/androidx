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

package androidx.xr.arcore.apps.whitebox.handtracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.arcore.Hand
import androidx.xr.arcore.apps.whitebox.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.common.SessionLifecycleHelper
import androidx.xr.runtime.Config
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/** Sample that demonstrates Hand Tracking API usage. */
class HandTrackingActivity : ComponentActivity() {

    enum class HandGesture {
        OTHER,
        THUMB,
        V,
        HEART,
    }

    private val gestureAnglesRange: Map<HandGesture, Array<Pair<Float, Float>>> =
        mapOf(
            HandGesture.THUMB to
                arrayOf(
                    Pair(0.98f, 1f), // [0]palm to wrist
                    Pair(0.41f, 0.88f), // [1]thumb to palm
                    Pair(0.9f, 1f), // [2]thumb
                    Pair(0.92f, 1f), // [3]
                    Pair(0.55f, 0.94f), // [4]thumb to index
                    Pair(-0.12f, 0.77f), // [5]index
                    Pair(-0.37f, 0.49f), // [6]
                    Pair(0.68f, 0.95f), // [7]
                    Pair(0.93f, 1f), // [8]index to middle
                    Pair(-0.08f, 0.73f), // [9]middle
                    Pair(-0.26f, 0.52f), // [10]
                    Pair(0.39f, 0.85f), // [11]
                    Pair(0.96f, 1f), // [12]middle to ring
                    Pair(-0.17f, 0.83f), // [13]ring
                    Pair(-0.44f, 0.41f), // [14]
                    Pair(0.52f, 0.87f), // [15]
                    Pair(0.94f, 1f), // [16]ring to little
                    Pair(-0.39f, 0.84f), // [17]little
                    Pair(-0.48f, 0.61f), // [18]
                    Pair(0.59f, 0.91f), // [19]
                ),
            HandGesture.V to
                arrayOf(
                    Pair(0.98f, 1f), // [0]palm to wrist
                    Pair(0.64f, 0.92f), // [1]thumb to palm
                    Pair(0.74f, 0.96f), // [2]thumb
                    Pair(0.88f, 0.99f), // [3]
                    Pair(0.57f, 0.95f), // [4]thumb to index
                    Pair(0.85f, 1f), // [5]index
                    Pair(0.92f, 1f), // [6]
                    Pair(0.98f, 1f), // [7]
                    Pair(0.6f, 0.99f), // [8]index to middle
                    Pair(0.37f, 0.99f), // [9]middle
                    Pair(0.12f, 1f), // [10]
                    Pair(0.66f, 1f), // [11]
                    Pair(0.4f, 1f), // [12]middle to ring
                    Pair(-0.06f, 0.91f), // [13]ring
                    Pair(-0.26f, 0.96f), // [14]
                    Pair(0.63f, 0.98f), // [15]
                    Pair(0.76f, 0.99f), // [16]ring to little
                    Pair(-0.47f, 0.78f), // [17]little
                    Pair(0.01f, 0.97f), // [18]
                    Pair(0.66f, 0.98f), // [19]
                ),
            HandGesture.HEART to
                arrayOf(
                    Pair(0.98f, 1f), // [0]palm to wrist
                    Pair(0.62f, 0.95f), // [1]thumb to palm
                    Pair(0.95f, 1f), // [2]thumb
                    Pair(0.88f, 0.99f), // [3]
                    Pair(0.58f, 0.99f), // [4]thumb to index
                    Pair(0.06f, 0.99f), // [5]index
                    Pair(-0.2f, 0.91f), // [6]
                    Pair(0.81f, 1f), // [7]
                    Pair(0.31f, 1f), // [8]index to middle
                    Pair(-0.12f, 0.79f), // [9]middle
                    Pair(-0.3f, 0.55f), // [10]
                    Pair(0.53f, 0.92f), // [11]
                    Pair(0.92f, 1f), // [12]middle to ring
                    Pair(-0.24f, 0.78f), // [13]ring
                    Pair(-0.25f, 0.51f), // [14]
                    Pair(0.58f, 0.90f), // [15]
                    Pair(0.93f, 1f), // [16]ring to little
                    Pair(-0.49f, 0.84f), // [17]little
                    Pair(-0.16f, 0.64f), // [18]
                    Pair(0.68f, 0.92f), // [19]
                ),
        )

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create session and renderers.
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(handTracking = Config.HandTrackingMode.BOTH),
                onSessionAvailable = { session ->
                    this.session = session

                    lifecycleScope.launch {
                        session.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            setContent { MainPanel(session) }
                            val xyzModel = GltfModel.create(session, "models/xyzArrows.glb").await()

                            val leftHandJointEntityMap =
                                HandJointType.entries.associateWith {
                                    GltfModelEntity.create(session, xyzModel).also {
                                        it.setScale(0.015f)
                                        it.setHidden(true)
                                    }
                                }

                            val rightHandJointEntityMap =
                                HandJointType.entries.associateWith {
                                    GltfModelEntity.create(session, xyzModel).also {
                                        it.setScale(0.015f)
                                        it.setHidden(true)
                                    }
                                }

                            launch {
                                Hand.left(session)?.state?.collect { leftHandState ->
                                    renderHandGizmos(leftHandState, leftHandJointEntityMap)
                                }
                            }

                            launch {
                                Hand.right(session)?.state?.collect { rightHandState ->
                                    renderHandGizmos(rightHandState, rightHandJointEntityMap)
                                }
                            }
                        }
                    }
                },
            )
        lifecycle.addObserver(sessionHelper)
    }

    private fun renderHandGizmos(
        handState: Hand.State,
        jointEntityMap: Map<HandJointType, GltfModelEntity>,
    ) {
        for ((jointType, gltfModelEntity) in jointEntityMap) {
            if (handState.trackingState == TrackingState.TRACKING) {
                // According to the experiment, calling setHidden will significantly
                // increase the latency. Thus, check the hidden state before calling
                // setHidden.
                if (gltfModelEntity.isHidden(false)) {
                    gltfModelEntity.setHidden(false)
                }
                val transformedPose =
                    session.scene.perceptionSpace.transformPoseTo(
                        handState.handJoints[jointType]!!,
                        session.scene.activitySpace,
                    )
                gltfModelEntity.setPose(transformedPose)
            } else {
                // According to the experiment, calling setHidden will significantly
                // increase the latency. Thus, check the hidden state before calling
                // setHidden.
                if (gltfModelEntity.isHidden(false)) {
                    return
                }
                gltfModelEntity.setHidden(true)
            }
        }
    }

    private fun deriveAngles(handJoints: Map<HandJointType, Pose>): FloatArray {
        val directions: Array<Vector3> =
            arrayOf(
                handJoints[HandJointType.PALM]!!.forward,
                handJoints[HandJointType.WRIST]!!.forward,
                handJoints[HandJointType.THUMB_METACARPAL]!!.forward,
                handJoints[HandJointType.THUMB_PROXIMAL]!!.forward,
                handJoints[HandJointType.THUMB_DISTAL]!!.forward,
                handJoints[HandJointType.INDEX_METACARPAL]!!.forward,
                handJoints[HandJointType.INDEX_PROXIMAL]!!.forward,
                handJoints[HandJointType.INDEX_INTERMEDIATE]!!.forward,
                handJoints[HandJointType.INDEX_DISTAL]!!.forward,
                handJoints[HandJointType.MIDDLE_METACARPAL]!!.forward,
                handJoints[HandJointType.MIDDLE_PROXIMAL]!!.forward,
                handJoints[HandJointType.MIDDLE_INTERMEDIATE]!!.forward,
                handJoints[HandJointType.MIDDLE_DISTAL]!!.forward,
                handJoints[HandJointType.RING_METACARPAL]!!.forward,
                handJoints[HandJointType.RING_PROXIMAL]!!.forward,
                handJoints[HandJointType.RING_INTERMEDIATE]!!.forward,
                handJoints[HandJointType.RING_DISTAL]!!.forward,
                handJoints[HandJointType.LITTLE_METACARPAL]!!.forward,
                handJoints[HandJointType.LITTLE_PROXIMAL]!!.forward,
                handJoints[HandJointType.LITTLE_INTERMEDIATE]!!.forward,
                handJoints[HandJointType.LITTLE_DISTAL]!!.forward,
            )
        return floatArrayOf(
            // palm to wrist
            directions[0].dot(directions[1]),
            // thumb to plam
            directions[0].dot(directions[2]),
            // thumb
            directions[2].dot(directions[3]),
            directions[3].dot(directions[4]),
            // thumb to index
            directions[2].dot(directions[5]),
            // index
            directions[5].dot(directions[6]),
            directions[6].dot(directions[7]),
            directions[7].dot(directions[8]),
            // index to middle
            directions[8].dot(directions[10]),
            // middle
            directions[9].dot(directions[10]),
            directions[10].dot(directions[11]),
            directions[11].dot(directions[12]),
            // middle to ring
            directions[10].dot(directions[14]),
            // ring
            directions[13].dot(directions[14]),
            directions[14].dot(directions[15]),
            directions[15].dot(directions[16]),
            // ring to little
            directions[14].dot(directions[18]),
            // little
            directions[17].dot(directions[18]),
            directions[18].dot(directions[19]),
            directions[19].dot(directions[20]),
        )
    }

    private fun guessGesture(angleData: FloatArray): HandGesture {
        return gestureAnglesRange.entries
            .firstOrNull { (_, ranges) ->
                angleData.indices.all { i ->
                    val (min, max) = ranges[i]
                    angleData[i] in min..max
                }
            }
            ?.key ?: HandGesture.OTHER
    }

    @Composable
    private fun MainPanel(session: Session) {
        val state by session.state.collectAsStateWithLifecycle()

        val leftHand = Hand.left(session)
        val rightHand = Hand.right(session)

        Column(
            modifier =
                Modifier.background(color = Color.White)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp)
        ) {
            BackToMainActivityButton()
            Text(text = "CoreState: ${state.timeMark.toString()}")
            if (leftHand == null || rightHand == null) {
                Text("Hand module is not supported.")
            } else {
                val handedness = Hand.getHandedness(contentResolver)
                Text("Handedness: ${handedness}")
                val leftHandState = leftHand.state.collectAsState().value
                val rightHandState = rightHand.state.collectAsState().value
                Text("Left hand tracking state: ${leftHandState.trackingState}")
                if (leftHandState.trackingState == TrackingState.TRACKING) {
                    val angles = deriveAngles(leftHandState.handJoints)
                    Text("Left hand gesture detected: ${guessGesture(angles)}")
                }
                Text("Right hand tracking state: ${rightHandState.trackingState}")
                if (rightHandState.trackingState == TrackingState.TRACKING) {
                    val angles = deriveAngles(rightHandState.handJoints)
                    Text("Right hand gesture detected: ${guessGesture(angles)}")
                }
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "PersistentAnchorsActivity"
    }
}
