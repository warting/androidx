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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissBoxState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

@Composable
@ExperimentalWearMaterialApi
fun DemoApp(
    currentDemo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    DisplayDemo(currentDemo, parentDemo, onNavigateTo, onNavigateBack)
}

@Composable
@ExperimentalWearMaterialApi
private fun DisplayDemo(
    demo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit
) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo -> {
            SwipeToDismissBox(
                state = swipeDismissStateWithNavigation(onNavigateBack),
                background = {
                    if (parentDemo != null) {
                        DisplayDemo(parentDemo, null, onNavigateTo, onNavigateBack)
                    }
                }
            ) {
                demo.content(onNavigateBack)
            }
        }
        is DemoCategory -> {
            DisplayDemoList(demo, parentDemo, onNavigateTo, onNavigateBack)
        }
    }
}

@Composable
@ExperimentalWearMaterialApi
internal fun DisplayDemoList(
    category: DemoCategory,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit
) {
    SwipeToDismissBox(
        state = swipeDismissStateWithNavigation(onNavigateBack),
        background = {
            if (parentDemo != null) {
                DisplayDemo(parentDemo, null, onNavigateTo, onNavigateBack)
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.caption1,
                color = Color.White
            )
            Spacer(modifier = Modifier.size(4.dp))
            category.demos.forEach { demo ->
                CompactChip(
                    onClick = { onNavigateTo(demo) },
                    colors = ChipDefaults.secondaryChipColors(),
                    label = {
                        Text(
                            text = demo.title,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 4.dp
                    )
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
@ExperimentalWearMaterialApi
internal fun swipeDismissStateWithNavigation(
    onNavigateBack: () -> Unit
): SwipeToDismissBoxState {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeDismissTarget.Dismissal) {
            state.snapTo(SwipeDismissTarget.Original)
            onNavigateBack()
        }
    }
    return state
}