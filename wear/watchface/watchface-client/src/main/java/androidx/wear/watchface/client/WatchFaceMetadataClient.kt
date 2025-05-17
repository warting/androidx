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

package androidx.wear.watchface.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.RectF
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.wear.watchface.BoundingArc
import androidx.wear.watchface.ComplicationSlotBoundsTypeIntDef
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition
import androidx.wear.watchface.client.WatchFaceControlClient.Companion.createWatchFaceControlClient
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.GetComplicationSlotMetadataParams
import androidx.wear.watchface.control.data.GetUserStyleFlavorsParams
import androidx.wear.watchface.control.data.GetUserStyleSchemaParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.style.UserStyleFlavors
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.utility.AsyncTraceEvent
import androidx.wear.watchface.utility.TraceEvent
import kotlinx.coroutines.CompletableDeferred

/**
 * Interface for fetching watch face metadata. E.g. the [UserStyleSchema] and
 * [ComplicationSlotMetadata]. This must be [close]d after use to release resources.
 *
 * @deprecated use Watch Face Format instead
 */
@Deprecated(
    message =
        "AndroidX watchface libraries are deprecated, use Watch Face Format instead. For more info see: https://developer.android.com/training/wearables/wff"
)
public interface WatchFaceMetadataClient : AutoCloseable {

    public companion object {

        /**
         * Constructs a [WatchFaceMetadataClient] for fetching metadata for the specified watch
         * face.
         *
         * @param context Calling application's [Context].
         * @param watchFaceName The [ComponentName] of the watch face to fetch meta data from.
         * @return The [WatchFaceMetadataClient] if there is one.
         * @throws [ServiceNotBoundException] if the underlying watch face control service can not
         *   be bound or a [ServiceStartFailureException] if the watch face dies during startup. If
         *   the service's manifest contains an
         *   androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition meta data node then
         *   [PackageManager.NameNotFoundException] is thrown if [watchFaceName] is invalid.
         */
        @Throws(
            ServiceNotBoundException::class,
            ServiceStartFailureException::class,
            PackageManager.NameNotFoundException::class,
        )
        @SuppressWarnings("MissingJvmstatic") // Can't really call a suspend fun from java.
        public suspend fun create(
            context: Context,
            watchFaceName: ComponentName,
        ): WatchFaceMetadataClient {
            // Fallback to binding the service (slow).
            return createImpl(
                context,
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                    setPackage(watchFaceName.packageName)
                },
                watchFaceName,
                ParserProvider(),
            )
        }

