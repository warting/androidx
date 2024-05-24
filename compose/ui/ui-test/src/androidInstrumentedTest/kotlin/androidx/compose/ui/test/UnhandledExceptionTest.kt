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

package androidx.compose.ui.test

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.forEach
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class UnhandledExceptionTest(private val activityClass: Class<out ComponentActivity>) {

    // Expect all tests in this suite to throw an ExpectedException. If they do, catch it and pass
    // the test. If they throw a different exception or no exception, fail the test with an
    // AssertionError.
    @get:Rule
    val exceptionAssertionRule: TestRule = ExceptionTestRule { throwable ->
        if (!throwable.isOrHasCauseOf<ExpectedException>()) {
            fail(
                "Expected test to fail with a thrown instance of ExpectedException, " +
                    "but instead got ${throwable::class.qualifiedName} with stacktrace:" +
                    "\n${throwable.stackTraceToString()}\n[end of comparison]\n\n"
            )
        }
    }

    @Test
    fun throwDuringCompose() =
        runAndroidComposeUiTest(activityClass) {
            installContent { throwExpectedException() }

            waitForIdle()
        }

    @Test
    fun throwDuringRecompose() =
        runAndroidComposeUiTest(activityClass) {
            var state by mutableIntStateOf(0)
            installContent {
                if (state == 1) throwExpectedException()

                Button(onClick = { state = 1 }) { Text("throw") }
            }

            onNodeWithText("throw").performClick()
            waitForIdle()
        }

    @Test
    fun throwDuringInteraction() =
        runAndroidComposeUiTest(activityClass) {
            installContent { Button(onClick = { throwExpectedException() }) { Text("throw") } }

            onNodeWithText("throw").performClick()
        }

    @Test
    fun throwDuringMeasure() =
        runAndroidComposeUiTest(activityClass) {
            installContent { Box(modifier = Modifier.layout { _, _ -> throwExpectedException() }) }

            waitForIdle()
        }

    @Test
    fun throwDuringDraw() =
        runAndroidComposeUiTest(activityClass) {
            installContent {
                Box(modifier = Modifier.fillMaxSize().drawWithContent { throwExpectedException() })
            }

            waitForIdle()
        }

    @Test
    fun throwDuringTeardown() =
        runAndroidComposeUiTest(activityClass) {
            installContent { DisposableEffect(Unit) { onDispose { throwExpectedException() } } }

            waitForIdle()
        }

    private fun AndroidComposeUiTest<*>.installContent(content: @Composable () -> Unit) {
        when (val activity = activity) {
            is CustomComposeHostActivity ->
                runOnUiThread { activity.findViewByClass<ComposeView>()!!.setContent(content) }
            else -> setContent(content)
        }
    }

    private inline fun <reified V : View> Activity.findViewByClass(): V? {
        return findViewById<ViewGroup>(android.R.id.content).findViewByClass(V::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : View> ViewGroup.findViewByClass(clazz: Class<V>): V? {
        forEach { childView ->
            if (clazz.isInstance(childView)) {
                return childView as V
            } else if (childView is ViewGroup) {
                childView.findViewByClass(clazz)?.let {
                    return it
                }
            }
        }
        return null
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet() =
            listOf(ComponentActivity::class.java, CustomComposeHostActivity::class.java)
    }
}

private class ExceptionTestRule(private val assertion: (throwable: Throwable) -> Unit) : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base?.evaluate()
                } catch (throwable: Throwable) {
                    assertion(throwable)
                    return
                }

                fail("Expected test to fail with an exception, but no exception was thrown.")
            }
        }
    }
}

private fun throwExpectedException(): Nothing = throw ExpectedException()

private class ExpectedException : Throwable()

private inline fun <reified T> Throwable.isOrHasCauseOf(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) return true
        current = current.cause
    }
    return false
}
