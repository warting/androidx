/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.core.graphics.withMatrix
import androidx.core.view.ViewCompat
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV21
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV29
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV33
import androidx.ink.authoring.internal.FinishedStroke
import androidx.ink.authoring.internal.InProgressStrokesManager
import androidx.ink.authoring.internal.InProgressStrokesRenderHelper
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.authoring.latency.LatencyDataCallback
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import androidx.test.espresso.idling.CountingIdlingResource
import java.util.concurrent.TimeUnit
import kotlin.math.hypot

// See https://www.nist.gov/pml/owm/si-units-length
private const val CM_PER_INCH = 2.54f

/** Displays in-progress ink strokes as [MotionEvent] user inputs are provided to it. */
@OptIn(ExperimentalLatencyDataApi::class)
public class InProgressStrokesView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Force the HWUI-based high latency implementation to be used under the hood, even if the
     * system supports low latency inking.
     *
     * This must be set to its desired value before the first call to [startStroke] or [eagerInit].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @Deprecated("Prefer to allow the underlying implementation details to be chosen automatically.")
    public var useHighLatencyRenderHelper: Boolean = false

    /**
     * Set a minimum delay from when the user finishes a stroke until rendering is handed off to the
     * client's dry layer via [InProgressStrokesFinishedListener.onStrokesFinished]. This value
     * would ideally be long enough that quick subsequent strokes - such as for fast handwriting -
     * are processed and later handed off as one group, but short enough that the handoff can take
     * place during short, natural pauses in handwriting.
     *
     * If handoff is ever needed as soon as safely possible, call [requestHandoff].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public var handoffDebounceTimeMs: Long = 0L
        @UiThread
        set(value) {
            require(value >= 0L) { "Debounce time must not be negative, received $value" }
            field = value
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            if (isInitialized()) {
                inProgressStrokesManager.setHandoffDebounceTimeMs(value)
            }
        }

    // Note: public experimental properties are not allowed because the accessors will not appear
    // experimental to Java clients. There are public accessors for this property below.
    @ExperimentalInkCustomBrushApi
    private var textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null }
        @JvmName("setTextureBitmapStorePrivate")
        set(value) {
            check(!isInitialized()) { "Cannot set textureBitmapStore after initialization." }
            field = value
        }

    /**
     * [TextureBitmapStore] used by the default value for [rendererFactory].
     *
     * By default, this is a no-op implementation that does not load any brush textures. The factory
     * functions are called when the renderer is initialized, so if this will be changed to
     * something that does load and store texture images, it must be set before the first call to
     * [startStroke] or [eagerInit].
     */
    @ExperimentalInkCustomBrushApi
    public fun getTextureBitmapStore(): TextureBitmapStore {
        return textureBitmapStore
    }

    /**
     * Sets the [TextureBitmapStore] used by the default value for [rendererFactory].
     *
     * See [getTextureBitmapStore].
     */
    @ExperimentalInkCustomBrushApi
    public fun setTextureBitmapStore(value: TextureBitmapStore) {
        textureBitmapStore = value
    }

    /**
     * A function that creates a [CanvasStrokeRenderer] when invoked. The default implementation of
     * this will automatically account for the Android OS version of the device. If you choose to
     * replace the default with an alternate implementation, then you must set this variable before
     * the first call to [startStroke] or [eagerInit].
     */
    public var rendererFactory: () -> CanvasStrokeRenderer = {
        @OptIn(ExperimentalInkCustomBrushApi::class) CanvasStrokeRenderer.create(textureBitmapStore)
    }
        set(value) {
            check(!isInitialized()) { "Cannot set rendererFactory after initialization." }
            field = value
            finishedStrokesView.rendererFactory = value
        }

    /**
     * Denote an area of this [InProgressStrokesView] where no ink should be visible. This is useful
     * for UI elements that float on top of (in Z order) the drawing surface - without this, a user
     * would be able to draw in-progress ("wet") strokes on top of those UI elements, but then when
     * the stroke is finished, it will appear as a dry stroke underneath of the UI element. If this
     * mask is set to the shape and position of the floating UI element, then the ink will never be
     * rendered in that area, making it appear as if it's being drawn underneath the UI element.
     *
     * This technique is most convincing when the UI element is opaque. Often there are parts of the
     * UI element that are translucent, such as drop shadows, or anti-aliasing along the edges. The
     * result will look a little different between wet and dry strokes for those cases, but it can
     * be a worthwhile tradeoff compared to the alternative of drawing wet strokes on top of that UI
     * element.
     */
    public var maskPath: Path? = null
        set(value) {
            field = value
            renderHelper?.maskPath = value
        }

    /**
     * The transform matrix to convert [MotionEvent] coordinates, as passed to [startStroke],
     * [addToStroke], and [finishStroke], into coordinates of this [InProgressStrokesView] for
     * rendering. Defaults to the identity matrix, for the recommended case where
     * [InProgressStrokesView] exactly overlays the [android.view.View] that has the touch listener
     * from which [MotionEvent] instances are being forwarded.
     */
    public var motionEventToViewTransform: Matrix = Matrix()
        set(value) {
            field.set(value)
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            if (isInitialized()) {
                inProgressStrokesManager.motionEventToViewTransform = value
            }
        }

    /**
     * Allows a test to easily wait until all in-progress strokes are completed and handed off.
     * There is no reason to set this in non-test code.
     */
    @VisibleForTesting
    public var inProgressStrokeCounter: CountingIdlingResource? = null
        set(value) {
            field = value
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            if (isInitialized()) {
                inProgressStrokesManager.inProgressStrokeCounter = value
            }
        }

    // Note: public experimental properties are not allowed because the accessors will not appear
    // experimental to Java clients. There are public accessors for this property below.
    @ExperimentalLatencyDataApi private var latencyDataCallback: LatencyDataCallback? = null

    /**
     * An optional callback for reporting latency of the processing of input events for in-progress
     * strokes. Clients may implement the [LatencyDataCallback] interface and set this field to
     * receive latency measurements.
     *
     * Notes for clients: Do not hold references to the [LatencyData] passed into this callback.
     * After this callback returns, the [LatencyData] instance will immediately become invalid: it
     * will be deleted or recycled. Also, to avoid stalling the UI thread, implementers should
     * minimize the amount of computation in this callback, and should also avoid allocations (since
     * allocation may trigger the garbage collector).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalLatencyDataApi
    public fun getLatencyDataCallback(): LatencyDataCallback? {
        return latencyDataCallback
    }

    /**
     * Sets the callback for reporting latency of the processing of input events for in-progress
     * strokes.
     *
     * See [getLatencyDataCallback]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalLatencyDataApi
    public fun setLatencyDataCallback(value: LatencyDataCallback?) {
        latencyDataCallback = value
    }

    private val renderHelperCallback =
        object : InProgressStrokesRenderHelper.Callback {

            override fun onDraw() = inProgressStrokesManager.onDraw()

            override fun onDrawComplete() = inProgressStrokesManager.onDrawComplete()

            override fun reportEstimatedPixelPresentationTime(timeNanos: Long) =
                inProgressStrokesManager.reportEstimatedPixelPresentationTime(timeNanos)

            override fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit) =
                inProgressStrokesManager.setCustomLatencyDataField(setter)

            override fun handOffAllLatencyData() = inProgressStrokesManager.handOffAllLatencyData()

            override fun setPauseStrokeCohortHandoffs(paused: Boolean) =
                inProgressStrokesManager.setPauseStrokeCohortHandoffs(paused)

            override fun onStrokeCohortHandoffToHwui(
                strokeCohort: Map<InProgressStrokeId, FinishedStroke>
            ) = inProgressStrokesManager.onStrokeCohortHandoffToHwui(strokeCohort)

            override fun onStrokeCohortHandoffToHwuiComplete() =
                inProgressStrokesManager.onStrokeCohortHandoffToHwuiComplete()
        }

    private val finishedStrokesListeners = mutableSetOf<InProgressStrokesFinishedListener>()

    private val finishedStrokes = mutableMapOf<InProgressStrokeId, Stroke>()

    // Most callers can use inProgressStrokesManager, but isInitialized() needs direct access to the
    // delegate's isInitialized method.
    private val inProgressStrokesManagerDelegate = lazy {
        InProgressStrokesManager(
                inProgressStrokesRenderHelper(),
                ::postOnAnimation,
                ::post,
                // When InProgressStrokesManager calls back to report a LatencyData, report it in
                // turn to
                // the client using the callback that they provided.
                latencyDataCallback = { latencyDataCallback?.onLatencyData(it) },
            )
            .also {
                it.addListener(inProgressStrokesManagerListener)
                // While initializing the InProgressStrokesManager, pass along any properties that
                // had been
                // set pre-initialization.
                it.motionEventToViewTransform = motionEventToViewTransform
                it.inProgressStrokeCounter = inProgressStrokeCounter
                it.setHandoffDebounceTimeMs(handoffDebounceTimeMs)
            }
    }
    private val inProgressStrokesManager by inProgressStrokesManagerDelegate

    private val inProgressStrokesManagerListener =
        object : InProgressStrokesManager.Listener {
            override fun onAllStrokesFinished(strokes: Map<InProgressStrokeId, FinishedStroke>) {
                finishedStrokesView.addStrokes(strokes)

                val newlyFinishedStrokes = mutableMapOf<InProgressStrokeId, Stroke>()
                for ((strokeId, finishedStroke) in strokes) {
                    newlyFinishedStrokes[strokeId] = finishedStroke.stroke
                }

                finishedStrokes.putAll(newlyFinishedStrokes)
                for (listener in finishedStrokesListeners) {
                    listener.onStrokesFinished(newlyFinishedStrokes)
                }
            }
        }

    private var renderHelper: InProgressStrokesRenderHelper? = null

    private val finishedStrokesView =
        FinishedStrokesView(context, rendererFactory = rendererFactory)

    // The simplified version of the API assumes that there is only one stroke in progress with a
    // given pointer ID at a time (i.e. that each stroke in a gesture is finished or cancelled
    // before
    // strokes in the next gesture are started, input for strokes from different gestures are not
    // interleaved).
    private val pointerIdToInProgressStrokeId = MutableIntObjectMap<InProgressStrokeId>()

    private fun inProgressStrokesRenderHelper(): InProgressStrokesRenderHelper {
        val existingInstance = renderHelper
        if (existingInstance != null) return existingInstance

        val renderer = rendererFactory()

        @Suppress("ObsoleteSdkInt") // TODO(b/262911421): Should not need to suppress.
        val result =
            @Suppress("DEPRECATION")
            if (useHighLatencyRenderHelper) {
                CanvasInProgressStrokesRenderHelperV21(this, renderHelperCallback, renderer)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                CanvasInProgressStrokesRenderHelperV33(this, renderHelperCallback, renderer)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CanvasInProgressStrokesRenderHelperV29(this, renderHelperCallback, renderer)
            } else {
                CanvasInProgressStrokesRenderHelperV21(this, renderHelperCallback, renderer)
            }

        result.maskPath = maskPath
        renderHelper = result

        return result
    }

    private fun isInitialized() = inProgressStrokesManagerDelegate.isInitialized()

    /**
     * Add a listener to be notified when strokes are finished. These strokes will continue to be
     * rendered within this view until [removeFinishedStrokes] is called. All of the strokes that
     * have been delivered to listeners but have not yet been removed with [removeFinishedStrokes]
     * are available through [getFinishedStrokes].
     */
    public fun addFinishedStrokesListener(listener: InProgressStrokesFinishedListener) {
        finishedStrokesListeners.add(listener)
    }

    /** Removes a listener that had previously been added with [addFinishedStrokesListener]. */
    public fun removeFinishedStrokesListener(listener: InProgressStrokesFinishedListener) {
        finishedStrokesListeners.remove(listener)
    }

    /**
     * Eagerly initialize rather than waiting for the first stroke to be drawn. Since initialization
     * can be somewhat heavyweight, doing this as soon as it's likely for the user to start drawing
     * can prevent initialization from introducing latency to the first stroke.
     */
    public fun eagerInit() {
        // Getting the lazy value kicks off its initialization.
        @Suppress("UNUSED_VARIABLE") val unused = inProgressStrokesManager
    }

    /**
     * Start building a stroke using a particular pointer within a [MotionEvent]. This would
     * typically be followed by many calls to [addToStroke], and the sequence would end with a call
     * to either [finishStroke] or [cancelStroke].
     *
     * In most circumstances, prefer to use this function over [startStroke] that accepts a
     * [StrokeInput].
     *
     * For optimum performance, it is strongly recommended to call [View.requestUnbufferedDispatch]
     * using [event] and the [View] that generated [event] alongside calling this function. When
     * requested this way, unbuffered dispatch mode will automatically end when the gesture is
     * complete.
     *
     * @param event The first [MotionEvent] as part of a Stroke's input data, typically one with a
     *   [MotionEvent.getActionMasked] value of [MotionEvent.ACTION_DOWN] or
     *   [MotionEvent.ACTION_POINTER_DOWN], but not restricted to those action types.
     * @param pointerId The identifier of the pointer within [event] to be used for inking, as
     *   determined by [MotionEvent.getPointerId] and used as an input to
     *   [MotionEvent.findPointerIndex]. Note that this is the ID of the pointer, not its index.
     * @param brush Brush specification for the stroke being started. Note that the overall scaling
     *   factor of [motionEventToWorldTransform] and [strokeToWorldTransform] combined should be
     *   related to the value of [Brush.epsilon] - in general, the larger the combined
     *   `motionEventToStrokeTransform` scaling factor is, the smaller on screen the stroke units
     *   are, so [Brush.epsilon] should be a larger quantity of stroke units to maintain a similar
     *   screen size.
     * @param motionEventToWorldTransform The matrix that transforms [event] coordinates into the
     *   client app's "world" coordinates, which typically is defined by how a client app's document
     *   is panned/zoomed/rotated. This defaults to the identity matrix, in which case the world
     *   coordinate space is the same as the [MotionEvent] coordinates, but the caller should pass
     *   in their own value reflecting a coordinate system that is independent of the device's pixel
     *   density (e.g. scaled by 1 / [android.util.DisplayMetrics.density]) and any pan/zoom/rotate
     *   gestures that have been applied to the "camera" which portrays the "world" on the device
     *   screen. This matrix must be invertible.
     * @param strokeToWorldTransform Allows an object-specific (stroke-specific) coordinate space to
     *   be defined in relation to the caller's "world" coordinate space. This defaults to the
     *   identity matrix, which is typical for many use cases at the time of stroke construction. In
     *   typical use cases, stroke coordinates and world coordinates may start to differ from one
     *   another after stroke creation as a particular stroke is manipulated within the world, e.g.
     *   it may be moved, scaled, or rotated relative to other content within an app's document.
     *   This matrix must be invertible.
     * @return The [InProgressStrokeId] of the stroke being built, later used to identify which
     *   stroke is being updated with [addToStroke] or ended with [finishStroke] or [cancelStroke].
     *   Callers that assume strokes map one-to-one with pointers in a gesture (as is typical) can
     *   skip storing this return value and use the overrides of [addToStroke], [finishStroke], and
     *   [cancelStroke] that just take a [MotionEvent] and a [pointerId].
     * @throws IllegalArgumentException if [motionEventToWorldTransform] or [strokeToWorldTransform]
     *   is not invertible.
     */
    @JvmOverloads
    @OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)
    public fun startStroke(
        event: MotionEvent,
        pointerId: Int,
        brush: Brush,
        motionEventToWorldTransform: Matrix = IDENTITY_MATRIX,
        strokeToWorldTransform: Matrix = IDENTITY_MATRIX,
    ): InProgressStrokeId =
        startStroke(
            event,
            pointerId,
            brush,
            { 0f },
            motionEventToWorldTransform,
            strokeToWorldTransform,
        )

    /**
     * Same as [startStroke], but for a stroke whose [Brush] includes a texture animation driven by
     * a [ValueAnimator].
     *
     * @param textureAnimationProgress An animator for the progress (from 0 to 1) of this stroke's
     *   texture animation. Values outside [0, 1] are wrapped. Non-finite values are not allowed. A
     *   null animator is treated as always 0.
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    public fun startStroke(
        event: MotionEvent,
        pointerId: Int,
        brush: Brush,
        textureAnimationProgress: ValueAnimator?,
        motionEventToWorldTransform: Matrix = IDENTITY_MATRIX,
        strokeToWorldTransform: Matrix = IDENTITY_MATRIX,
    ): InProgressStrokeId =
        startStroke(
            event,
            pointerId,
            brush,
            { textureAnimationProgress?.animatedValue as Float? ?: 0f },
            motionEventToWorldTransform,
            strokeToWorldTransform,
        )

    /**
     * Same as [startStroke], but for a stroke whose [Brush] includes a texture animation.
     *
     * @param textureAnimationProgress A lambda that, at any given time, will return the current
     *   progress (from 0 to 1) of this stroke's texture animation. Values outside [0, 1] are
     *   wrapped. Non-finite values are not allowed. This lambda will be called on the UI thread,
     *   potentially multiple times per frame, so it should be fast, stateless, and side-effect
     *   free.
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    public fun startStroke(
        event: MotionEvent,
        pointerId: Int,
        brush: Brush,
        textureAnimationProgress: () -> Float,
        motionEventToWorldTransform: Matrix = IDENTITY_MATRIX,
        strokeToWorldTransform: Matrix = IDENTITY_MATRIX,
    ): InProgressStrokeId =
        inProgressStrokesManager
            .startStroke(
                event,
                pointerId,
                motionEventToWorldTransform,
                strokeToWorldTransform,
                brush,
                textureAnimationProgress,
                strokeUnitLengthCm =
                    strokeUnitLengthCm(motionEventToWorldTransform, strokeToWorldTransform),
            )
            .also { strokeId -> pointerIdToInProgressStrokeId.put(pointerId, strokeId) }

    private fun strokeUnitLengthCm(
        motionEventToWorldTransform: Matrix,
        strokeToWorldTransform: Matrix,
    ): Float {
        val strokeToCmTransform =
            Matrix().also {
                // Compute (world -> MotionEvent) = (MotionEvent -> world)^-1
                require(motionEventToWorldTransform.invert(it)) {
                    "motionEventToWorldTransform must be invertible, but was $motionEventToWorldTransform"
                }
                // Compute (stroke -> MotionEvent) = (world -> MotionEvent) * (stroke -> world)
                it.preConcat(strokeToWorldTransform)
                // Compute (stroke -> screen) = (MotionEvent -> screen) * (stroke -> MotionEvent)
                ViewCompat.transformMatrixToGlobal(this, it)
                // Compute (stroke -> cm) = (screen -> cm) * (stroke -> screen)
                val metrics = context.resources.displayMetrics
                it.postScale(CM_PER_INCH / metrics.xdpi, CM_PER_INCH / metrics.ydpi)
            }
        // Compute the scaling factor that is being applied by the (stroke -> cm) transform. If the
        // transform is isotropic (which it should be, unless the client app is doing something
        // weird),
        // then the vertical and horizontal scaling factors will be the same, but just in case
        // they're
        // not, average them together.
        val values = FloatArray(9)
        strokeToCmTransform.getValues(values)
        return 0.5f * (hypot(values[0], values[1]) + hypot(values[3], values[4]))
    }

    /**
     * Start building a stroke with the provided [input]. This would typically be followed by many
     * calls to [addToStroke], and the sequence would end with a call to either [finishStroke] or
     * [cancelStroke].
     *
     * In most circumstances, the [startStroke] overload that accepts a [MotionEvent] is more
     * convenient. However, this overload using a [StrokeInput] is available for cases where the
     * input data may not come directly from a [MotionEvent], such as receiving events over a
     * network connection.
     *
     * If there is a way to request unbuffered dispatch from the source of the input data used here,
     * equivalent to [View.requestUnbufferedDispatch] for unbuffered [MotionEvent] data, then be
     * sure to request it for optimal performance.
     *
     * @param input The [StrokeInput] that started a stroke.
     * @param brush Brush specification for the stroke being started. Note that if stroke coordinate
     *   units (the [StrokeInput.x] and [StrokeInput.y] fields of [input]) are scaled to be very
     *   different in size than screen pixels, then it is recommended to update the value of
     *   [Brush.epsilon] to reflect that.
     * @param strokeToViewTransform The [Matrix] that converts stroke coordinates as provided in
     *   [input] into the coordinate space of this [InProgressStrokesView] for rendering.
     * @return The [InProgressStrokeId] of the stroke being built, later used to identify which
     *   stroke is being updated with [addToStroke] or ended with [finishStroke] or [cancelStroke].
     */
    @JvmOverloads
    public fun startStroke(
        input: StrokeInput,
        brush: Brush,
        strokeToViewTransform: Matrix = IDENTITY_MATRIX,
    ): InProgressStrokeId =
        inProgressStrokesManager.startStroke(input, brush, strokeToViewTransform)

    /**
     * Add input data, from a particular pointer within a [MotionEvent], to an existing stroke.
     *
     * @param event The next [MotionEvent] as part of a stroke's input data, typically one with
     *   [MotionEvent.getActionMasked] of [MotionEvent.ACTION_MOVE].
     * @param pointerId The identifier of the pointer within [event] to be used for inking, as
     *   determined by [MotionEvent.getPointerId] and used as an input to
     *   [MotionEvent.findPointerIndex]. Note that this is the ID of the pointer, not its index.
     * @param strokeId The [InProgressStrokeId] of the stroke to be built upon.
     * @param prediction Predicted [MotionEvent] containing predicted inputs between [event] and the
     *   time of the next frame. This value typically comes from
     *   [androidx.input.motionprediction.MotionEventPredictor.predict]. It is technically optional,
     *   but it is strongly recommended to achieve the best performance.
     */
    @JvmOverloads
    public fun addToStroke(
        event: MotionEvent,
        pointerId: Int,
        strokeId: InProgressStrokeId,
        prediction: MotionEvent? = null,
    ): Unit =
        inProgressStrokesManager.addToStroke(
            event,
            pointerId,
            strokeId,
            makeCorrectPrediction(prediction),
        )

    /**
     * Add [event] data for [pointerId] to the corresponding in-progress stroke, if present.
     *
     * @param event the next [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_MOVE.
     * @param pointerId the index of the relevant pointer in the [event]. If [pointerId] does not
     *   correspond to an in-progress stroke, this call is ignored.
     * @param prediction optional predicted [MotionEvent] containing predicted inputs between event
     *   and the time of the next frame, as generated by
     *   [androidx.input.motionprediction.MotionEventPredictor.predict].
     * @return Whether the pointer corresponds to an in-progress stroke.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @JvmOverloads
    public fun addToStroke(
        event: MotionEvent,
        pointerId: Int,
        prediction: MotionEvent? = null,
    ): Boolean {
        addToStroke(
            event,
            pointerId,
            pointerIdToInProgressStrokeId[pointerId] ?: return false,
            prediction,
        )
        return true
    }

    /**
     * Add input data from a [StrokeInputBatch] to an existing stroke.
     *
     * @param inputs The next [StrokeInputBatch] to be added to the stroke.
     * @param strokeId The [InProgressStrokeId] of the stroke to be built upon.
     * @param prediction Predicted [StrokeInputBatch] containing predicted inputs between [inputs]
     *   and the time of the next frame. This can technically be empty, but it is strongly
     *   recommended for it to be non-empty to achieve the best performance.
     */
    @JvmOverloads
    public fun addToStroke(
        inputs: StrokeInputBatch,
        strokeId: InProgressStrokeId,
        prediction: StrokeInputBatch = ImmutableStrokeInputBatch.EMPTY,
    ): Unit = inProgressStrokesManager.addToStroke(inputs, strokeId, prediction)

    /**
     * Temporary helper to clean prediction input to avoid crashing on multi-pointer draw. Remove
     * once prediction motionevents are cleaned up.
     *
     * TODO b/306361370 - Remove this function when prediction motionevents contain clean eventtime
     * data.
     */
    private fun makeCorrectPrediction(event: MotionEvent?): MotionEvent? {
        if (event == null) return null
        if (event.eventTime == 0L) {
            Log.e(
                "InProgressStrokesView",
                "prediction motionevent has eventTime = 0L and is being ignored.",
            )
            return null
        }
        for (index in 0 until event.historySize) {
            if (event.getHistoricalEventTime(index) == 0L) {
                Log.e(
                    "InProgressStrokesView",
                    "Prediction motionevent has historicalEventTime[$index] = 0L and is being ignored.",
                )
                return null
            }
        }
        return event
    }

    /**
     * Complete the building of a stroke, with the last input data coming from a particular pointer
     * of a [MotionEvent].
     *
     * When the stroke no longer needs to be rendered by this [InProgressStrokesView] and can
     * instead be rendered anywhere in the [View] hierarchy using [CanvasStrokeRenderer], the
     * resulting [Stroke] object will be passed to the [InProgressStrokesFinishedListener] instances
     * registered with this [InProgressStrokesView] using [addFinishedStrokesListener].
     *
     * @param event The last [MotionEvent] as part of a stroke's input data, typically one with
     *   [MotionEvent.getActionMasked] of [MotionEvent.ACTION_UP] or
     *   [MotionEvent.ACTION_POINTER_UP], but can also be other actions.
     * @param pointerId The identifier of the pointer within [event] to be used for inking, as
     *   determined by [MotionEvent.getPointerId] and used as an input to
     *   [MotionEvent.findPointerIndex]. Note that this is the ID of the pointer, not its index.
     * @param strokeId The [InProgressStrokeId] of the stroke to be finished.
     */
    public fun finishStroke(event: MotionEvent, pointerId: Int, strokeId: InProgressStrokeId) {
        // Remove the strokeId from the map. If it corresponded to this pointer ID (the usual case),
        // we can do that in the fast way.
        if (!pointerIdToInProgressStrokeId.remove(pointerId, strokeId)) {
            pointerIdToInProgressStrokeId.removeIf { _, v -> v == strokeId }
        }
        inProgressStrokesManager.finishStroke(event, pointerId, strokeId)
    }

    /**
     * Finish the corresponding in-progress stroke with [event] data for [pointerId], if present.
     *
     * @param event the last [MotionEvent] as part of a stroke, typically an ACTION_UP.
     * @param pointerId the id of the relevant pointer in the [event]. If [pointerId] does not
     *   correspond to an in-progress stroke, this call is ignored.
     * @return Whether the pointer corresponded to an in-progress stroke.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun finishStroke(event: MotionEvent, pointerId: Int): Boolean {
        inProgressStrokesManager.finishStroke(
            event,
            pointerId,
            pointerIdToInProgressStrokeId.remove(pointerId) ?: return false,
        )
        return true
    }

    /**
     * Complete the building of a stroke, with the last input data coming from a [StrokeInput].
     *
     * @param input The last [StrokeInput] in the stroke.
     * @param strokeId The [InProgressStrokeId] of the stroke to be finished.
     */
    public fun finishStroke(input: StrokeInput, strokeId: InProgressStrokeId) {
        // In general, use of the StrokeInput[Batch] API won't be mixed with the MotionEvent API
        // (especially the version that isn't keeping track of StrokeId explicitly), so this map
        // will be empty. Even if not, we would expect it to be short.
        pointerIdToInProgressStrokeId.removeIf { _, v -> v == strokeId }
        inProgressStrokesManager.finishStroke(input, strokeId)
    }

    /**
     * Cancel the building of a stroke. It will no longer be visible within this
     * [InProgressStrokesView], and no completed [Stroke] object will come through
     * [InProgressStrokesFinishedListener].
     *
     * This is typically done for one of three reasons:
     * 1. A [MotionEvent] with [MotionEvent.getActionMasked] of [MotionEvent.ACTION_CANCEL]. This
     *    tends to be when an entire gesture has been canceled, for example when a parent [View]
     *    uses [android.view.ViewGroup.onInterceptTouchEvent] to intercept and handle the gesture
     *    itself.
     * 2. A [MotionEvent] with [MotionEvent.getFlags] containing [MotionEvent.FLAG_CANCELED]. This
     *    tends to be when the system has detected an unintentional touch, such as from the user
     *    resting their palm on the screen while writing or drawing, after some events from that
     *    unintentional pointer have already been delivered.
     * 3. An app's business logic reinterprets a gesture previously used for inking as something
     *    else, and the earlier inking may be seen as unintentional. For example, an app that uses
     *    single-pointer gestures for inking and dual-pointer gestures for pan/zoom/rotate will
     *    start inking when the first pointer goes down, but when the second pointer goes down it
     *    may want to cancel the stroke from the first pointer rather than leave the small ink marks
     *    on the screen.
     *
     * @param strokeId The [InProgressStrokeId] of the stroke to be canceled.
     * @param event The [MotionEvent] that led to this cancellation, if applicable.
     */
    @JvmOverloads
    public fun cancelStroke(strokeId: InProgressStrokeId, event: MotionEvent? = null) {
        // Linear scan, but we expect the number of in-progress strokes to be small.
        pointerIdToInProgressStrokeId.removeIf { _, v -> v == strokeId }
        inProgressStrokesManager.cancelStroke(strokeId, event)
    }

    /**
     * Cancel the corresponding in-progress stroke with [event] data for [pointerId], if present.
     *
     * @param event The [MotionEvent] that led to this cancellation, typically an ACTION_CANCEL.
     * @param pointerId the id of the relevant pointer in the [event].
     * @return Whether the pointer corresponded to an in-progress stroke.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun cancelStroke(event: MotionEvent, pointerId: Int): Boolean {
        inProgressStrokesManager.cancelStroke(
            pointerIdToInProgressStrokeId.remove(pointerId) ?: return false,
            event,
        )
        return true
    }

    /** Cancel all in-progress strokes. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun cancelUnfinishedStrokes(): Unit = inProgressStrokesManager.cancelUnfinishedStrokes()

    /** Returns true if there are any in-progress strokes. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun hasUnfinishedStrokes(): Boolean = inProgressStrokesManager.hasUnfinishedStrokes()

    /**
     * Request that [handoffDebounceTimeMs] be temporarily ignored to hand off rendering to the
     * client's dry layer via [InProgressStrokesFinishedListener.onStrokesFinished]. This will be
     * done as soon as safely possible, still at a time when a rendering flicker can be avoided.
     * Afterwards, handoff debouncing will resume as normal.
     *
     * This API is experimental for now, as one approach to address start-of-stroke latency for fast
     * subsequent strokes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun requestHandoff(): Unit = inProgressStrokesManager.requestImmediateHandoff()

    /**
     * Make a best effort to end all currently in progress strokes, which will include a callback to
     * [InProgressStrokesFinishedListener.onStrokesFinished] during this function's execution if
     * there are any strokes to hand off. In normal operation, prefer to call [finishStroke] or
     * [cancelStroke] for each of your in progress strokes and wait for the callback to
     * [InProgressStrokesFinishedListener.onStrokesFinished], possibly accelerated by
     * [requestHandoff] if you have set a non-zero value for [handoffDebounceTimeMs]. This function
     * is for situations where an immediate shutdown is necessary, such as
     * [android.app.Activity.onPause]. This must be called on the UI thread, and will block it for
     * up to [timeoutMillis] milliseconds. Note that if this is called when the app is still visible
     * on screen, then the visual behavior is undefined - the stroke content may flicker.
     *
     * @param cancelAllInProgress If `true`, treat any unfinished strokes as if you called
     *   [cancelStroke] with their [InProgressStrokeId], so they will not be visible and not
     *   included in the return value of [getFinishedStrokes]. If `false`, treat unfinished strokes
     *   as if you called [finishStroke] with their [InProgressStrokeId], which will keep them
     *   visible and included in the return value of [getFinishedStrokes].
     * @param timeout The maximum time that will be spent waiting before returning. If this is not
     *   positive, then this will not wait at all.
     * @param timeoutUnit The [TimeUnit] for [timeout].
     * @return `true` if and only if the flush completed successfully. Note that not all
     *   configurations support flushing, and flushing is best effort, so this is not guaranteed to
     *   return `true`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun flush(
        timeout: Long,
        timeoutUnit: TimeUnit,
        cancelAllInProgress: Boolean = false,
    ): Boolean {
        if (!isInitialized()) {
            // Nothing to flush if it's not initialized.
            return true
        }
        pointerIdToInProgressStrokeId.clear()
        return inProgressStrokesManager.flush(timeout, timeoutUnit, cancelAllInProgress)
    }

    /**
     * For testing only. Wait up to [timeout] ([timeoutUnit]) until the queued actions have all been
     * processed. This must be called on the UI thread, and blocks it to run synchronously. This is
     * useful for tests to know that certain events have been processed, to be able to assert that a
     * screenshot will look a certain way, or that certain callbacks should be scheduled/delivered.
     * Do not call this from production code.
     *
     * In some ways this is similar to [flush], which is intended for production use in certain
     * circumstances.
     */
    @VisibleForTesting
    internal fun sync(timeout: Long, timeoutUnit: TimeUnit) {
        if (isInitialized()) {
            // Nothing to sync if it's not initialized.
            inProgressStrokesManager.sync(timeout, timeoutUnit)
        }
    }

    /**
     * Returns all the finished strokes that are still being rendered by this view. The IDs of these
     * strokes should be passed to [removeFinishedStrokes] when they are handed off to another view.
     */
    public fun getFinishedStrokes(): Map<InProgressStrokeId, Stroke> {
        return finishedStrokes
    }

    /**
     * Stop this view from rendering the strokes with the given IDs.
     *
     * This should be called in the same UI thread run loop (HWUI frame) as when the strokes start
     * being rendered elsewhere in the view hierarchy. This means they are saved in a location where
     * they will be picked up in a view's next call to [onDraw], and that view's [invalidate] method
     * has been called. If these two operations are not done within the same UI thread run loop
     * (usually side by side - see example below), then there will be brief rendering errors -
     * either a visual gap where the stroke is not drawn during a frame, or a double draw where the
     * stroke is drawn twice and translucent strokes appear more opaque than they should.
     */
    public fun removeFinishedStrokes(strokeIds: Set<InProgressStrokeId>) {
        for (id in strokeIds) finishedStrokes.remove(id)
        finishedStrokesView.removeStrokes(strokeIds)
    }

    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addView(finishedStrokesView)
    }

    private companion object {
        val IDENTITY_MATRIX = Matrix()
    }
}

