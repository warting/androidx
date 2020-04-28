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

package androidx.paging

import androidx.annotation.CheckResult
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Insert.Companion.Refresh
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Container for Paged data from a single generation of loads.
 *
 * Each refresh of data (generally either pushed by local storage, or pulled from the network)
 * will have a separate corresponding [PagingData].
 */
class PagingData<T : Any> internal constructor(
    internal val flow: Flow<PageEvent<T>>,
    internal val receiver: UiReceiver
) {
    private inline fun <R : Any> transform(crossinline transform: (PageEvent<T>) -> PageEvent<R>) =
        PagingData(
            flow = flow.map { transform(it) },
            receiver = receiver
        )

    /**
     * Returns a [PagingData] containing the result of applying the given [transform] to each
     * element, as it is loaded.
     */
    @CheckResult
    fun <R : Any> map(transform: (T) -> R): PagingData<R> = transform { it.map(transform) }

    /**
     * Returns a [PagingData] of all elements returned from applying the given [transform]
     * to each element, as it is loaded.
     */
    @CheckResult
    fun <R : Any> flatMap(transform: (T) -> Iterable<R>): PagingData<R> =
        transform { it.flatMap(transform) }

    /**
     * Returns a [PagingData] containing only elements matching the given [predicate]
     */
    @CheckResult
    fun filter(predicate: (T) -> Boolean): PagingData<T> = transform { it.filter(predicate) }

    /**
     * Returns a [PagingData] containing each original element, with the passed header [item] added
     * to the start of the list.
     *
     * The header [item] is added to a loaded page which marks the end of the data stream in the
     * prepend direction by returning null in [PagingSource.LoadResult.Page.prevKey]. It will be
     * removed if the first page in the list is dropped, which can happen in the case of loaded
     * pages exceeding [PagedList.Config.maxSize].
     *
     * Note: This operation is not idempotent, calling it multiple times will continually add
     * more headers to the start of the list, which can be useful if multiple header items are
     * required.
     *
     * @see [insertFooterItem]
     */
    @CheckResult
    fun insertHeaderItem(item: T) = insertSeparators { before, _ ->
        if (before == null) item else null
    }

    /**
     * Returns a [PagingData] containing each original element, with the passed footer [item] added
     * to the end of the list.
     *
     * The footer [item] is added to a loaded page which marks the end of the data stream in the
     * append direction, either by returning null in [PagingSource.LoadResult.Page.nextKey]. It
     * will be removed if the first page in the list is dropped, which can happen in the case of
     * loaded* pages exceeding [PagedList.Config.maxSize].
     *
     * Note: This operation is not idempotent, calling it multiple times will continually add
     * more footers to the end of the list, which can be useful if multiple footer items are
     * required.
     *
     * @see [insertHeaderItem]
     */
    @CheckResult
    fun insertFooterItem(item: T) = insertSeparators { _, after ->
        if (after == null) item else null
    }

    companion object {
        internal val NOOP_RECEIVER = object : UiReceiver {
            override fun addHint(hint: ViewportHint) {}

            override fun retry() {}

            override fun refresh() {}
        }

        private val EMPTY = PagingData<Any>(
            flow = flowOf(
                Refresh(
                    pages = listOf(TransformablePage(originalPageOffset = 0, data = emptyList())),
                    placeholdersBefore = 0,
                    placeholdersAfter = 0,
                    loadStates = mapOf(
                        REFRESH to NotLoading.Idle,
                        PREPEND to NotLoading.Done,
                        APPEND to NotLoading.Done
                    )
                )
            ),
            receiver = NOOP_RECEIVER
        )

        @Suppress("UNCHECKED_CAST", "SyntheticAccessor")
        @JvmStatic // Convenience for Java developers.
        fun <T : Any> empty() = EMPTY as PagingData<T>

        // NOTE: samples in the doc below are manually imported from Java code in the samples
        // project, since Java cannot be linked with @sample.
        // DO NOT CHANGE THE BELOW COMMENT WITHOUT MAKING THE CORRESPONDING CHANGE IN `samples/`
        /**
         * Returns a [PagingData] containing each original element, with an optional separator
         * generated by [generator], given the elements before and after (or null, in boundary
         * conditions).
         *
         * Note that this transform is applied asynchronously, as pages are loaded. Potential
         * separators between pages are only computed once both pages are loaded.
         *
         * **Kotlin callers should instead use the extension function [insertSeparators]**
         *
         * ```
         * /*
         *  * Create letter separators in an alphabetically sorted list.
         *  *
         *  * For example, if the input is:
         *  *     "apple", "apricot", "banana", "carrot"
         *  *
         *  * The operator would output:
         *  *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
         *  */
         * pagingDataStream.map((pagingData) ->
         *         // map outer stream, so we can perform transformations on each paging generation
         *         PagingData.insertSeparators(pagingData,
         *                 (@Nullable String before, @Nullable String after) -> {
         *                     if (after != null && (before == null
         *                             || before.charAt(0) != after.charAt(0))) {
         *                         // separator - after is first item that starts with its first letter
         *                         return Character.toString(Character.toUpperCase(after.charAt(0)));
         *                     } else {
         *                         // no separator - either end of list, or first
         *                         // letters of items are the same
         *                         return null;
         *                     }
         *                 }));
         *
         * /*
         *  * Create letter separators in an alphabetically sorted list of Items, with UiModel objects.
         *  *
         *  * For example, if the input is (each an `Item`):
         *  *     "apple", "apricot", "banana", "carrot"
         *  *
         *  * The operator would output a list of UiModels corresponding to:
         *  *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
         *  */
         * pagingDataStream.map((itemPagingData) -> {
         *     // map outer stream, so we can perform transformations on each paging generation
         *
         *     // first convert items in stream to UiModel.Item
         *     PagingData<UiModel.ItemModel> itemModelPagingData =
         *             itemPagingData.map(UiModel.ItemModel::new);
         *
         *     // Now insert UiModel.Separators, which makes the PagingData of generic type UiModel
         *     return PagingData.insertSeparators(
         *             itemModelPagingData,
         *             (@Nullable UiModel.ItemModel before, @Nullable UiModel.ItemModel after) -> {
         *                 if (after != null && (before == null
         *                         || before.item.label.charAt(0) != after.item.label.charAt(0))) {
         *                     // separator - after is first item that starts with its first letter
         *                     return new UiModel.SeparatorModel(
         *                             Character.toUpperCase(after.item.label.charAt(0)));
         *                 } else {
         *                     // no separator - either end of list, or first
         *                     // letters of items are the same
         *                     return null;
         *                 }
         *             });
         * });
         *
         * public class UiModel {
         *     static class ItemModel extends UiModel {
         *         public Item item;
         *         ItemModel(Item item) {
         *             this.item = item;
         *         }
         *     }
         *     static class SeparatorModel extends UiModel {
         *         public char character;
         *         SeparatorModel(char character) {
         *             this.character = character;
         *         }
         *     }
         * }
         */
        @JvmStatic
        @CheckResult
        fun <T : R, R : Any> insertSeparators(
            pagingData: PagingData<T>,
            generator: (T?, T?) -> R?
        ): PagingData<R> {
            return pagingData.insertSeparators(generator)
        }
    }
}

/**
 * Returns a [PagingData] containing each original element, with an optional separator
 * generated by [generator], given the elements before and after (or null, in boundary
 * conditions).
 *
 * Note that this transform is applied asynchronously, as pages are loaded. Potential
 * separators between pages are only computed once both pages are loaded.
 *
 * **Java callers should instead use the static function [PagingData.insertSeparators]**
 *
 * @sample androidx.paging.samples.insertSeparatorsSample
 * @sample androidx.paging.samples.insertSeparatorsUiModelSample
 */
@CheckResult
fun <T : R, R : Any> PagingData<T>.insertSeparators(
    generator: (T?, T?) -> R?
): PagingData<R> {
    // This function must be an extension method, as it indirectly imposes a constraint on
    // the type of T (because T extends R). Ideally it would be declared not be an
    // extension, to make this method discoverable for Java callers, but we need to support
    // the common UI model pattern for separators:
    //     class UiModel
    //     class ItemModel: UiModel
    //     class SeparatorModel: UiModel
    return PagingData(
        flow = flow.insertEventSeparators(generator),
        receiver = receiver
    )
}

internal interface UiReceiver {
    fun addHint(hint: ViewportHint)
    fun retry()
    fun refresh()
}
