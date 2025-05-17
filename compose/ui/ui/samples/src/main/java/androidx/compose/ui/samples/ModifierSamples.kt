/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("unused")

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Sampled
@Composable
fun ModifierUsageSample() {
    Text(
        "Hello, World!",
        Modifier.padding(16.dp) // Outer padding; outside background
            .background(color = Color.Green) // Solid element background color
            .padding(16.dp), // Inner padding; inside background, around text
    )
}

@Sampled
@Composable
fun ModifierFactorySample() {
    class FancyModifier(val level: Float) : Modifier.Element

    fun Modifier.fancy(level: Float) = this.then(FancyModifier(level))

    Row(Modifier.fancy(1f).padding(10.dp)) {
        // content
    }
}

@Sampled
@Composable
fun ModifierParameterSample() {
    @Composable
    fun PaddedColumn(modifier: Modifier = Modifier) {
        Column(modifier.padding(10.dp)) {
            // ...
        }
    }
}

@Sampled
@Composable
fun SubcomponentModifierSample() {
    @Composable
    fun ButtonBar(
        onOk: () -> Unit,
        onCancel: () -> Unit,
        modifier: Modifier = Modifier,
        buttonModifier: Modifier = Modifier,
    ) {
        Row(modifier) {
            Button(onCancel, buttonModifier) { Text("Cancel") }
            Button(onOk, buttonModifier) { Text("Ok") }
        }
    }
}

@Sampled
@Composable
fun DelegatedNodeSampleExplicit() {
    class TapGestureNode(var onTap: () -> Unit) : PointerInputModifierNode, Modifier.Node() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            // ...
        }

        override fun onCancelPointerInput() {
            // ...
        }
    }
    class TapGestureWithClickSemantics(onTap: () -> Unit) :
        PointerInputModifierNode, SemanticsModifierNode, DelegatingNode() {
        var onTap: () -> Unit
            get() = gesture.onTap
            set(value) {
                gesture.onTap = value
            }

        val gesture = delegate(TapGestureNode(onTap))

        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            gesture.onPointerEvent(pointerEvent, pass, bounds)
        }

        override fun onCancelPointerInput() {
            gesture.onCancelPointerInput()
        }

        override fun SemanticsPropertyReceiver.applySemantics() {
            onClick {
                gesture.onTap()
                true
            }
        }
    }
}

@Sampled
@Composable
fun DelegatedNodeSampleImplicit() {
    class TapGestureNode(var onTap: () -> Unit) : PointerInputModifierNode, Modifier.Node() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            // ...
        }

        override fun onCancelPointerInput() {
            // ...
        }
    }

    class TapSemanticsNode(var onTap: () -> Unit) : SemanticsModifierNode, Modifier.Node() {
        override fun SemanticsPropertyReceiver.applySemantics() {
            onClick {
                onTap()
                true
            }
        }
    }
    class TapGestureWithClickSemantics(onTap: () -> Unit) : DelegatingNode() {
        var onTap: () -> Unit
            get() = gesture.onTap
            set(value) {
                gesture.onTap = value
                semantics.onTap = value
            }

        val gesture = delegate(TapGestureNode(onTap))
        val semantics = delegate(TapSemanticsNode(onTap))
    }
}

@Sampled
@Composable
fun LazyDelegationExample() {
    class ExpensivePositionHandlingOnPointerEvents : PointerInputModifierNode, DelegatingNode() {

        val globalAwareNode =
            object : GlobalPositionAwareModifierNode, Modifier.Node() {
                override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
                    // ...
                }
            }

        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            // wait until first pointer event to start listening to global
            // position
            if (!globalAwareNode.isAttached) {
                delegate(globalAwareNode)
            }
            // normal input processing
        }

        override fun onCancelPointerInput() {
            // ...
        }
    }

    class TapGestureNode(var onTap: () -> Unit) : PointerInputModifierNode, Modifier.Node() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            // ...
        }

        override fun onCancelPointerInput() {
            // ...
        }
    }

    class TapSemanticsNode(var onTap: () -> Unit) : SemanticsModifierNode, Modifier.Node() {
        override fun SemanticsPropertyReceiver.applySemantics() {
            onClick {
                onTap()
                true
            }
        }
    }
    class TapGestureWithClickSemantics(onTap: () -> Unit) : DelegatingNode() {
        var onTap: () -> Unit
            get() = gesture.onTap
            set(value) {
                gesture.onTap = value
                semantics.onTap = value
            }

        val gesture = delegate(TapGestureNode(onTap))
        val semantics = delegate(TapSemanticsNode(onTap))
    }
}

@Sampled
fun ConditionalDelegationExample() {
    class MyModifierNode(global: Boolean) : DelegatingNode() {
        val globalAwareNode =
            object : GlobalPositionAwareModifierNode, Modifier.Node() {
                    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
                        // ...
                    }
                }
                .also { if (global) delegate(it) }
        var global: Boolean = global
            set(value) {
                if (global && !value) {
                    undelegate(globalAwareNode)
                } else if (!global && value) {
                    delegate(globalAwareNode)
                }
                field = value
            }
    }
}

@Sampled
fun DelegateInAttachSample() {
    class MyModifierNode : DelegatingNode() {
        val globalAwareNode =
            object : GlobalPositionAwareModifierNode, Modifier.Node() {
                override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
                    // ...
                }
            }

        override fun onAttach() {
            // one can conditionally delegate in attach, for instance if certain conditions are met
            if (requireLayoutDirection() == LayoutDirection.Rtl) {
                delegate(globalAwareNode)
            }
        }
    }
}

