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

package androidx.collection

import kotlin.random.Random

class ScatterMapInsertBenchmark(private val dataSet: Array<String>) : CollectionBenchmark {
    override fun measuredBlock() {
        val map = MutableScatterMap<String, String>(dataSet.size)
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }
}

class ScatterMapInsertBenchmarkBadHash(private val dataSet: Array<Int?>) : CollectionBenchmark {
    override fun measuredBlock() {
        val map = MutableScatterMap<Int?, Int?>(dataSet.size)
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }
}

class ScatterHashMapReadBenchmark(private val dataSet: Array<String>) : CollectionBenchmark {
    private val map = MutableScatterMap<String, String>()

    init {
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }

    override fun measuredBlock() {
        for (testValue in dataSet) {
            map[testValue]
        }
    }
}

class ScatterHashMapReadBadHashBenchmark(private val dataSet: Array<Int?>) : CollectionBenchmark {
    private val map = MutableScatterMap<Int?, Int?>()

    init {
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }

    override fun measuredBlock() {
        for (testValue in dataSet) {
            map[testValue]
        }
    }
}

class ScatterMapForEachBenchmark(dataSet: Array<String>) : CollectionBenchmark {
    private val map = MutableScatterMap<String, String>()

    init {
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }

    override fun measuredBlock() {
        map.forEach { k, v ->
            @Suppress("UnusedEquals", "RedundantSuppression")
            k == v
        }
    }
}

class ScatterMapRemoveBenchmark(private val dataSet: Array<String>) : CollectionBenchmark {
    private val map = MutableScatterMap<String, String>()

    init {
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }

    override fun measuredBlock() {
        for (testValue in dataSet) {
            map.remove(testValue)
        }
    }
}

class ScatterMapComputeBenchmark(private val dataSet: Array<String>) : CollectionBenchmark {
    private val map = MutableScatterMap<String, String>()

    init {
        for (testValue in dataSet) {
            map[testValue] = testValue
        }
    }

    override fun measuredBlock() {
        for (testValue in dataSet) {
            map.compute(testValue) { _, v -> v ?: testValue }
        }
    }
}

class ScatterMapInsertRemoveBenchmark(private val dataSet: Array<Int?>) : CollectionBenchmark {
    private val map = MutableScatterMap<Int?, Int?>()

    override fun measuredBlock() {
        for (testValue in dataSet) {
            map[testValue] = testValue
            map.remove(testValue)
        }
    }
}

fun createDataSet(size: Int): Array<String> =
    Array(size) { index -> (index * Random.Default.nextFloat()).toString() }

fun createBadHashDataSet(size: Int): Array<Int?> = Array(size) { it }
