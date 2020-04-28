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

package androidx.paging.integration.testapp.v3

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

val dataSourceError = AtomicBoolean(false)

/**
 * Sample position-based PagingSource with artificial data.
 */
internal class ItemPagingSource : PagingSource<Int, Item>() {
    class RetryableItemError : Exception()

    private val generationId = sGenerationId++

    override fun getRefreshKey(state: PagingState<Int, Item>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> =
        when (params.loadType) {
            LoadType.REFRESH ->
                loadInternal(
                    position = ((params.key ?: 0) - params.loadSize / 2).coerceAtLeast(0),
                    loadSize = params.loadSize
                )
            LoadType.PREPEND -> {
                val loadSize = minOf(params.key!!, params.pageSize)
                loadInternal(
                    position = params.key!! - loadSize,
                    loadSize = loadSize
                )
            }
            LoadType.APPEND ->
                loadInternal(
                    position = params.key!!,
                    loadSize = params.loadSize
                )
        }

    private suspend fun loadInternal(
        position: Int,
        loadSize: Int
    ): LoadResult<Int, Item> {
        delay(1000)
        if (dataSourceError.compareAndSet(true, false)) {
            return LoadResult.Error(RetryableItemError())
        } else {
            val bgColor = COLORS[generationId % COLORS.size]
            val endExclusive = (position + loadSize).coerceAtMost(COUNT)
            val data = (position until endExclusive).map {
                Item(it, "item $it", bgColor)
            }

            return LoadResult.Page(
                data = data,
                prevKey = if (position > 0) position else null,
                nextKey = if (endExclusive < COUNT) endExclusive else null,
                itemsBefore = position,
                itemsAfter = COUNT - endExclusive
            )
        }
    }

    companion object {
        val Factory =
            fun(): PagingSource<Int, Item> {
                sGenerationId++
                return ItemPagingSource()
            }

        private const val COUNT = 60

        @ColorInt
        private val COLORS = intArrayOf(Color.RED, Color.BLUE, Color.BLACK)
        private var sGenerationId: Int = 0
    }
}
