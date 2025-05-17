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

package androidx.glance.appwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.junit.Test

@SdkSuppress(minSdkVersion = 26)
class CoroutineBroadcastReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private class TestBroadcast : BroadcastReceiver() {
        val extraValue = AtomicReference("")
        val broadcastExecuted = CountDownLatch(1)
        val coroutineScopeUsed = AtomicReference<CoroutineScope>(null)

        override fun onReceive(context: Context, intent: Intent) {
            goAsync {
                coroutineScopeUsed.set(this)
                extraValue.set(intent.getStringExtra(EXTRA_STRING))
                broadcastExecuted.countDown()
                // Test throwing an error directly in goAsync's job. The error should be caught,
                // logged, and cancel the job/scope, without crashing the process.
                error("This error should be caught and logged.")
            }
        }
    }

    @MediumTest
    @Test
    fun onReceive() {
        val broadcastReceiver = TestBroadcast()

        if (android.os.Build.VERSION.SDK_INT < 33) {
            context.registerReceiver(broadcastReceiver, IntentFilter(BROADCAST_ACTION))
        } else {
            context.registerReceiver(
                broadcastReceiver,
                IntentFilter(BROADCAST_ACTION),
                RECEIVER_NOT_EXPORTED,
            )
        }

        val value = "value"
        context.sendBroadcast(
            Intent(BROADCAST_ACTION)
                .setPackage(context.packageName)
                .putExtra(EXTRA_STRING, value)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        )
        waitForBroadcastIdle()

        assertWithMessage("Broadcast receiver did not execute")
            .that(broadcastReceiver.broadcastExecuted.await(5, TimeUnit.SECONDS))
            .isTrue()
        waitFor(Duration.ofSeconds(5)) { !broadcastReceiver.coroutineScopeUsed.get().isActive }
        assertWithMessage("Coroutine scope did not get cancelled")
            .that(broadcastReceiver.coroutineScopeUsed.get().isActive)
            .isFalse()
        assertThat(broadcastReceiver.extraValue.get()).isEqualTo(value)
    }

    private fun waitFor(timeout: Duration, condition: () -> Boolean) {
        val start = Instant.now()
        val sleepMs = min(500, timeout.toMillis() / 10)
        while (Duration.between(start, Instant.now()) < timeout) {
            if (condition()) return
            Thread.sleep(sleepMs)
        }
    }

    private companion object {
        const val BROADCAST_ACTION = "androidx.glance.appwidget.utils.TEST_ACTION"
        const val EXTRA_STRING = "extra_string"
    }
}
