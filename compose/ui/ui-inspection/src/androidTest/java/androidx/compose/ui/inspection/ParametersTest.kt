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

package androidx.compose.ui.inspection

import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.ParametersTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.GetParameterDetailsCommand
import androidx.compose.ui.inspection.util.GetParametersByAnchorIdCommand
import androidx.compose.ui.inspection.util.GetParametersByIdCommand
import androidx.compose.ui.inspection.util.GetParametersCommand
import androidx.compose.ui.inspection.util.GetUpdateSettingsCommand
import androidx.compose.ui.inspection.util.filter
import androidx.compose.ui.inspection.util.find
import androidx.compose.ui.inspection.util.findMerged
import androidx.compose.ui.inspection.util.findUnmerged
import androidx.compose.ui.inspection.util.resolve
import androidx.compose.ui.inspection.util.toMap
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.collections.listOf
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StringEntry
import org.junit.Rule
import org.junit.Test

@LargeTest
class ParametersTest {
    @get:Rule val rule = ComposeInspectionRule(ParametersTestActivity::class)

    @Test
    fun paramList(): Unit = runBlocking {
        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val text = composables.filter("Text").first()
        val params =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, text.id))
                .getParametersResponse

        assertThat(params.parameterGroup.parameterList.map { it.name.resolve(params) })
            .isEqualTo(
                listOf(
                    "text",
                    "modifier",
                    "color",
                    "fontSize",
                    "fontStyle",
                    "fontWeight",
                    "fontFamily",
                    "letterSpacing",
                    "textDecoration",
                    "textAlign",
                    "lineHeight",
                    "overflow",
                    "softWrap",
                    "maxLines",
                    "minLines",
                    "onTextLayout",
                    "style",
                )
            )
    }

    @Test
    fun resource(): Unit = runBlocking {
        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val text = composables.filter("Text").first()
        val params =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, text.id))
                .getParametersResponse

        val resourceValue = params.find("fontFamily").resourceValue
        assertThat(resourceValue.type.resolve(params)).isEqualTo("font")
        assertThat(resourceValue.namespace.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.test")
        assertThat(resourceValue.name.resolve(params)).isEqualTo("samplefont")
    }

    @Test
    fun lambda(): Unit = runBlocking {
        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val buttons = composables.filter("Button")
        val buttonId = buttons.first().id
        val params =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, buttonId))
                .getParametersResponse

        val lambdaValue = params.find("onClick").lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(46)
        assertThat(lambdaValue.endLineNumber).isEqualTo(46)
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun contentLambda(): Unit = runBlocking {
        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val buttons = composables.filter("SomeContent")
        val someId = buttons.single().id
        val params =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, someId))
                .getParametersResponse

        val lambdaValue = params.find("content").lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(50)
        assertThat(lambdaValue.endLineNumber).isEqualTo(50)
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun functionType(): Unit = runBlocking {
        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val buttons = composables.filter("Button")
        val buttonId = buttons.last().id
        val params =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, buttonId))
                .getParametersResponse

        val lambdaValue = params.find("onClick").lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(47)
        assertThat(lambdaValue.endLineNumber).isEqualTo(47)
        assertThat(lambdaValue.functionName.resolve(params)).isEqualTo("testClickHandler")
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun testIntArrayWithoutDelayedExtraction() = intArray(useDelayedParameterExtraction = false)

    @Test fun testIntArrayWithDelayedExtraction() = intArray(useDelayedParameterExtraction = true)

    private fun intArray(useDelayedParameterExtraction: Boolean): Unit = runBlocking {
        setUpDelayedExtraction(useDelayedParameterExtraction)

        val tester = rule.inspectorTester
        val nodes = tester.sendCommand(GetComposablesCommand(rule.rootId)).getComposablesResponse

        val function = nodes.filter("FunctionWithIntArray").single()
        val paramResponse =
            tester.sendCommand(
                GetParametersCommand(rule.rootId, function, useDelayedParameterExtraction)
            )
        val params = paramResponse.getParametersResponse

        val intArray = params.find("intArray")
        var strings = params.stringsList

        checkStringParam(strings, intArray, "intArray", "IntArray[8]", 0)
        assertThat(intArray.elementsCount).isEqualTo(5)
        checkIntParam(strings, intArray.elementsList[0], "[0]", 10, 0)
        checkIntParam(strings, intArray.elementsList[1], "[1]", 11, 1)
        checkIntParam(strings, intArray.elementsList[2], "[2]", 12, 2)
        checkIntParam(strings, intArray.elementsList[3], "[3]", 13, 3)
        checkIntParam(strings, intArray.elementsList[4], "[4]", 14, 4)

        val reference = intArray.reference.toBuilder()
        // Remove the id/anchor we should NOT use for lookup of the node
        if (useDelayedParameterExtraction) {
            reference.clearComposableId()
        } else {
            reference.clearAnchorHash()
        }

        val expanded =
            tester
                .sendCommand(
                    GetParameterDetailsCommand(
                        rule.rootId,
                        reference.build(),
                        startIndex = 5,
                        maxElements = 5,
                    )
                )
                .getParameterDetailsResponse
        val intArray2 = expanded.parameter
        strings = expanded.stringsList
        checkStringParam(strings, intArray2, "intArray", "IntArray[8]", 0)
        assertThat(intArray2.elementsCount).isEqualTo(3)
        checkIntParam(strings, intArray2.elementsList[0], "[5]", 15, 5)
        checkIntParam(strings, intArray2.elementsList[1], "[6]", 16, 6)
        checkIntParam(strings, intArray2.elementsList[2], "[7]", 17, 7)
    }

    @Test
    fun testUnmergedSemanticsWithoutDelayedExtraction() =
        unmergedSemantics(useDelayedParameterExtraction = false)

    @Test
    fun testUnmergedSemanticsWithDelayedExtraction() =
        unmergedSemantics(useDelayedParameterExtraction = true)

    private fun unmergedSemantics(useDelayedParameterExtraction: Boolean): Unit = runBlocking {
        setUpDelayedExtraction(useDelayedParameterExtraction)

        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val texts = composables.filter("Text")
        val textOne = texts.first()
        val params =
            rule.inspectorTester
                .sendCommand(
                    GetParametersCommand(rule.rootId, textOne, useDelayedParameterExtraction)
                )
                .getParametersResponse

        val text = params.findUnmerged("Text")
        assertThat(text.type).isEqualTo(Parameter.Type.ITERABLE)
        val strings = params.stringsList
        val first = text.elementsList[0]

        assertThat(strings.toMap()[text.name]).isEqualTo("Text")
        assertThat(strings.toMap()[first.int32Value]).isEqualTo("one")
        assertThat(text.elementsList.size).isEqualTo(1)
    }

    @Test
    fun testMergedSemanticsWithoutDelayedExtraction() =
        mergedSemantics(useDelayedParameterExtraction = false)

    @Test
    fun testMergedSemanticsWithDelayedExtraction() =
        mergedSemantics(useDelayedParameterExtraction = true)

    private fun mergedSemantics(useDelayedParameterExtraction: Boolean): Unit = runBlocking {
        setUpDelayedExtraction(useDelayedParameterExtraction)

        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse

        val textFirst = composables.filter("Column").first()
        val params =
            rule.inspectorTester
                .sendCommand(
                    GetParametersCommand(rule.rootId, textFirst, useDelayedParameterExtraction)
                )
                .getParametersResponse

        val text = params.findMerged("Text")
        val strings = params.stringsList

        assertThat(text.type).isEqualTo(Parameter.Type.ITERABLE)
        val first = text.elementsList[0]
        val second = text.elementsList[1]
        val third = text.elementsList[2]

        assertThat(strings.toMap()[text.name]).isEqualTo("Text")
        assertThat(strings.toMap()[first.int32Value]).isEqualTo("three")
        assertThat(strings.toMap()[second.int32Value]).isEqualTo("four")
        assertThat(strings.toMap()[third.int32Value]).isEqualTo("five")
        assertThat(text.elementsList.size).isEqualTo(3)

        val row = composables.filter("Row").single()
        val params2 =
            rule.inspectorTester
                .sendCommand(GetParametersCommand(rule.rootId, row, useDelayedParameterExtraction))
                .getParametersResponse

        val texts2 = params2.findMerged("Text")
        val strings2 = params2.stringsList

        checkStringParam(strings2, texts2, "Text", "List[8]", 0)
        assertThat(texts2.elementsCount).isEqualTo(5)
        checkStringParam(strings2, texts2.elementsList[0], "[0]", "Text1", 0)
        checkStringParam(strings2, texts2.elementsList[1], "[1]", "Text2", 1)
        checkStringParam(strings2, texts2.elementsList[2], "[2]", "Text3", 2)
        checkStringParam(strings2, texts2.elementsList[3], "[3]", "Text4", 3)
        checkStringParam(strings2, texts2.elementsList[4], "[4]", "Text5", 4)

        val reference = texts2.reference.toBuilder()
        val expanded =
            rule.inspectorTester
                .sendCommand(
                    GetParameterDetailsCommand(
                        rule.rootId,
                        reference.build(),
                        startIndex = 5,
                        maxElements = 5,
                    )
                )
                .getParameterDetailsResponse
        val texts3 = expanded.parameter
        val strings3 = expanded.stringsList
        checkStringParam(strings3, texts3, "Text", "List[8]", 0)
        assertThat(texts3.elementsCount).isEqualTo(3)
        checkStringParam(strings3, texts3.elementsList[0], "[5]", "Text6", 5)
        checkStringParam(strings3, texts3.elementsList[1], "[6]", "Text7", 6)
        checkStringParam(strings3, texts3.elementsList[2], "[7]", "Text8", 7)
    }

    @Test
    fun delayedExtraction(): Unit = runBlocking {
        setUpDelayedExtraction(true)

        val composables =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId))
                .getComposablesResponse
        var strings = composables.stringsList.toMap()
        var column = composables.filter("Column").first()
        var text = column.childrenList.single { strings[it.name] == "Text" }

        // Lookup by anchor will find the parameters
        var paramsByAnchor =
            rule.inspectorTester
                .sendCommand(GetParametersByAnchorIdCommand(rule.rootId, text.anchorHash, text.id))
                .getParametersResponse
        strings = paramsByAnchor.stringsList.toMap()
        assertThat(paramsByAnchor.parameterGroup.parameterList).isNotEmpty()
        var textValue = paramsByAnchor.find("text")
        assertThat(strings[textValue.int32Value]).isEqualTo("four")

        // Lookup parameters without anchor should fallback to the older approach and return
        // the same parameters:
        var paramsById =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, text.id))
                .getParametersResponse
        // We are using delayed parameter extractions so the cache does not have parameters
        // (The code should look for an anchor but the anchor is not specified.)
        assertThat(paramsById.parameterGroup.parameterList).isNotEmpty()
        textValue = paramsById.find("text")
        assertThat(strings[textValue.int32Value]).isEqualTo("four")

        val snapshot =
            rule.inspectorTester
                .sendCommand(GetComposablesCommand(rule.rootId, extractAllParameters = true))
                .getComposablesResponse
        strings = snapshot.stringsList.toMap()
        column = snapshot.filter("Column").first()
        text = column.childrenList.single { strings[it.name] == "Text" }

        paramsById =
            rule.inspectorTester
                .sendCommand(GetParametersByIdCommand(rule.rootId, text.id))
                .getParametersResponse
        // Even when using delayed parameter extractions, use the cache if it contains all params:
        strings = paramsById.stringsList.toMap()
        assertThat(paramsById.parameterGroup.parameterList).isNotEmpty()
        textValue = paramsById.find("text")
        assertThat(strings[textValue.int32Value]).isEqualTo("four")

        // Looking up by anchor should find the parameters
        // (The code should use the cached values.)
        paramsByAnchor =
            rule.inspectorTester
                .sendCommand(GetParametersByAnchorIdCommand(rule.rootId, text.anchorHash, text.id))
                .getParametersResponse
        assertThat(paramsByAnchor.parameterGroup.parameterList).isNotEmpty()
    }

    private suspend fun setUpDelayedExtraction(useDelayedParameterExtraction: Boolean) {
        if (useDelayedParameterExtraction) {
            val updated =
                rule.inspectorTester
                    .sendCommand(GetUpdateSettingsCommand(delayParameterExtractions = true))
                    .updateSettingsResponse
            assertThat(updated.canDelayParameterExtractions).isTrue()
        }
    }
}

@Suppress("SameParameterValue")
private fun checkStringParam(
    stringList: List<StringEntry>,
    param: Parameter,
    name: String,
    value: String,
    index: Int = 0,
) {
    assertThat(stringList.toMap()[param.name]).isEqualTo(name)
    assertThat(stringList.toMap()[param.int32Value]).isEqualTo(value)
    assertThat(param.index).isEqualTo(index)
}

private fun checkIntParam(
    stringList: List<StringEntry>,
    param: Parameter,
    name: String,
    value: Int,
    index: Int = 0,
) {
    assertThat(stringList.toMap()[param.name]).isEqualTo(name)
    assertThat(param.int32Value).isEqualTo(value)
    assertThat(param.index).isEqualTo(index)
}