        /**
         * Constructs a [WatchFaceMetadataClient] for fetching metadata for the specified resource
         * only watch face from its runtime. A resource only watch face runtime is a special watch
         * face that knows how to load watch faces from resources in another package that contains
         * only resources and no executable code.
         *
         * @param context Calling application's [Context].
         * @param watchFaceName The [ComponentName] of the watch face to fetch meta data from.
         * @param runtimePackage The package that contains the Resource only Watch Face runtime.
         * @return The [WatchFaceMetadataClient] if there is one.
         * @throws [ServiceNotBoundException] if the underlying watch face control service can not
         *   be bound or a [ServiceStartFailureException] if the watch face dies during startup. If
         *   the service's manifest contains an
         *   androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition meta data node then
         *   [PackageManager.NameNotFoundException] is thrown if [watchFaceName] is invalid.
         */
        @Throws(
            ServiceNotBoundException::class,
            ServiceStartFailureException::class,
            PackageManager.NameNotFoundException::class,
        )
        @SuppressWarnings("MissingJvmstatic") // Can't really call a suspend fun from java.
        public suspend fun createForRuntime(
            context: Context,
            watchFaceName: ComponentName,
            runtimePackage: String,
        ): WatchFaceMetadataClient {
            return createImpl(
                context,
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                    setPackage(runtimePackage)
                },
                watchFaceName,
                parserProvider = null,
            )
        }

        @Suppress("DEPRECATION") // getServiceInfo
        internal fun isXmlVersionCompatible(
            context: Context,
            resources: Resources,
            controlServicePackage: String,
            controlServiceName: String = ANDROIDX_WATCHFACE_CONTROL_SERVICE,
        ): Boolean {
            val controlServiceComponentName =
                ComponentName(controlServicePackage, controlServiceName)
            val version =
                try {
                    context.packageManager
                        .getServiceInfo(
                            controlServiceComponentName,
                            PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS,
                        )
                        .metaData
                        .getInt(ANDROIDX_WATCHFACE_XML_VERSION, 0)
                } catch (exception: PackageManager.NameNotFoundException) {
                    // WatchFaceControlService may be missing in case WF is built with
                    // pre-androidx watchface library.
                    return false
                }

            val ourVersion =
                resources.getInteger(androidx.wear.watchface.R.integer.watch_face_xml_version)

            if (version > ourVersion) {
                Log.w(
                    TAG,
                    "WatchFaceControlService version ($version) " +
                        "of $controlServiceComponentName is higher than $ourVersion",
                )
                return false
            }

            return true
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("DEPRECATION")
        open class ParserProvider {
            // Open to allow testing without having to install the sample app.
            open fun getParser(context: Context, watchFaceName: ComponentName): XmlResourceParser? {
                if (!isXmlVersionCompatible(context, context.resources, watchFaceName.packageName))
                    return null

                return context.packageManager
                    .getServiceInfo(watchFaceName, PackageManager.GET_META_DATA)
                    .loadXmlMetaData(
                        context.packageManager,
                        WatchFaceService.XML_WATCH_FACE_METADATA,
                    )
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("ShowingMemberInHiddenClass") // Spurious warning about exposing the
        // 'hidden' companion object, which _isn't_ hidden.
        public suspend fun createImpl(
            context: Context,
            intent: Intent,
            watchFaceName: ComponentName,
            parserProvider: ParserProvider?,
        ): WatchFaceMetadataClient {
            // Check if there's static metadata we can read (fast).
            parserProvider?.getParser(context, watchFaceName)?.let {
                return XmlWatchFaceMetadataClientImpl(
                    XmlSchemaAndComplicationSlotsDefinition.inflate(
                        context.packageManager.getResourcesForApplication(
                            watchFaceName.packageName
                        ),
                        it,
                    )
                )
            }

            val deferredService = CompletableDeferred<IWatchFaceControlService>()
            val traceEvent = AsyncTraceEvent("WatchFaceMetadataClientImpl.bindService")
            val serviceConnection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        traceEvent.close()
                        deferredService.complete(IWatchFaceControlService.Stub.asInterface(binder))
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        // Note if onServiceConnected is called first completeExceptionally will do
                        // nothing because the CompletableDeferred is already completed.
                        traceEvent.close()
                        deferredService.completeExceptionally(ServiceStartFailureException())
                    }
                }
            if (!BindHelper.bindService(context, intent, serviceConnection)) {
                traceEvent.close()
                throw ServiceNotBoundException()
            }
            return WatchFaceMetadataClientImpl(
                context,
                deferredService.await(),
                serviceConnection,
                watchFaceName,
            )
        }
    }

    /** Exception thrown by [createWatchFaceControlClient] if the remote service can't be bound. */
    public class ServiceNotBoundException : Exception()

    /** Exception thrown by [WatchFaceControlClient] methods if the service dies during start up. */
    public class ServiceStartFailureException(message: String = "") : Exception(message)

    /**
     * Returns the watch face's [UserStyleSchema].
     *
     * @throws [RuntimeException] if the watch face threw an exception while trying to service the
     *   request or there was a communication problem with watch face process.
     */
    public fun getUserStyleSchema(): UserStyleSchema

    /**
     * Whether or not the [UserStyleSchema] is static and won't change unless the APK is updated.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("isUserStyleSchemaStatic")
    public val isUserStyleSchemaStatic: Boolean

    /**
     * Returns a map of [androidx.wear.watchface.ComplicationSlot] ID to [ComplicationSlotMetadata]
     * for each slot in the watch face's [androidx.wear.watchface.ComplicationSlotsManager].
     *
     * @throws [RuntimeException] if the watch face threw an exception while trying to service the
     *   request or there was a communication problem with watch face process.
     */
    public fun getComplicationSlotMetadataMap(): Map<Int, ComplicationSlotMetadata>

    /**
     * Returns the watch face's [UserStyleFlavors].
     *
     * @throws [RuntimeException] if the watch face threw an exception while trying to service the
     *   request or there was a communication problem with watch face process.
     */
    public fun getUserStyleFlavors(): UserStyleFlavors
}

private const val TAG = "WatchFaceMetadataClient"
private const val ANDROIDX_WATCHFACE_XML_VERSION = "androidx.wear.watchface.xml_version"
private const val ANDROIDX_WATCHFACE_CONTROL_SERVICE =
    "androidx.wear.watchface.control.WatchFaceControlService"