@Sampled
@Composable
fun ModifierNodeElementSample() {
    class Circle(var color: Color) : DrawModifierNode, Modifier.Node() {
        override fun ContentDrawScope.draw() {
            drawCircle(color)
        }
    }
    data class CircleElement(val color: Color) : ModifierNodeElement<Circle>() {
        override fun create() = Circle(color)

        override fun update(node: Circle) {
            node.color = color
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "circle"
            properties["color"] = color
        }
    }
    fun Modifier.circle(color: Color) = this then CircleElement(color)
}

@Suppress("LocalVariableName")
@Sampled
@Composable
fun SemanticsModifierNodeSample() {
    class HeadingNode : SemanticsModifierNode, Modifier.Node() {
        override fun SemanticsPropertyReceiver.applySemantics() {
            heading()
        }
    }

    val HeadingElement =
        object : ModifierNodeElement<HeadingNode>() {
            override fun create() = HeadingNode()

            override fun update(node: HeadingNode) {
                // Nothing to update.
            }

            override fun InspectorInfo.inspectableProperties() {
                name = "heading"
            }

            override fun hashCode(): Int = "heading".hashCode()

            override fun equals(other: Any?) = (other === this)
        }

    fun Modifier.heading() = this then HeadingElement
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun PointerInputModifierNodeSample() {
    class OnPointerEventNode(var callback: (PointerEvent) -> Unit) :
        PointerInputModifierNode, Modifier.Node() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            if (pass == PointerEventPass.Initial) {
                callback(pointerEvent)
            }
        }

        override fun onCancelPointerInput() {
            // Do nothing
        }
    }

    data class PointerInputElement(val callback: (PointerEvent) -> Unit) :
        ModifierNodeElement<OnPointerEventNode>() {
        override fun create() = OnPointerEventNode(callback)

        override fun update(node: OnPointerEventNode) {
            node.callback = callback
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "onPointerEvent"
            properties["callback"] = callback
        }
    }

    fun Modifier.onPointerEvent(callback: (PointerEvent) -> Unit) =
        this then PointerInputElement(callback)
}

@Sampled
@Composable
fun LayoutAwareModifierNodeSample() {
    class SizeLoggerNode(var id: String) : LayoutAwareModifierNode, Modifier.Node() {
        override fun onRemeasured(size: IntSize) {
            println("The size of $id was $size")
        }
    }

    data class LogSizeElement(val id: String) : ModifierNodeElement<SizeLoggerNode>() {
        override fun create(): SizeLoggerNode = SizeLoggerNode(id)

        override fun update(node: SizeLoggerNode) {
            node.id = id
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "logSize"
            properties["id"] = id
        }
    }

    fun Modifier.logSize(id: String) = this then LogSizeElement(id)
}

@Sampled
@Composable
fun GlobalPositionAwareModifierNodeSample() {
    class PositionLoggerNode(var id: String) : GlobalPositionAwareModifierNode, Modifier.Node() {
        override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
            // This will be the size of the Layout.
            coordinates.size
            // The position of the Layout relative to the application window.
            coordinates.positionInWindow()
            // The position of the Layout relative to the Compose root.
            coordinates.positionInRoot()
            // These will be the alignment lines provided to the Layout
            coordinates.providedAlignmentLines
            // This will be a LayoutCoordinates instance corresponding to the parent of the Layout.
            coordinates.parentLayoutCoordinates
        }
    }

    data class PositionLoggerElement(val id: String) : ModifierNodeElement<PositionLoggerNode>() {
        override fun create() = PositionLoggerNode(id)

        override fun update(node: PositionLoggerNode) {
            node.id = id
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "logPosition"
            properties["id"] = id
        }
    }

    fun Modifier.logPosition(id: String) = this then PositionLoggerElement(id)
}

@Sampled
@Composable
fun JustReadingOrProvidingModifierLocalNodeSample() {
    class Logger {
        fun log(string: String) {
            println(string)
        }
    }

    val loggerLocal = modifierLocalOf { Logger() }

    class ProvideLoggerNode(logger: Logger) : ModifierLocalModifierNode, Modifier.Node() {
        override val providedValues = modifierLocalMapOf(loggerLocal to logger)
    }

    data class ProvideLoggerElement(val logger: Logger) : ModifierNodeElement<ProvideLoggerNode>() {
        override fun create() = ProvideLoggerNode(logger)

        override fun update(node: ProvideLoggerNode) {
            node.provide(loggerLocal, logger)
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "provideLogger"
            properties["logger"] = logger
        }
    }

    class SizeLoggerNode(var id: String) :
        ModifierLocalModifierNode, LayoutAwareModifierNode, Modifier.Node() {
        override fun onRemeasured(size: IntSize) {
            loggerLocal.current.log("The size of $id was $size")
        }
    }

    data class SizeLoggerElement(val id: String) : ModifierNodeElement<SizeLoggerNode>() {
        override fun create() = SizeLoggerNode(id)

        override fun update(node: SizeLoggerNode) {
            node.id = id
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "logSize"
            properties["id"] = id
        }
    }

    fun Modifier.logSize(id: String) = this then SizeLoggerElement(id)
    fun Modifier.provideLogger(logger: Logger) = this then ProvideLoggerElement(logger)
}

@Sampled
@Composable
fun ModifierNodeResetSample() {
    class SelectableNode : Modifier.Node() {
        var selected by mutableStateOf(false)

        override fun onReset() {
            // reset `selected` to the initial value as if the node will be reused for
            // displaying different content it shouldn't be selected straight away.
            selected = false
        }

        // some logic which sets `selected` to true when it is selected
    }
}

@Sampled
@Composable
fun ModifierNodeCoroutineScopeSample() {
    class AnimatedNode : Modifier.Node() {
        val animatable = Animatable(0f)

        override fun onAttach() {
            coroutineScope.launch { animatable.animateTo(1f) }
        }
    }
}
