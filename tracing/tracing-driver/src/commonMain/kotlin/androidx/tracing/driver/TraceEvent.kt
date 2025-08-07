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

package androidx.tracing.driver

import androidx.annotation.RestrictTo

@PublishedApi internal const val TRACE_EVENT_TYPE_UNDEFINED: Int = 0

@PublishedApi internal const val TRACE_EVENT_TYPE_BEGIN: Int = 1

@PublishedApi internal const val TRACE_EVENT_TYPE_END: Int = 2

@PublishedApi internal const val TRACE_EVENT_TYPE_INSTANT: Int = 3

@PublishedApi internal const val TRACE_EVENT_TYPE_COUNTER: Int = 4

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val METADATA_ENTRIES_EXPECTED_SIZE: Int = 4

/**
 * Mutable in-memory only representation a trace event, such as a slice start, slice end, or counter
 * update.
 *
 * This structure is optimized for performance, with the expectation that these are created by a
 * trace event, and passed to a background thread to be serialized. Mutability (and thus reuse) is
 * an important component of this.
 *
 * Code outside of tracing-driver implementation should only ever consume these objects, not produce
 * them.
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("NOTHING_TO_INLINE", "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
public class TraceEvent
internal constructor(
    /**
     * Must be one of [TRACE_EVENT_TYPE_UNDEFINED], [TRACE_EVENT_TYPE_INSTANT],
     * [TRACE_EVENT_TYPE_BEGIN], [TRACE_EVENT_TYPE_END], [TRACE_EVENT_TYPE_COUNTER]
     */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var type: Int,
    /** Set to the value of the containing [Track]'s [Track.uuid]. */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var trackUuid: Long,

    /** Timestamp in nanoseconds of the trace event. */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var timestamp: Long,

    /** Name of the trace event - null if the event [type] is [TRACE_EVENT_TYPE_COUNTER]. */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var name: String?,
    @field:Suppress(
        "MutableBareField", // public / mutable to minimize overhead
        "AutoBoxing", // temporary
    )

    /**
     * Value of the trace event if the event [type] is [TRACE_EVENT_TYPE_COUNTER].
     *
     * Note that only one of [counterDoubleValue] and [counterLongValue] may be set.
     */
    @JvmField
    public var counterDoubleValue: Double?,

    /**
     * Value of the trace event if the event [type] is [TRACE_EVENT_TYPE_COUNTER].
     *
     * Note that only one of [counterDoubleValue] and [counterLongValue] may be set.
     */
    @field:Suppress(
        "MutableBareField", // public / mutable to minimize overhead
        "AutoBoxing", // temporary
    )
    @JvmField
    public var counterLongValue: Long?,

    /** List of trace flows associated with this event. */
    // ideally this would be a array to avoid boxing, but proto libs consume longs anyway :|
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var flowIds: List<Long>,

    /**
     * If not null, this TraceEvent initializes a track, and the [TrackDescriptor] defines its
     * properties.
     */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var trackDescriptor: TrackDescriptor?,

    /** The list of debug annotations associated with a slice */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var metadataEntries: MutableList<MetadataEntry>,

    /**
     * Keeping track of the index separately, because the MutableList is pre-allocated with sentinel
     * objects for performance reasons.
     */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var metadataEntryIndex: Int,
) {
    public constructor() :
        this(
            type = DEFAULT_INT,
            trackUuid = DEFAULT_LONG,
            timestamp = DEFAULT_LONG,
            name = null,
            counterDoubleValue = null,
            counterLongValue = null,
            flowIds = emptyList(),
            trackDescriptor = null,
            metadataEntries = MutableList(METADATA_ENTRIES_EXPECTED_SIZE) { MetadataEntry() },
            metadataEntryIndex = -1,
        )

    internal inline fun setPreamble(trackDescriptor: TrackDescriptor) {
        this.trackDescriptor = trackDescriptor
        this.timestamp = nanoTime()
    }

    @PublishedApi
    internal inline fun setBeginSection(trackUuid: Long, name: String) {
        type = TRACE_EVENT_TYPE_BEGIN
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        this.name = name
    }

    @PublishedApi
    internal inline fun setBeginSectionWithFlows(
        trackUuid: Long,
        name: String,
        flowIds: List<Long>,
    ) {
        type = TRACE_EVENT_TYPE_BEGIN
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        this.flowIds = flowIds
        this.name = name
    }

    @PublishedApi
    internal inline fun setEndSection(trackUuid: Long) {
        type = TRACE_EVENT_TYPE_END
        this.trackUuid = trackUuid
        timestamp = nanoTime()
    }

    @PublishedApi
    internal inline fun setInstant(trackUuid: Long, name: String) {
        type = TRACE_EVENT_TYPE_END
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        this.name = name
    }

    @PublishedApi
    internal inline fun setCounterLong(trackUuid: Long, value: Long) {
        type = TRACE_EVENT_TYPE_COUNTER
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        counterLongValue = value
    }

    @PublishedApi
    internal inline fun setCounterDouble(trackUuid: Long, value: Double) {
        type = TRACE_EVENT_TYPE_COUNTER
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        counterDoubleValue = value
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public inline fun forEachMetadataEntry(block: (MetadataEntry) -> Unit) {
        repeat(metadataEntryIndex + 1) { block(metadataEntries[it]) }
    }

    public fun reset() {
        type = DEFAULT_INT
        trackUuid = DEFAULT_LONG
        timestamp = DEFAULT_LONG
        name = null
        counterDoubleValue = null
        counterLongValue = null
        flowIds = emptyList()
        trackDescriptor = null
        if (metadataEntryIndex >= 0) {
            // Reset metadata entries and resize
            forEachMetadataEntry { it.reset() }
            if (metadataEntryIndex >= METADATA_ENTRIES_EXPECTED_SIZE) {
                metadataEntries = metadataEntries.subList(0, METADATA_ENTRIES_EXPECTED_SIZE)
            }
            metadataEntryIndex = -1
        }
    }
}