/**
 * Static metadata for a [androidx.wear.watchface.ComplicationSlot].
 *
 * @property bounds The complication slot's [ComplicationSlotBounds]. Only non `null` for watch
 *   faces with a new enough [androidx.wear.watchface.control.WatchFaceControlService].
 * @property boundsType The [ComplicationSlotBoundsTypeIntDef] of the complication slot.
 * @property supportedTypes The list of [ComplicationType]s accepted by this complication slot. Used
 *   during complication data source selection, this list should be non-empty.
 * @property defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] which controls the
 *   initial complication data source when the watch face is first installed.
 * @property isInitiallyEnabled At creation a complication slot is either enabled or disabled. This
 *   can be overridden by a [ComplicationSlotsUserStyleSetting] (see
 *   [ComplicationSlotOverlay.enabled]). Editors need to know the initial state of a complication
 *   slot to predict the effects of making a style change.
 * @property fixedComplicationDataSource Whether or not the complication slot's complication data
 *   source is fixed (i.e. can't be changed by the user). This is useful for watch faces built
 *   around specific complication complication data sources.
 * @property complicationConfigExtras Extras to be merged into the Intent sent when invoking the
 *   complication data source chooser activity.
 * @deprecated use Watch Face Format instead
 */
@Deprecated(
    message =
        "AndroidX watchface libraries are deprecated, use Watch Face Format instead. For more info see: https://developer.android.com/training/wearables/wff"
)
public class ComplicationSlotMetadata
@ComplicationExperimental
constructor(
    public val bounds: ComplicationSlotBounds?,
    @ComplicationSlotBoundsTypeIntDef public val boundsType: Int,
    public val supportedTypes: List<ComplicationType>,
    public val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
    @get:JvmName("isInitiallyEnabled") public val isInitiallyEnabled: Boolean,
    public val fixedComplicationDataSource: Boolean,
    public val complicationConfigExtras: Bundle,
    private val boundingArc: BoundingArc?,
) {
    /** The optional [BoundingArc] for an edge complication if specified, or `null` otherwise. */
    // TODO(b/230364881): Make this a normal primary constructor property when BoundingArc is no
    // longer experimental.
    @ComplicationExperimental public fun getBoundingArc(): BoundingArc? = boundingArc

    /**
     * Constructs a [ComplicationSlotMetadata].
     *
     * @param bounds The complication slot's [ComplicationSlotBounds]. Only non `null` for watch
     *   faces with a new enough [androidx.wear.watchface.control.WatchFaceControlService].
     * @param boundsType The [ComplicationSlotBoundsTypeIntDef] of the complication slot.
     * @param supportedTypes The list of [ComplicationType]s accepted by this complication slot.
     *   Used during complication data source selection, this list should be non-empty.
     * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] which controls the
     *   initial complication data source when the watch face is first installed.
     * @param isInitiallyEnabled At creation a complication slot is either enabled or disabled. This
     *   can be overridden by a [ComplicationSlotsUserStyleSetting] (see
     *   [ComplicationSlotOverlay.enabled]). Editors need to know the initial state of a
     *   complication slot to predict the effects of making a style change.
     * @param fixedComplicationDataSource Whether or not the complication slot's complication data
     *   source is fixed (i.e. can't be changed by the user). This is useful for watch faces built
     *   around specific complication complication data sources.
     * @param complicationConfigExtras Extras to be merged into the Intent sent when invoking the
     *   complication data source chooser activity.
     */
    // TODO(b/230364881): Deprecate when BoundingArc is no longer experimental.
    @OptIn(ComplicationExperimental::class)
    constructor(
        bounds: ComplicationSlotBounds?,
        @ComplicationSlotBoundsTypeIntDef boundsType: Int,
        supportedTypes: List<ComplicationType>,
        defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        isInitiallyEnabled: Boolean,
        fixedComplicationDataSource: Boolean,
        complicationConfigExtras: Bundle,
    ) : this(
        bounds,
        boundsType,
        supportedTypes,
        defaultDataSourcePolicy,
        isInitiallyEnabled,
        fixedComplicationDataSource,
        complicationConfigExtras,
        null,
    )
}

