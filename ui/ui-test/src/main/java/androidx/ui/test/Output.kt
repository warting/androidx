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

package androidx.ui.test

import android.util.Log
import androidx.annotation.IntRange
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.text.AnnotatedString
import androidx.ui.unit.PxBounds
import androidx.ui.unit.toSize

/**
 * Prints all the semantics nodes information it holds into string.
 *
 * By default this also prints all the sub-hierarchy. This can be changed by setting a custom max
 * depth in [maxDepth].
 *
 * Note that this will fetch the latest snapshot of nodes it sees in the hierarchy for the IDs it
 * collected before. So the output can change over time if the tree changes.
 *
 * @param maxDepth Max depth of the nodes in hierarchy to print. Zero will print just this node.
 */
fun SemanticsNodeInteraction.printToString(
    @IntRange(from = 0) maxDepth: Int = Int.MAX_VALUE
): String {
    val result = fetchSemanticsNode()
    return result.printToString(maxDepth)
}

/**
 * Prints all the semantics nodes information into logs (as debug level).
 *
 * By default this also prints all the sub-hierarchy. This can be changed by setting a custom max
 * depth in [maxDepth].
 *
 * Note that this will fetch the latest snapshot of nodes it sees in the hierarchy for the IDs it
 * collected before. So the output can change over time if the tree changes.
 *
 * @param tag The tag to be used in the log messages.
 * @param maxDepth Max depth of the nodes in hierarchy to print. Zero will print just this node.
 */
fun SemanticsNodeInteraction.printToLog(
    tag: String,
    @IntRange(from = 0) maxDepth: Int = Int.MAX_VALUE
) {
    val result = "printToLog:\n" + printToString(maxDepth)
    Log.d(tag, result)
}

/**
 * Prints all the semantics nodes information it holds into string.
 *
 * By default this does not print nodes sub-hierarchies. This can be changed by setting a custom max
 * depth in [maxDepth].
 *
 * Note that this will fetch the latest snapshot of nodes it sees in the hierarchy for the IDs it
 * collected before. So the output can change over time if the tree changes.
 *
 * @param maxDepth Max depth of the nodes in hierarchy to print. Zero will print nodes in this
 * collection only.
 */
fun SemanticsNodeInteractionCollection.printToString(
    @IntRange(from = 0) maxDepth: Int = 0
): String {
    val nodes = fetchSemanticsNodes()
    return if (nodes.isEmpty()) {
        "There were 0 nodes found!"
    } else {
        nodes.printToString(maxDepth)
    }
}

/**
 * Prints all the semantics nodes information into logs (as debug level).
 *
 * By default this does not print nodes sub-hierarchies. This can be changed by setting a custom max
 * depth in [maxDepth].
 *
 * Note that this will fetch the latest snapshot of nodes it sees in the hierarchy for the IDs it
 * collected before. So the output can change over time if the tree changes.
 *
 * @param tag The tag to be used in the log messages.
 * @param maxDepth Max depth of the nodes in hierarchy to print. Zero will print nodes in this
 * collection only.
 */
fun SemanticsNodeInteractionCollection.printToLog(
    tag: String,
    @IntRange(from = 0) maxDepth: Int = 0
) {
    val result = "printToLog:\n" + printToString(maxDepth)
    Log.d(tag, result)
}

internal fun Collection<SemanticsNode>.printToString(maxDepth: Int = 0): String {
    var sb = StringBuilder()
    var i = 1
    forEach {
        if (size > 1) {
            sb.append(i)
            sb.append(") ")
        }
        sb.append(it.printToString(maxDepth))
        if (i < size) {
            sb.appendLine()
        }
        ++i
    }
    return sb.toString()
}

internal fun SemanticsNode.printToString(maxDepth: Int = 0): String {
    val sb = StringBuilder()
    printToStringInner(
        sb = sb,
        maxDepth = maxDepth,
        nestingLevel = 0,
        nestingIndent = "",
        isFollowedBySibling = false
    )
    return sb.toString()
}

private fun SemanticsNode.printToStringInner(
    sb: StringBuilder,
    maxDepth: Int,
    nestingLevel: Int,
    nestingIndent: String,
    isFollowedBySibling: Boolean
) {
    val newIndent = if (nestingLevel == 0) {
        ""
    } else if (isFollowedBySibling) {
        "$nestingIndent | "
    } else {
        "$nestingIndent   "
    }

    if (nestingLevel > 0) {
        sb.append("$nestingIndent |-")
    }
    sb.append("Node #$id at ")
    sb.append(pxBoundsToShortString(unclippedGlobalBounds))

    if (config.contains(SemanticsProperties.TestTag)) {
        sb.append(", Tag: '")
        sb.append(config[SemanticsProperties.TestTag])
        sb.append("'")
    }

    val maxLevelReached = nestingLevel == maxDepth

    sb.appendConfigInfo(config, newIndent)

    if (maxLevelReached) {
        val childrenCount = children.size
        val siblingsCount = (parent?.children?.size ?: 1) - 1
        if (childrenCount > 0 || (siblingsCount > 0 && nestingLevel == 0)) {
            sb.appendLine()
            sb.append(newIndent)
            sb.append("Has ")
            if (childrenCount > 1) {
                sb.append("$childrenCount children")
            } else if (childrenCount == 1) {
                sb.append("$childrenCount child")
            }
            if (siblingsCount > 0 && nestingLevel == 0) {
                if (childrenCount > 0) {
                    sb.append(", ")
                }
                if (siblingsCount > 1) {
                    sb.append("$siblingsCount siblings")
                } else {
                    sb.append("$siblingsCount sibling")
                }
            }
        }
        return
    }

    val childrenLevel = nestingLevel + 1
    val children = this.children.toList()
    children.forEachIndexed { index, child ->
        val hasSibling = index < children.size - 1
        sb.appendLine()
        child.printToStringInner(sb, maxDepth, childrenLevel, newIndent, hasSibling)
    }
}

private val SemanticsNode.unclippedGlobalBounds: PxBounds
    get() {
        return PxBounds(globalPosition, size.toSize())
    }

private fun pxBoundsToShortString(bounds: PxBounds): String {
    return "(${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})px"
}

private fun StringBuilder.appendConfigInfo(config: SemanticsConfiguration, indent: String = "") {
    for ((key, value) in config) {
        if (key == SemanticsProperties.TestTag) {
            continue
        }

        appendLine()
        append(indent)
        append(key.name)
        append(" = '")

        if (value is AnnotatedString) {
            if (value.paragraphStyles.isEmpty() && value.spanStyles.isEmpty() && value
                    .getStringAnnotations(0, value.text.length).isEmpty()) {
                append(value.text)
            } else {
                // Save space if we there is text only in the object
                append(value)
            }
        } else {
            append(value)
        }

        append("'")
    }

    if (config.isMergingSemanticsOfDescendants) {
        appendLine()
        append(indent)
        append("MergeDescendants = 'true'")
    }
}
