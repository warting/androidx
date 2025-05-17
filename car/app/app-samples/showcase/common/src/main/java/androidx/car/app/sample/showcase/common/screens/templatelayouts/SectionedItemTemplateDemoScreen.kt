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
package androidx.car.app.sample.showcase.common.screens.templatelayouts

import android.util.Log
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/** Demonstrates the usage of the [SectionedItemTemplate]. */
@OptIn(ExperimentalCarApi::class)
class SectionedItemTemplateDemoScreen(carContext: CarContext) : Screen(carContext) {
    companion object {
        private val imageResources =
            listOf(
                R.drawable.ic_baseline_add_alert_24,
                R.drawable.ic_mic,
                R.drawable.ic_bug_report_24px,
                R.drawable.ic_face_24px,
                R.drawable.baseline_directions_boat_filled_24,
                R.drawable.ic_explore_white_24dp,
            )
    }

    override fun onGetTemplate(): Template {
        val builder =
            SectionedItemTemplate.Builder()
                .setHeader(
                    Header.Builder()
                        .setTitle(carContext.getString(R.string.sectioned_item_template_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .build()
                )

        builder.addSection(
            createRowSectionBuilder(
                    sectionTitle =
                        carContext.getString(
                            R.string.sectioned_item_template_radio_button_section_title
                        ),
                    numberOfRows = 5,
                ) { rowBuilder, index ->
                    rowBuilder.setOnClickListener { showToast("Active radio button index: $index") }
                }
                .setAsSelectionGroup(1)
                .build()
        )
        builder.addSection(
            createGridSectionBuilder(
                    sectionTitle =
                        carContext.getString(
                            R.string.sectioned_item_template_grid_item_section_title
                        ),
                    numberOfGridItems = 6,
                )
                .setItemSize(GridSection.ITEM_SIZE_LARGE)
                .build()
        )

        builder.addSection(
            createRowSectionBuilder(
                    sectionTitle =
                        carContext.getString(R.string.sectioned_item_template_toggle_section_title),
                    numberOfRows = 5,
                ) { rowBuilder, index ->
                    rowBuilder.setToggle(
                        Toggle.Builder { isToggled -> showToast("$index: $isToggled") }.build()
                    )
                }
                .build()
        )
        builder.addSection(
            createRowSectionBuilder(
                    sectionTitle =
                        carContext.getString(
                            R.string.sectioned_item_template_lots_of_rows_section_title
                        ),
                    numberOfRows = 150,
                )
                .build()
        )

        return builder.build()
    }

    /**
     * Creates [RowSection.Builder] prepopulated with [numberOfRows] that were created with a
     * default title and subtitle and modified by [rowBuilderAugment].
     *
     * @param[sectionTitle] a string to set as the section's title
     * @param[numberOfRows] the number of rows to generate in this section
     * @param[rowBuilderAugment] augments that are applied to each row as its being built
     */
    private fun createRowSectionBuilder(
        sectionTitle: String,
        numberOfRows: Int,
        rowBuilderAugment: ((builder: Row.Builder, index: Int) -> Unit)? = null,
    ): RowSection.Builder {
        val builder = RowSection.Builder().setTitle(sectionTitle)

        for (i in 0 until numberOfRows) {
            val rowBuilder = Row.Builder().setTitle("Row $i").addText("This is subtext")
            rowBuilderAugment?.invoke(rowBuilder, i)
            builder.addItem(rowBuilder.build())
        }
        return builder
    }

    private fun createGridSectionBuilder(
        sectionTitle: String,
        numberOfGridItems: Int,
        gridBuilderAugment: ((builder: GridItem.Builder, index: Int) -> Unit)? = null,
    ): GridSection.Builder {
        val builder = GridSection.Builder().setTitle(sectionTitle)
        for (i in 0 until numberOfGridItems) {
            val gridBuilder =
                GridItem.Builder()
                    .setTitle("Grid $i")
                    .setImage(
                        CarIcon.Builder(
                                IconCompat.createWithResource(
                                    carContext,
                                    imageResources[i % imageResources.size],
                                )
                            )
                            .setTint(CarColor.PRIMARY)
                            .build()
                    )
            gridBuilderAugment?.invoke(gridBuilder, i)
            builder.addItem(gridBuilder.build())
        }
        return builder
    }

    private fun showToast(text: String) {
        // Toasts are sometimes throttled, so we log as well to guarantee an output somewhere
        Log.i(SectionedItemTemplateDemoScreen::class.simpleName, text)
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }
}
