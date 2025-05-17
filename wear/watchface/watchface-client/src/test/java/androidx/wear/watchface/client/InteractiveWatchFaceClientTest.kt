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

package androidx.wear.watchface.client

import android.os.IBinder
import androidx.wear.watchface.control.IInteractiveWatchFace
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(ClientTestRunner::class)
class InteractiveWatchFaceClientTest {
    private val iInteractiveWatchFace = mock<IInteractiveWatchFace>()
    private val iBinder = mock<IBinder>()

    init {
        `when`(iInteractiveWatchFace.asBinder()).thenReturn(iBinder)
    }

    @Test
    public fun sendDisconnectNotification() {
        val client =
            InteractiveWatchFaceClientImpl(
                iInteractiveWatchFace,
                previewImageUpdateRequestedExecutor = null,
                previewImageUpdateRequestedListener = null,
            )

        val listener = mock<InteractiveWatchFaceClient.ClientDisconnectListener>()
        client.addClientDisconnectListener(listener, { it.run() })

        // Simulate multiple disconnect notifications.
        client.sendDisconnectNotification(DisconnectReasons.ENGINE_DETACHED)
        client.sendDisconnectNotification(DisconnectReasons.ENGINE_DIED)

        // But only one should be sent to the listener.
        verify(listener, times(1)).onClientDisconnected(any())
    }

    @Test
    fun renderWatchFaceToSurfaceSupported_oldApi() {
        `when`(iInteractiveWatchFace.apiVersion).thenReturn(8)
        val client =
            InteractiveWatchFaceClientImpl(
                iInteractiveWatchFace,
                previewImageUpdateRequestedExecutor = null,
                previewImageUpdateRequestedListener = null,
            )

        Assert.assertFalse(client.isRemoteWatchFaceViewHostSupported)
    }

    @Test
    fun renderWatchFaceToSurfaceSupported_currentApi() {
        `when`(iInteractiveWatchFace.apiVersion).thenReturn(IInteractiveWatchFace.API_VERSION)
        val client =
            InteractiveWatchFaceClientImpl(
                iInteractiveWatchFace,
                previewImageUpdateRequestedExecutor = null,
                previewImageUpdateRequestedListener = null,
            )

        Assert.assertTrue(client.isRemoteWatchFaceViewHostSupported)
    }
}
