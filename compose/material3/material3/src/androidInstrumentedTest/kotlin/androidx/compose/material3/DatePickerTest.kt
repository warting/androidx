/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePickerDefaults.defaultDatePickerColors
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.createCalendarModel
import androidx.compose.material3.internal.formatWithSkeleton
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class DatePickerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun dateSelectionWithInitialDate() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(
                    initialSelectedDateMillis = initialDateMillis,
                    initialDisplayedMonthMillis = monthInUtcMillis,
                )
            DatePicker(state = datePickerState)
        }

        // Select the 11th day of the displayed month is selected.
        rule.onNode(hasText("11", substring = true) and hasClickAction()).assertIsSelected()
        rule.onNodeWithText("May 11, 2010").assertExists()
    }

    @Test
    fun dateSelection() {
        lateinit var defaultHeadline: String
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DatePickerHeadline)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DatePicker(state = datePickerState)
        }

        rule.onNodeWithText(defaultHeadline).assertExists()

        // Select the 27th day of the displayed month.
        rule.onNode(hasText("27", substring = true) and hasClickAction()).assertIsNotSelected()
        rule.onNode(hasText("27", substring = true) and hasClickAction()).performClick()

        rule.runOnIdle {
            assertThat(datePickerState.selectedDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 27))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(datePickerState.getSelectedDate()).isEqualTo(LocalDate.of(2019, 1, 27))
            }
        }

        rule.onNodeWithText(defaultHeadline).assertDoesNotExist()
        rule.onNodeWithText("Jan 27, 2019").assertExists()
        rule.onNode(hasText("27", substring = true) and hasClickAction()).assertIsSelected()
    }

    @Test
    fun blockedDateSelection() {
        lateinit var defaultHeadline: String
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DatePickerHeadline)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis,
                    selectableDates =
                        object : SelectableDates {
                            // All dates are invalid for the sake of this test.
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean = false
                        },
                )
            DatePicker(state = datePickerState)
        }

        rule.onNodeWithText(defaultHeadline).assertExists()

        // Select the 27th day of the displayed month.
        rule.onNode(hasText("27", substring = true) and hasClickAction()).performClick()

        rule.runOnIdle {
            assertThat(datePickerState.selectedDateMillis).isNull()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(datePickerState.getSelectedDate()).isNull()
            }
        }

        rule.onNodeWithText(defaultHeadline).assertExists()
    }

    @Test
    fun blockedYearSelection() {
        lateinit var datePickerState: DatePickerState
        lateinit var yearDescriptionFormat: String
        rule.setMaterialContent(lightColorScheme()) {
            yearDescriptionFormat = getString(string = Strings.DatePickerNavigateToYearDescription)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis,
                    selectableDates =
                        object : SelectableDates {
                            // All years are invalid for the sake of this test.
                            override fun isSelectableYear(year: Int): Boolean = false
                        },
                )
            DatePicker(state = datePickerState)
        }

        // Open the years selection and sample a few years to ensure they are disabled.
        rule.onNodeWithText("January 2019").performClick()
        rule.waitForIdle()
        (2019..2031).forEach { year ->
            rule.onNodeWithText(yearDescriptionFormat.format(year)).assertIsNotEnabled()
        }
    }

    @Test
    fun selectableDates_updatedSelection() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val allEnabled =
                object : SelectableDates {
                    // All dates are valid.
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                }
            val allDisabled =
                object : SelectableDates {
                    // All dates are invalid.
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = false
                }

            var selectableDates: SelectableDates by remember { mutableStateOf(allEnabled) }
            Column {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
                datePickerState =
                    rememberDatePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis,
                        selectableDates = selectableDates,
                    )
                DatePicker(state = datePickerState)
                Button(
                    onClick = { selectableDates = allDisabled },
                    modifier = Modifier.testTag("disableSelection"),
                ) {
                    Text("Disable selection")
                }
            }
        }

        // Assert that the 27th of the month is enabled.
        rule.onNode(hasText("27", substring = true) and hasClickAction()).assertIsEnabled()

        // Click the button to disable all dates.
        rule.onNodeWithTag("disableSelection").performClick()
        rule.waitForIdle()

        // Assert that the 27th of the month is disabled.
        rule.onNode(hasText("27", substring = true) and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun yearSelection() {
        lateinit var datePickerState: DatePickerState
        lateinit var navigateToYearFormat: String
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            navigateToYearFormat = getString(string = Strings.DatePickerNavigateToYearDescription)
            DatePicker(state = datePickerState)
        }

        rule.onNodeWithText("January 2019").performClick()
        rule.onNodeWithText(navigateToYearFormat.format(2019)).assertIsSelected()
        rule.onNodeWithText(navigateToYearFormat.format(2020)).performClick()
        // Select the 15th day of the displayed month in 2020.
        rule
            .onAllNodes(hasText("15", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        rule.runOnIdle {
            assertThat(datePickerState.selectedDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 15))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(datePickerState.getSelectedDate()).isEqualTo(LocalDate.of(2020, 1, 15))
            }
        }

        // Check that if the years are opened again, the last selected year is still marked as such
        rule.onNodeWithText("January 2020").performClick()
        rule.onNodeWithText(navigateToYearFormat.format(2019)).assertIsNotSelected()
        rule.onNodeWithText(navigateToYearFormat.format(2020)).assertIsSelected()
    }

    @Test
    fun yearRange() {
        lateinit var navigateToYearFormat: String
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            navigateToYearFormat = getString(string = Strings.DatePickerNavigateToYearDescription)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis,
                        // Limit the years selection to 2018-2023
                        yearRange = IntRange(2018, 2023),
                    )
            )
        }

        rule.onNodeWithText("January 2019").performClick()
        (2018..2023).forEach { year ->
            rule.onNodeWithText(navigateToYearFormat.format(year)).assertExists()
        }
        rule.onNodeWithText(navigateToYearFormat.format(2017)).assertDoesNotExist()
        rule.onNodeWithText(navigateToYearFormat.format(2024)).assertDoesNotExist()
    }

    @Test
    fun yearRange_minYearAfterCurrentYear() {
        var currentYear = 0
        rule.setMaterialContent(lightColorScheme()) {
            currentYear = createCalendarModel(Locale.getDefault()).today.year
            DatePicker(
                state =
                    rememberDatePickerState(yearRange = IntRange(currentYear + 1, currentYear + 10))
            )
        }

        rule.onNodeWithText("January ${currentYear + 1}").assertIsDisplayed()
    }

    @Test
    fun monthsTraversal() {
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2018, month = 1, dayOfMonth = 1)
            DatePicker(
                state = rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            )
        }

        rule.onNodeWithText("January 2018").assertExists()
        // Click the next month arrow button
        rule
            .onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()

        // Check that the current month's menu button content was changed.
        rule.onNodeWithText("February 2018").assertExists()
        rule.onNodeWithText("January 2018").assertDoesNotExist()

        // Click the previous month arrow button
        rule
            .onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()

        // Check that we are back to the original month
        rule.onNodeWithText("January 2018").assertExists()
        rule.onNodeWithText("February 2018").assertDoesNotExist()
    }

    @Test
    fun monthsTraversalAtRangeEdges() {
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2018, month = 1, dayOfMonth = 1)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis,
                        // Limit the years to just 2018
                        yearRange = IntRange(2018, 2018),
                    )
            )
        }

        // Assert that we can only click next at the initial state.
        val nextMonthButton =
            rule.onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
        nextMonthButton.assertIsEnabled()
        val previousMonthButton =
            rule.onNodeWithContentDescription(
                label = "previous",
                substring = true,
                ignoreCase = true,
            )
        previousMonthButton.assertIsNotEnabled()

        // Click 11 times next and assert that we can only click previous.
        repeat(11) {
            nextMonthButton.performClick()
            previousMonthButton.assertIsEnabled()
        }
        nextMonthButton.assertIsNotEnabled()
    }

    @Test
    fun switchToDateInput() {
        lateinit var switchToInputDescription: String
        lateinit var dateInputLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            switchToInputDescription = getString(string = Strings.DatePickerSwitchToInputMode)
            dateInputLabel = getString(string = Strings.DateInputLabel)
            DatePicker(state = rememberDatePickerState())
        }

        // Click to switch to DateInput.
        rule.onNodeWithContentDescription(label = switchToInputDescription).performClick()

        rule.waitForIdle()
        rule.onNodeWithText(dateInputLabel).assertIsDisplayed()
        rule
            .onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        rule
            .onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun datePicker_swipeLeft_goesToNextMonth() {
        val day = dayInUtcMilliseconds(year = 2018, month = 1, dayOfMonth = 1)

        rule.setMaterialContent(lightColorScheme()) {
            DatePicker(
                modifier = Modifier.testTag("datePicker"),
                state =
                    rememberDatePickerState(
                        initialSelectedDateMillis = day,
                        yearRange = IntRange(2016, 2019),
                    ),
            )
        }

        rule.onAllNodes(hasText("Feb", substring = true)).assertCountEquals(0)

        rule.onNodeWithTag("datePicker").performTouchInput { swipeLeft(center.x, centerLeft.x) }

        rule.onAllNodes(hasText("Feb", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun state_initWithoutRemember() {
        val datePickerState =
            DatePickerState(
                locale = Locale.getDefault(),
                initialSelectedDateMillis = 1649721600000L, // 04/12/2022
            )
        with(datePickerState) {
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            // Using the JVM Locale.getDefault() for testing purposes only.
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            assertThat(locale).isEqualTo(Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
        }
    }

    @Test
    fun state_initWithSelectedDate() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState = rememberDatePickerState(initialSelectedDateMillis = 1649721600000L)
        }
        with(datePickerState) {
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            // Using the JVM Locale.getDefault() for testing purposes only.
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
        }
    }

    @Test
    fun state_initWithSelectedDate_roundingToStartDay() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = 1649721600000L + 10000L)
        }
        with(datePickerState) {
            // Assert that the actual selectedDateMillis was rounded down to the start of day
            // timestamp
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
        }
    }

    @Test
    fun state_initWithSelectedDateAndNullMonth() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState =
                rememberDatePickerState(
                    initialSelectedDateMillis = 1649721600000L,
                    initialDisplayedMonthMillis = null,
                )
        }

        with(datePickerState) {
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            // Assert that the displayed month is the current month as of today.
            // Using the JVM Locale.getDefault() for testing purposes only.
            val calendarModel = createCalendarModel(Locale.getDefault())
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    calendarModel.getMonth(calendarModel.today.utcTimeMillis).startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.now())
            }
        }
    }

    @Test
    fun state_initWithNulls() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            datePickerState =
                rememberDatePickerState(
                    initialSelectedDateMillis = null,
                    initialDisplayedMonthMillis = null,
                )
        }

        with(datePickerState) {
            assertThat(selectedDateMillis).isNull()
            // Assert that the displayed month is the current month as of today.
            val calendarModel = createCalendarModel(Locale.getDefault())
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    calendarModel.getMonth(calendarModel.today.utcTimeMillis).startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isNull()
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.now())
            }
        }
    }

    @Test
    fun state_resetSelection() {
        lateinit var defaultHeadline: String
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DatePickerHeadline)
            // 04/12/2022
            datePickerState = rememberDatePickerState(initialSelectedDateMillis = 1649721600000L)
            DatePicker(state = datePickerState)
        }
        rule.onNodeWithText("Apr 12, 2022").assertExists()
        with(datePickerState) {
            assertThat(selectedDateMillis).isEqualTo(1649721600000L)
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }

            // Reset the selection
            datePickerState.selectedDateMillis = null
            assertThat(selectedDateMillis).isNull()
            rule.onNodeWithText("Apr 12, 2022").assertDoesNotExist()
            rule.onNodeWithText(defaultHeadline).assertExists()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedDate()).isNull()
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_resetSelection_withLocalDate() {
        lateinit var defaultHeadline: String
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DatePickerHeadline)
            // 04/12/2022
            datePickerState = rememberDatePickerState(initialSelectedDateMillis = 1649721600000L)
            DatePicker(state = datePickerState)
        }
        rule.onNodeWithText("Apr 12, 2022").assertExists()
        with(datePickerState) {
            assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
            assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))

            // Reset the selection
            datePickerState.setSelectedDate(null)
            assertThat(getSelectedDate()).isNull()
            assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            rule.onNodeWithText("Apr 12, 2022").assertDoesNotExist()
            rule.onNodeWithText(defaultHeadline).assertExists()
        }
    }

    @Test
    fun state_restoresDatePickerState() {
        val restorationTester = StateRestorationTester(rule)
        var datePickerState: DatePickerState? = null
        restorationTester.setContent { datePickerState = rememberDatePickerState() }

        // Using the JVM Locale.getDefault() for testing purposes only.
        val calendarModel = createCalendarModel(Locale.getDefault())
        with(datePickerState!!) {
            val date = calendarModel.getCanonicalDate(1649721600000L) // 04/12/2022
            val displayedMonth = calendarModel.getMonth(date)
            rule.runOnIdle {
                selectedDateMillis = date.utcTimeMillis
                displayedMonthMillis = displayedMonth.startUtcTimeMillis
            }

            datePickerState = null

            restorationTester.emulateSavedInstanceStateRestore()

            rule.runOnIdle {
                assertThat(selectedDateMillis).isEqualTo(date.utcTimeMillis)
                assertThat(displayedMonthMillis).isEqualTo(displayedMonth.startUtcTimeMillis)
                assertThat(datePickerState!!.selectedDateMillis).isEqualTo(1649721600000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    assertThat(getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                    assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
                }
            }
        }
    }

    @Test
    fun state_changeDisplayedMonth() {
        var futureMonthInUtcMillis = 0L
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 1)
            futureMonthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 7, dayOfMonth = 1)
            state = rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DatePicker(state = state)
        }

        rule.onNodeWithText("January 2020").assertExists()

        // Update the displayed month to be ~6 months in the future.
        state.displayedMonthMillis = futureMonthInUtcMillis

        rule.waitForIdle()
        rule.onNodeWithText("July 2020").assertExists()

        // Check that clicking "next" and "previous" traverses the month range correctly.
        rule
            .onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()
        rule.onNodeWithText("August 2020").assertExists()
        rule
            .onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()

        // Check that we are back to the original month
        rule.onNodeWithText("July 2020").assertExists()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_changeDisplayedMonth_withYearMonth() {
        lateinit var futureMonth: YearMonth
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 1)
            futureMonth = YearMonth.of(2020, 7)
            state = rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DatePicker(state = state)
        }

        rule.onNodeWithText("January 2020").assertExists()

        // Update the displayed month to be ~6 months in the future.
        state.setDisplayedMonth(futureMonth)

        rule.waitForIdle()
        rule.onNodeWithText("July 2020").assertExists()

        // Check that clicking "next" and "previous" traverses the month range correctly.
        rule
            .onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()
        rule.onNodeWithText("August 2020").assertExists()
        rule
            .onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .performClick()
        rule.waitForIdle()

        // Check that we are back to the original month
        rule.onNodeWithText("July 2020").assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_initWithJavaTimeApi() {
        val dateInUtcMillis = dayInUtcMilliseconds(year = 2022, month = 4, dayOfMonth = 12)
        val monthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 1)
        lateinit var datePickerState: DatePickerState
        lateinit var datePickerStateWithJavaTimeApi: DatePickerState
        rule.setContent {
            datePickerState =
                rememberDatePickerState(
                    initialSelectedDateMillis = dateInUtcMillis,
                    initialDisplayedMonthMillis = monthInUtcMillis,
                )
            datePickerStateWithJavaTimeApi =
                rememberDatePickerState(
                    initialSelectedDate = LocalDate.of(2022, 4, 12),
                    initialDisplayedMonth = YearMonth.of(2020, 1),
                )
        }

        // Assert that we get the same results for both states.
        assertThat(datePickerState.selectedDateMillis).isEqualTo(dateInUtcMillis)
        assertThat(datePickerState.displayedMonthMillis).isEqualTo(monthInUtcMillis)
        assertThat(datePickerStateWithJavaTimeApi.selectedDateMillis).isEqualTo(dateInUtcMillis)
        assertThat(datePickerStateWithJavaTimeApi.displayedMonthMillis).isEqualTo(monthInUtcMillis)

        assertThat(datePickerState.getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(datePickerState.getDisplayedMonth()).isEqualTo(YearMonth.of(2020, 1))
        assertThat(datePickerStateWithJavaTimeApi.getSelectedDate())
            .isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(datePickerStateWithJavaTimeApi.getDisplayedMonth())
            .isEqualTo(YearMonth.of(2020, 1))

        assertThat(datePickerState.yearRange).isEqualTo(datePickerStateWithJavaTimeApi.yearRange)
        assertThat(datePickerState.displayMode)
            .isEqualTo(datePickerStateWithJavaTimeApi.displayMode)
        assertThat(datePickerState.locale).isEqualTo(datePickerStateWithJavaTimeApi.locale)
        assertThat(datePickerState.selectableDates)
            .isEqualTo(datePickerStateWithJavaTimeApi.selectableDates)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_initWithJavaTimeApi_withoutRemember() {
        val dateInUtcMillis = dayInUtcMilliseconds(year = 2022, month = 4, dayOfMonth = 12)
        val monthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 1)
        val datePickerState =
            DatePickerState(
                locale = Locale.getDefault(),
                initialSelectedDateMillis = dateInUtcMillis,
                initialDisplayedMonthMillis = monthInUtcMillis,
            )
        val datePickerStateWithJavaTimeApi =
            DatePickerState(
                Locale.getDefault(),
                initialSelectedDate = LocalDate.of(2022, 4, 12),
                initialDisplayedMonth = YearMonth.of(2020, 1),
            )

        // Assert that we get the same results for both states.
        assertThat(datePickerState.selectedDateMillis).isEqualTo(dateInUtcMillis)
        assertThat(datePickerState.displayedMonthMillis).isEqualTo(monthInUtcMillis)
        assertThat(datePickerStateWithJavaTimeApi.selectedDateMillis).isEqualTo(dateInUtcMillis)
        assertThat(datePickerStateWithJavaTimeApi.displayedMonthMillis).isEqualTo(monthInUtcMillis)

        assertThat(datePickerState.getSelectedDate()).isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(datePickerState.getDisplayedMonth()).isEqualTo(YearMonth.of(2020, 1))
        assertThat(datePickerStateWithJavaTimeApi.getSelectedDate())
            .isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(datePickerStateWithJavaTimeApi.getDisplayedMonth())
            .isEqualTo(YearMonth.of(2020, 1))

        assertThat(datePickerState.yearRange).isEqualTo(datePickerStateWithJavaTimeApi.yearRange)
        assertThat(datePickerState.displayMode)
            .isEqualTo(datePickerStateWithJavaTimeApi.displayMode)
        assertThat(datePickerState.locale).isEqualTo(datePickerStateWithJavaTimeApi.locale)
        assertThat(datePickerState.selectableDates)
            .isEqualTo(datePickerStateWithJavaTimeApi.selectableDates)
    }

    @Test
    fun setSelection_outOfYearsBound() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            datePickerState = rememberDatePickerState(yearRange = IntRange(2000, 2050))
        }

        // Setting the selection to a year that is out of range.
        datePickerState.selectedDateMillis =
            dayInUtcMilliseconds(year = 1999, month = 5, dayOfMonth = 11)

        // Expecting a null selected date as the provided date was out of range.
        assertThat(datePickerState.selectedDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(datePickerState.getSelectedDate()).isNull()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun setSelection_outOfYearsBound_withLocalDate() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            datePickerState = rememberDatePickerState(yearRange = IntRange(2000, 2050))
        }

        // Setting the selection to a year that is out of range.
        datePickerState.setSelectedDate(LocalDate.of(1999, 5, 11))

        // Expecting a null selected date as the provided date was out of range.
        assertThat(datePickerState.getSelectedDate()).isNull()
    }

    @Test
    fun initialDateOutOfBounds() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2051, month = 5, dayOfMonth = 11)
            datePickerState =
                rememberDatePickerState(
                    initialSelectedDateMillis = initialDateMillis,
                    yearRange = IntRange(2000, 2050),
                )
        }

        // Expecting a null selected date as the initial date is out of range.
        assertThat(datePickerState.selectedDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(datePickerState.getSelectedDate()).isNull()
        }
    }

    @Test
    fun initialDisplayedMonthOutObBounds() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 1999, month = 1, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis,
                    yearRange = IntRange(2000, 2050),
                )
            DatePicker(state = datePickerState)
        }

        val calendarModel = createCalendarModel(Locale.getDefault())

        // Check that the initial displayed month is the current month.
        assertThat(datePickerState.displayedMonthMillis)
            .isEqualTo(calendarModel.getMonth(calendarModel.today).startUtcTimeMillis)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(datePickerState.getDisplayedMonth()).isEqualTo(YearMonth.now())
        }
    }

    @Test
    fun defaultSemantics() {
        val selectedDateInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
        val monthInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 1)
        lateinit var expectedHeadlineStringFormat: String
        rule.setMaterialContent(lightColorScheme()) {
            // e.g. "Current selection: %1$s"
            expectedHeadlineStringFormat = getString(Strings.DatePickerHeadlineDescription)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialSelectedDateMillis = selectedDateInUtcMillis,
                        initialDisplayedMonthMillis = monthInUtcMillis,
                    )
            )
        }

        val fullDateDescription =
            formatWithSkeleton(
                selectedDateInUtcMillis,
                DatePickerDefaults.YearMonthWeekdayDaySkeleton,
                Locale.US,
                cache = mutableMapOf(),
            )

        rule
            .onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule
            .onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithText("May 2010").assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule
            .onNode(hasText(fullDateDescription) and hasClickAction())
            .assert(expectValue(SemanticsProperties.Role, Role.Button))
        rule
            .onNodeWithText("May 11, 2010")
            .assertContentDescriptionEquals(
                expectedHeadlineStringFormat.format(fullDateDescription)
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun customColorsSupersedeTypographyColors() {
        lateinit var datePickerState: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 1)
            datePickerState =
                rememberDatePickerState(
                    initialSelectedDateMillis = initialDateMillis,
                    initialDisplayedMonthMillis = monthInUtcMillis,
                )
            // Wrap in a MaterialTheme that has a typography that was set with custom colors.
            // The date picker is using BodyLarge for days. See
            // DatePickerModalTokens.DateLabelTextFont.
            MaterialTheme(
                typography =
                    MaterialTheme.typography.copy(
                        // Set the body-large with a yellow color. We would like to test that no
                        // yellow appears on any day.
                        bodyLarge = TextStyle(fontSize = 30.sp, color = Color.Yellow),
                        headlineLarge = TextStyle(fontSize = 30.sp, color = Color.Green),
                    )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors =
                        MaterialTheme.colorScheme.defaultDatePickerColors.copy(
                            dayContentColor = Color.Blue,
                            selectedDayContentColor = Color.Red,
                            headlineContentColor = Color.Yellow,
                        ),
                )
            }
        }

        // Select the 11th day of the displayed month is selected and is with a red content color.
        rule
            .onNode(hasText("11", substring = true) and hasClickAction())
            .captureToImage()
            .assertDoesNotContainColor(Color.Yellow)
            .assertContainsColor(Color.Red)

        // Any other day should have a blue content color.
        rule
            .onNode(hasText("12", substring = true) and hasClickAction())
            .captureToImage()
            .assertDoesNotContainColor(Color.Yellow)
            .assertContainsColor(Color.Blue)

        // The headline color should the yellow, as we override the typography green color for
        // "headlineLarge".
        rule.onNodeWithText("May 11, 2010").captureToImage().assertContainsColor(Color.Yellow)
    }

    // Returns the given date's day as milliseconds from epoch. The returned value is for the day's
    // start on midnight.
    private fun dayInUtcMilliseconds(year: Int, month: Int, dayOfMonth: Int): Long {
        val firstDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        firstDayCalendar.clear()
        firstDayCalendar[Calendar.YEAR] = year
        firstDayCalendar[Calendar.MONTH] = month - 1
        firstDayCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
        return firstDayCalendar.timeInMillis
    }
}