/**
 * Renders finished strokes until the client says they are ready to render the strokes themselves
 * with [InProgressStrokesView.removeFinishedStrokes].
 */
@SuppressLint("ViewConstructor") // Not inflated through XML
private class FinishedStrokesView(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    // Since many clients will call removeFinishedStrokes immediately with the callback, this
    // holding
    // view may never need to render strokes within this holding view.
    var rendererFactory: () -> CanvasStrokeRenderer,
) : View(context, attrs, defStyleAttr) {

    private lateinit var renderer: CanvasStrokeRenderer

    private val finishedStrokes = mutableMapOf<InProgressStrokeId, FinishedStroke>()

    fun addStrokes(strokes: Map<InProgressStrokeId, FinishedStroke>) {
        finishedStrokes.putAll(strokes)
        invalidate()
    }

    fun removeStrokes(strokeIds: Set<InProgressStrokeId>) {
        for (strokeId in strokeIds) finishedStrokes.remove(strokeId)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Only initialize the renderer if there are strokes to draw, to avoid unnecessary
        // initialization if the client never adds any strokes.
        // NOMUTANTS -- only initialize the renderer once to optimize performance.
        if (!::renderer.isInitialized && finishedStrokes.isNotEmpty()) {
            renderer = rendererFactory()
        }
        for ((_, finishedStroke) in finishedStrokes) {
            canvas.withMatrix(finishedStroke.strokeToViewTransform) {
                renderer.draw(canvas, finishedStroke.stroke, finishedStroke.strokeToViewTransform)
            }
        }
    }
}
