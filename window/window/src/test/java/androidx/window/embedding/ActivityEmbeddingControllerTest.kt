/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.embedding

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.core.util.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** The unit tests for [ActivityEmbeddingController]. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityEmbeddingControllerTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var mockEmbeddingBackend: EmbeddingBackend
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var activityEmbeddingController: ActivityEmbeddingController

    @Before
    fun setUp() {
        mockEmbeddingBackend = mock()
        activityEmbeddingController = ActivityEmbeddingController(mockEmbeddingBackend)

        mockContext = mock()
        mockActivity = mock()
        whenever(mockActivity.applicationContext).doReturn(mockContext)
    }

    @Test
    fun testIsActivityEmbedded() {
        whenever(mockEmbeddingBackend.isActivityEmbedded(mockActivity)).thenReturn(true)

        assertTrue(activityEmbeddingController.isActivityEmbedded(mockActivity))

        whenever(mockEmbeddingBackend.isActivityEmbedded(mockActivity)).thenReturn(false)

        assertFalse(activityEmbeddingController.isActivityEmbedded(mockActivity))
    }

    @Test
    fun testGetActivityStack() {
        val activityStack = ActivityStack(listOf(), true)
        whenever(mockEmbeddingBackend.getActivityStack(mockActivity)).thenReturn(activityStack)

        assertEquals(activityStack, activityEmbeddingController.getActivityStack(mockActivity))
    }

    @Test
    fun testFinishActivityStacks() {
        val activityStacks: Set<ActivityStack> = mock()
        activityEmbeddingController.finishActivityStacks(activityStacks)

        verify(mockEmbeddingBackend).finishActivityStacks(activityStacks)
    }

    @Test
    fun test_invalidateTopVisibleSplitAttributes_delegates() {
        activityEmbeddingController.invalidateVisibleActivityStacks()
        verify(mockEmbeddingBackend).invalidateVisibleActivityStacks()
    }

    @Test
    fun test_embeddedActivityWindowInfo_delegates() =
        testScope.runTest {
            val expectedInfo =
                EmbeddedActivityWindowInfo(
                    isEmbedded = true,
                    parentHostBounds = Rect(0, 0, 1000, 2000),
                    boundsInParentHost = Rect(0, 0, 500, 2000),
                )
            doAnswer { invocationOnMock ->
                    @Suppress("UNCHECKED_CAST")
                    val callback =
                        invocationOnMock.arguments.last() as Consumer<EmbeddedActivityWindowInfo>
                    callback.accept(expectedInfo)
                }
                .whenever(mockEmbeddingBackend)
                .addEmbeddedActivityWindowInfoCallbackForActivity(any(), any())

            val actualInfo =
                activityEmbeddingController
                    .embeddedActivityWindowInfo(mockActivity)
                    .take(1)
                    .toList()
                    .first()

            assertEquals(expectedInfo, actualInfo)
            verify(mockEmbeddingBackend)
                .addEmbeddedActivityWindowInfoCallbackForActivity(eq(mockActivity), any())
            verify(mockEmbeddingBackend).removeEmbeddedActivityWindowInfoCallbackForActivity(any())
        }

    @Test
    fun testGetInstance() {
        EmbeddingBackend.overrideDecorator(
            object : EmbeddingBackendDecorator {
                override fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend =
                    mockEmbeddingBackend
            }
        )
        val controller = ActivityEmbeddingController.getInstance(mockActivity)
        val activityStacks: Set<ActivityStack> = mock()

        controller.finishActivityStacks(activityStacks)

        verify(mockEmbeddingBackend).finishActivityStacks(activityStacks)

        EmbeddingBackend.reset()
    }
}