internal class WatchFaceMetadataClientImpl
internal constructor(
    private val context: Context,
    private val service: IWatchFaceControlService,
    private val serviceConnection: ServiceConnection,
    private val watchFaceName: ComponentName,
) : WatchFaceMetadataClient {
    private var closed = false
    private val headlessClientDelegate = lazy {
        createHeadlessWatchFaceClient(watchFaceName)
            ?: throw WatchFaceMetadataClient.ServiceStartFailureException(
                "Could not open headless client for ${watchFaceName.flattenToString()}"
            )
    }
    private val headlessClient by headlessClientDelegate

    private fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName
    ): HeadlessWatchFaceClient? =
        TraceEvent("WatchFaceMetadataClientImpl.createHeadlessWatchFaceClient").use {
            requireNotClosed()
            return service
                .createHeadlessWatchFaceInstance(
                    HeadlessWatchFaceInstanceParams(
                        watchFaceName,
                        androidx.wear.watchface.data.DeviceConfig(false, false, 0, 0),
                        1,
                        1,
                        null,
                    )
                )
                ?.let { HeadlessWatchFaceClientImpl(it) }
        }

    private fun requireNotClosed() {
        require(!closed) { "WatchFaceMetadataClient method called after close" }
    }

    override fun getUserStyleSchema(): UserStyleSchema = callRemote {
        if (service.apiVersion >= 3) {
            UserStyleSchema(service.getUserStyleSchema(GetUserStyleSchemaParams(watchFaceName)))
        } else {
            headlessClient.userStyleSchema
        }
    }

    override val isUserStyleSchemaStatic: Boolean
        get() = false

    @OptIn(ComplicationExperimental::class)
    override fun getComplicationSlotMetadataMap(): Map<Int, ComplicationSlotMetadata> {
        requireNotClosed()
        return callRemote {
            if (service.apiVersion >= 3) {
                val wireFormat =
                    service.getComplicationSlotMetadata(
                        GetComplicationSlotMetadataParams(watchFaceName)
                    )
                wireFormat.associateBy(
                    { it.id },
                    {
                        val perSlotBounds = HashMap<ComplicationType, RectF>()
                        val perSlotMargins = HashMap<ComplicationType, RectF>()
                        for (i in it.complicationBoundsType.indices) {
                            val type = ComplicationType.fromWireType(it.complicationBoundsType[i])
                            perSlotBounds[type] = it.complicationBounds[i] ?: RectF()
                            perSlotMargins[type] = it.complicationMargins?.get(i) ?: RectF()
                        }
                        ComplicationSlotMetadata(
                            ComplicationSlotBounds.createFromPartialMap(
                                perSlotBounds,
                                perSlotMargins,
                            ),
                            it.boundsType,
                            it.supportedTypes.map { ComplicationType.fromWireType(it) },
                            DefaultComplicationDataSourcePolicy(
                                it.defaultDataSourcesToTry ?: emptyList(),
                                it.fallbackSystemDataSource,
                                ComplicationType.fromWireType(it.primaryDataSourceDefaultType),
                                ComplicationType.fromWireType(it.secondaryDataSourceDefaultType),
                                ComplicationType.fromWireType(it.defaultDataSourceType),
                            ),
                            it.isInitiallyEnabled,
                            it.isFixedComplicationDataSource,
                            it.complicationConfigExtras,
                            it.boundingArc?.let { arc ->
                                BoundingArc(arc.arcStartAngle, arc.totalArcAngle, arc.arcThickness)
                            },
                        )
                    },
                )
            } else {
                headlessClient.complicationSlotsState.mapValues {
                    ComplicationSlotMetadata(
                        null,
                        it.value.boundsType,
                        it.value.supportedTypes,
                        it.value.defaultDataSourcePolicy,
                        it.value.isInitiallyEnabled,
                        it.value.fixedComplicationDataSource,
                        it.value.complicationConfigExtras,
                        null,
                    )
                }
            }
        }
    }

    override fun getUserStyleFlavors(): UserStyleFlavors = callRemote {
        if (service.apiVersion >= 5) {
            UserStyleFlavors(service.getUserStyleFlavors(GetUserStyleFlavorsParams(watchFaceName)))
        } else {
            UserStyleFlavors()
        }
    }

    override fun close() =
        TraceEvent("WatchFaceMetadataClientImpl.close").use {
            closed = true
            if (headlessClientDelegate.isInitialized()) {
                headlessClient.close()
            }
            context.unbindService(serviceConnection)
        }
}

internal class XmlWatchFaceMetadataClientImpl(
    private val xmlSchemaAndComplicationSlotsDefinition: XmlSchemaAndComplicationSlotsDefinition
) : WatchFaceMetadataClient {
    override fun getUserStyleSchema() =
        xmlSchemaAndComplicationSlotsDefinition.schema ?: UserStyleSchema(emptyList())

    override val isUserStyleSchemaStatic: Boolean
        get() = true

    @OptIn(ComplicationExperimental::class)
    override fun getComplicationSlotMetadataMap() =
        xmlSchemaAndComplicationSlotsDefinition.complicationSlots.associateBy(
            { it.slotId },
            {
                ComplicationSlotMetadata(
                    it.bounds,
                    it.boundsType,
                    it.supportedTypes,
                    it.defaultDataSourcePolicy,
                    it.initiallyEnabled,
                    it.fixedComplicationDataSource,
                    Bundle(),
                    it.boundingArc,
                )
            },
        )

    override fun getUserStyleFlavors() =
        xmlSchemaAndComplicationSlotsDefinition.flavors ?: UserStyleFlavors()

    override fun close() {}
}
