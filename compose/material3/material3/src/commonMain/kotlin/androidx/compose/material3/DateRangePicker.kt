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

package androidx.compose.material3

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DateRangePickerStateImpl.Companion.Saver
import androidx.compose.material3.internal.CalendarDate
import androidx.compose.material3.internal.CalendarModel
import androidx.compose.material3.internal.CalendarMonth
import androidx.compose.material3.internal.DaysInWeek
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.createCalendarModel
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [Material Design date range picker](https://m3.material.io/components/date-pickers/overview)
 *
 * Date range pickers let people select a range of dates and can be embedded into Dialogs.
 *
 * ![Date range picker
 * image](https://developer.android.com/images/reference/androidx/compose/material3/range-picker.png)
 *
 * A simple DateRangePicker looks like:
 *
 * @sample androidx.compose.material3.samples.DateRangePickerSample
 *
 * A DateRangePicker can also be initialized with Java Time APIs when running on Android with API 26
 * and above:
 *
 * @sample androidx.compose.material3.samples.DateRangePickerApi26Sample
 * @param state state of the date range picker. See [rememberDateRangePickerState].
 * @param modifier the [Modifier] to be applied to this date range picker
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date range
 *   picker in different states. See [DatePickerDefaults.colors].
 * @param title the title to be displayed in the date range picker
 * @param headline the headline to be displayed in the date range picker
 * @param showModeToggle indicates if this DateRangePicker should show a mode toggle action that
 *   transforms it into a date range input
 * @param focusRequester a focus requester that will be used to focus the text field when the date
 *   picker is in an input mode. Pass `null` to not focus the text field if that's the desired
 *   behavior.
 */
@Composable
fun DateRangePicker(
    state: DateRangePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
    colors: DatePickerColors = DatePickerDefaults.colors(),
    title: (@Composable () -> Unit)? = {
        DateRangePickerDefaults.DateRangePickerTitle(
            displayMode = state.displayMode,
            modifier = Modifier.padding(DateRangePickerTitlePadding),
            contentColor = colors.titleContentColor,
        )
    },
    headline: (@Composable () -> Unit)? = {
        DateRangePickerDefaults.DateRangePickerHeadline(
            selectedStartDateMillis = state.selectedStartDateMillis,
            selectedEndDateMillis = state.selectedEndDateMillis,
            displayMode = state.displayMode,
            dateFormatter,
            modifier = Modifier.padding(DateRangePickerHeadlinePadding),
            contentColor = colors.headlineContentColor,
        )
    },
    showModeToggle: Boolean = true,
    focusRequester: FocusRequester? = remember { FocusRequester() },
) {
    val calendarModel =
        remember(state.locale) {
            if (state is BaseDatePickerStateImpl) {
                state.calendarModel
            } else {
                createCalendarModel(state.locale)
            }
        }
    DateEntryContainer(
        modifier = modifier,
        title = title,
        headline = headline,
        modeToggleButton =
            if (showModeToggle) {
                {
                    DisplayModeToggleButton(
                        modifier = Modifier.padding(DatePickerModeTogglePadding),
                        displayMode = state.displayMode,
                        onDisplayModeChange = { displayMode -> state.displayMode = displayMode },
                        colors = colors,
                    )
                }
            } else {
                null
            },
        headlineTextStyle = DatePickerModalTokens.RangeSelectionHeaderHeadlineFont.value,
        headerMinHeight =
            DatePickerModalTokens.RangeSelectionHeaderContainerHeight - HeaderHeightOffset,
        colors = colors,
    ) {
        SwitchableDateEntryContent(
            selectedStartDateMillis = state.selectedStartDateMillis,
            selectedEndDateMillis = state.selectedEndDateMillis,
            displayedMonthMillis = state.displayedMonthMillis,
            displayMode = state.displayMode,
            onDatesSelectionChange = { startDateMillis, endDateMillis ->
                try {
                    state.setSelection(
                        startDateMillis = startDateMillis,
                        endDateMillis = endDateMillis,
                    )
                } catch (iae: IllegalArgumentException) {
                    // By default, ignore exceptions that setSelection throws.
                    // Custom implementation may act differently.
                }
            },
            onDisplayedMonthChange = { monthInMillis ->
                state.displayedMonthMillis = monthInMillis
            },
            calendarModel = calendarModel,
            yearRange = state.yearRange,
            dateFormatter = dateFormatter,
            selectableDates = state.selectableDates,
            colors = colors,
            focusRequester = focusRequester,
        )
    }
}

/**
 * A state object that can be hoisted to observe the date range picker state. See
 * [rememberDateRangePickerState].
 */
@Stable
interface DateRangePickerState {

    /**
     * A timestamp that represents the selected start date _start_ of the day in _UTC_ milliseconds
     * from the epoch.
     *
     * @see [setSelection] for setting this value along with the [selectedEndDateMillis].
     */
    @get:Suppress("AutoBoxing") val selectedStartDateMillis: Long?

    /**
     * A timestamp that represents the selected end date _start_ of the day in _UTC_ milliseconds
     * from the epoch.
     *
     * @see [setSelection] for setting this value along with the [selectedStartDateMillis].
     */
    @get:Suppress("AutoBoxing") val selectedEndDateMillis: Long?

    /**
     * A timestamp that represents the currently displayed month _start_ date in _UTC_ milliseconds
     * from the epoch.
     *
     * @throws IllegalArgumentException in case the value is set with a timestamp that does not fall
     *   within the [yearRange].
     */
    var displayedMonthMillis: Long

    /** A [DisplayMode] that represents the current UI mode (i.e. picker or input). */
    var displayMode: DisplayMode

    /** An [IntRange] that holds the year range that the date picker will be limited to. */
    val yearRange: IntRange

    /**
     * A [SelectableDates] that is consulted to check if a date is allowed.
     *
     * In case a date is not allowed to be selected, it will appear disabled in the UI.
     */
    val selectableDates: SelectableDates

    /**
     * A locale that will be used when formatting dates, determining the input format, week-days,
     * and more.
     */
    val locale: CalendarLocale

    /**
     * Sets a start and end selection dates.
     *
     * The function expects the dates to be within the state's year-range, and for the start date to
     * appear before, or be equal, the end date. Also, if an end date is provided (e.g. not `null`),
     * a start date is also expected to be provided. In any other case, an
     * [IllegalArgumentException] is thrown.
     *
     * @param startDateMillis timestamp in _UTC_ milliseconds from the epoch that represents the
     *   start date selection. Provide a `null` to indicate no selection.
     * @param endDateMillis timestamp in _UTC_ milliseconds from the epoch that represents the end
     *   date selection. Provide a `null` to indicate no selection.
     * @throws IllegalArgumentException in case the given timestamps do not comply with the expected
     *   values specified above.
     */
    fun setSelection(
        @Suppress("AutoBoxing") startDateMillis: Long?,
        @Suppress("AutoBoxing") endDateMillis: Long?,
    )
}

/**
 * Creates a [DateRangePickerState] for a [DateRangePicker] that is remembered across compositions.
 *
 * To create a date range picker state outside composition, see the `DateRangePickerState` function.
 *
 * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
 *   represents an initial selection of a start date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
 *   represents an initial selection of an end date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. By default, in case an
 *   `initialSelectedStartDateMillis` is provided, the initial displayed month would be the month of
 *   the selected date. Otherwise, in case `null` is provided, the displayed month would be the
 *   current one.
 * @param yearRange an [IntRange] that holds the year range that the date range picker will be
 *   limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 */
@Composable
fun rememberDateRangePickerState(
    @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedStartDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DateRangePickerState {
    val locale = defaultLocale()
    return rememberSaveable(saver = DateRangePickerStateImpl.Saver(selectableDates, locale)) {
            DateRangePickerStateImpl(
                initialSelectedStartDateMillis = initialSelectedStartDateMillis,
                initialSelectedEndDateMillis = initialSelectedEndDateMillis,
                initialDisplayedMonthMillis = initialDisplayedMonthMillis,
                yearRange = yearRange,
                initialDisplayMode = initialDisplayMode,
                selectableDates = selectableDates,
                locale = locale,
            )
        }
        .apply {
            // Update the state's selectable dates if they were changed.
            this.selectableDates = selectableDates
        }
}

/**
 * Creates a [DateRangePickerState].
 *
 * For most cases, you are advised to use the [rememberDateRangePickerState] when in a composition.
 *
 * Note that in case you provide a [locale] that is different than the default platform locale, you
 * may need to ensure that the picker's title and headline are localized correctly. The following
 * sample shows one possible way of doing so by applying a local composition of a `LocalContext` and
 * `LocaleConfiguration`.
 *
 * @sample androidx.compose.material3.samples.DatePickerCustomLocaleSample
 * @param locale the [CalendarLocale] that will be used when formatting dates, determining the input
 *   format, displaying the week-day, determining the first day of the week, and more. Note that in
 *   case the provided [CalendarLocale] differs from the platform's default Locale, you may need to
 *   ensure that the picker's title and headline are localized correctly, and in some cases, you may
 *   need to apply an RTL layout.
 * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
 *   represents an initial selection of a start date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
 *   represents an initial selection of an end date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. By default, in case an
 *   `initialSelectedStartDateMillis` is provided, the initial displayed month would be the month of
 *   the selected date. Otherwise, in case `null` is provided, the displayed month would be the
 *   current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI
 * @throws IllegalArgumentException if the initial timestamps do not fall within the year range this
 *   state is created with, or the end date precedes the start date, or when an end date is provided
 *   without a start date (e.g. the start date was null, while the end date was not).
 * @see rememberDateRangePickerState
 */
fun DateRangePickerState(
    locale: CalendarLocale,
    @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedStartDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DateRangePickerState =
    DateRangePickerStateImpl(
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates,
        locale = locale,
    )

/** Contains default values used by the [DateRangePicker]. */
@Stable
object DateRangePickerDefaults {

    /**
     * A default date range picker title composable.
     *
     * @param displayMode the current [DisplayMode]
     * @param modifier a [Modifier] to be applied for the title
     * @param contentColor the content color of this title
     */
    @Composable
    fun DateRangePickerTitle(
        displayMode: DisplayMode,
        modifier: Modifier = Modifier,
        contentColor: Color = DatePickerDefaults.colors().titleContentColor,
    ) {
        when (displayMode) {
            DisplayMode.Picker ->
                Text(
                    getString(string = Strings.DateRangePickerTitle),
                    modifier = modifier,
                    color = contentColor,
                )
            DisplayMode.Input ->
                Text(
                    getString(string = Strings.DateRangeInputTitle),
                    modifier = modifier,
                    color = contentColor,
                )
        }
    }

    /**
     * A default date picker headline composable lambda that displays a default headline text when
     * there is no date selection, and an actual date string when there is.
     *
     * @param selectedStartDateMillis a timestamp that represents the selected start date _start_ of
     *   the day in _UTC_ milliseconds from the epoch
     * @param selectedEndDateMillis a timestamp that represents the selected end date _start_ of the
     *   day in _UTC_ milliseconds from the epoch
     * @param displayMode the current [DisplayMode]
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     * @param contentColor the content color of this headline
     */
    @Composable
    fun DateRangePickerHeadline(
        @Suppress("AutoBoxing") selectedStartDateMillis: Long?,
        @Suppress("AutoBoxing") selectedEndDateMillis: Long?,
        displayMode: DisplayMode,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier = Modifier,
        contentColor: Color = DatePickerDefaults.colors().headlineContentColor,
    ) {
        val startDateText = getString(Strings.DateRangePickerStartHeadline)
        val endDateText = getString(Strings.DateRangePickerEndHeadline)
        DateRangePickerHeadline(
            selectedStartDateMillis = selectedStartDateMillis,
            selectedEndDateMillis = selectedEndDateMillis,
            displayMode = displayMode,
            dateFormatter = dateFormatter,
            modifier = modifier,
            contentColor = contentColor,
            startDateText = startDateText,
            endDateText = endDateText,
            startDatePlaceholder = { Text(text = startDateText, color = contentColor) },
            endDatePlaceholder = { Text(text = endDateText, color = contentColor) },
            datesDelimiter = { Text(text = "-", color = contentColor) },
            locale = defaultLocale(),
        )
    }

    /**
     * A date picker headline composable lambda that displays a default headline text when there is
     * no date selection, and an actual date string when there is.
     *
     * @param selectedStartDateMillis a timestamp that represents the selected start date _start_ of
     *   the day in _UTC_ milliseconds from the epoch
     * @param selectedEndDateMillis a timestamp that represents the selected end date _start_ of the
     *   day in _UTC_ milliseconds from the epoch
     * @param displayMode the current [DisplayMode]
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     * @param startDateText a string that, by default, be used as the text content for the
     *   [startDatePlaceholder], as well as a prefix for the content description for the selected
     *   start date
     * @param endDateText a string that, by default, be used as the text content for the
     *   [endDatePlaceholder], as well as a prefix for the content description for the selected end
     *   date
     * @param startDatePlaceholder a composable to be displayed as a headline placeholder for the
     *   start date (i.e. a [Text] with a "Start date" string)
     * @param endDatePlaceholder a composable to be displayed as a headline placeholder for the end
     *   date (i.e a [Text] with an "End date" string)
     * @param datesDelimiter a composable to be displayed as a headline delimiter between the start
     *   and the end dates
     * @param locale a [CalendarLocale] to be used when formatting dates at the headline. The
     *   default value holds the default locale of the platform.
     */
    @Composable
    private fun DateRangePickerHeadline(
        selectedStartDateMillis: Long?,
        selectedEndDateMillis: Long?,
        displayMode: DisplayMode,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier,
        contentColor: Color,
        startDateText: String,
        endDateText: String,
        startDatePlaceholder: @Composable () -> Unit,
        endDatePlaceholder: @Composable () -> Unit,
        datesDelimiter: @Composable () -> Unit,
        locale: CalendarLocale,
    ) {
        val formatterStartDate =
            dateFormatter.formatDate(dateMillis = selectedStartDateMillis, locale = locale)

        val formatterEndDate =
            dateFormatter.formatDate(dateMillis = selectedEndDateMillis, locale = locale)

        val verboseStartDateDescription =
            dateFormatter.formatDate(
                dateMillis = selectedStartDateMillis,
                locale = locale,
                forContentDescription = true,
            )
                ?: when (displayMode) {
                    DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                    DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                    else -> ""
                }

        val verboseEndDateDescription =
            dateFormatter.formatDate(
                dateMillis = selectedEndDateMillis,
                locale = locale,
                forContentDescription = true,
            )
                ?: when (displayMode) {
                    DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                    DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                    else -> ""
                }

        val startHeadlineDescription = "$startDateText: $verboseStartDateDescription"
        val endHeadlineDescription = "$endDateText: $verboseEndDateDescription"

        Row(
            modifier =
                modifier.clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "$startHeadlineDescription, $endHeadlineDescription"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (formatterStartDate != null) {
                Text(text = formatterStartDate, color = contentColor)
            } else {
                startDatePlaceholder()
            }
            datesDelimiter()
            if (formatterEndDate != null) {
                Text(text = formatterEndDate, color = contentColor)
            } else {
                endDatePlaceholder()
            }
        }
    }
}

/**
 * A default implementation of the [DateRangePickerState]. See [rememberDateRangePickerState].
 *
 * The state's [selectedStartDateMillis] and [selectedEndDateMillis] will provide timestamps for the
 * _beginning_ of the selected days (i.e. midnight in _UTC_ milliseconds from the epoch).
 *
 * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
 *   represents an initial selection of a start date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
 *   represents an initial selection of an end date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. By default, in case an
 *   `initialSelectedStartDateMillis` is provided, the initial displayed month would be the month of
 *   the selected date. Otherwise, in case `null` is provided, the displayed month would be the
 *   current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI
 * @param locale a [CalendarLocale] to be used when formatting dates, determining the input format,
 *   and more
 * @throws IllegalArgumentException if the initial timestamps do not fall within the year range this
 *   state is created with, or the end date precedes the start date, or when an end date is provided
 *   without a start date (e.g. the start date was null, while the end date was not).
 * @see rememberDateRangePickerState
 */
@Stable
private class DateRangePickerStateImpl(
    @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long?,
    @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long?,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
    yearRange: IntRange,
    initialDisplayMode: DisplayMode,
    selectableDates: SelectableDates,
    locale: CalendarLocale,
) :
    BaseDatePickerStateImpl(initialDisplayedMonthMillis, yearRange, selectableDates, locale),
    DateRangePickerState {

    /** A mutable state of [CalendarDate] that represents a selected start date. */
    private var _selectedStartDate = mutableStateOf<CalendarDate?>(null)

    /** A mutable state of [CalendarDate] that represents a selected end date. */
    private var _selectedEndDate = mutableStateOf<CalendarDate?>(null)

    /** Initialize the state with the provided initial selections. */
    init {
        setSelection(
            startDateMillis = initialSelectedStartDateMillis,
            endDateMillis = initialSelectedEndDateMillis,
        )
    }

    /**
     * A timestamp that represents the _start_ of the day of the selected start date in _UTC_
     * milliseconds from the epoch.
     *
     * In case no date was selected or provided, the state will hold a `null` value.
     *
     * @throws IllegalArgumentException in case a set timestamp does not fall within the year range
     *   this state was created with.
     */
    override val selectedStartDateMillis: Long?
        @Suppress("AutoBoxing") get() = _selectedStartDate.value?.utcTimeMillis

    /**
     * A timestamp that represents the _start_ of the day of the selected end date in _UTC_
     * milliseconds from the epoch.
     *
     * In case no date was selected or provided, the state will hold a `null` value.
     *
     * @throws IllegalArgumentException in case a set timestamp does not fall within the year range
     *   this state was created with.
     */
    override val selectedEndDateMillis: Long?
        @Suppress("AutoBoxing") get() = _selectedEndDate.value?.utcTimeMillis

    /**
     * A mutable state of [DisplayMode] that represents the current display mode of the UI (i.e.
     * picker or input).
     */
    private var _displayMode = mutableStateOf(initialDisplayMode)

    override var displayMode
        get() = _displayMode.value
        set(displayMode) {
            selectedStartDateMillis?.let {
                displayedMonthMillis = calendarModel.getMonth(it).startUtcTimeMillis
            }
            _displayMode.value = displayMode
        }

    override fun setSelection(
        @Suppress("AutoBoxing") startDateMillis: Long?,
        @Suppress("AutoBoxing") endDateMillis: Long?,
    ) {
        val startDate = getDate(startDateMillis)
        val endDate = getDate(endDateMillis)

        // Validate that an end date cannot be set without a start date and that the end date
        // appears on or after the start date.
        if (
            startDate != null &&
                (endDate == null || startDate.utcTimeMillis <= endDate.utcTimeMillis)
        ) {
            _selectedStartDate.value = startDate
            _selectedEndDate.value = endDate
        } else {
            _selectedStartDate.value = null
            _selectedEndDate.value = null
        }
    }

    private fun getDate(dateMillis: Long?) =
        if (dateMillis != null) {
            val date = calendarModel.getCanonicalDate(dateMillis)
            // Validate that the date is within the valid years range.
            if (yearRange.contains(date.year)) {
                date
            } else {
                null
            }
        } else {
            null
        }

    companion object {
        /**
         * The default [Saver] implementation for [DateRangePickerStateImpl].
         *
         * @param selectableDates a [SelectableDates] instance that is consulted to check if a date
         *   is allowed
         */
        fun Saver(
            selectableDates: SelectableDates,
            locale: CalendarLocale,
        ): Saver<DateRangePickerStateImpl, Any> =
            listSaver(
                save = {
                    listOf(
                        it.selectedStartDateMillis,
                        it.selectedEndDateMillis,
                        it.displayedMonthMillis,
                        it.yearRange.first,
                        it.yearRange.last,
                        it.displayMode.value,
                    )
                },
                restore = { value ->
                    DateRangePickerStateImpl(
                        initialSelectedStartDateMillis = value[0] as Long?,
                        initialSelectedEndDateMillis = value[1] as Long?,
                        initialDisplayedMonthMillis = value[2] as Long?,
                        yearRange = IntRange(value[3] as Int, value[4] as Int),
                        initialDisplayMode = DisplayMode(value[5] as Int),
                        selectableDates = selectableDates,
                        locale = locale,
                    )
                },
            )
    }
}

/**
 * Date entry content that displays a [DateRangePickerContent] or a [DateRangeInputContent]
 * according to the state's display mode.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwitchableDateEntryContent(
    selectedStartDateMillis: Long?,
    selectedEndDateMillis: Long?,
    displayedMonthMillis: Long,
    displayMode: DisplayMode,
    onDatesSelectionChange: (startDateMillis: Long?, endDateMillis: Long?) -> Unit,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    focusRequester: FocusRequester?,
) {
    // TODO(b/266480386): Apply the motion spec for this once we have it. Consider replacing this
    //  with AnimatedContent when it's out of experimental.
    // TODO Load the motionScheme tokens from the component tokens file
    Crossfade(
        targetState = displayMode,
        animationSpec = MotionSchemeKeyTokens.FastEffects.value(),
        modifier =
            Modifier.semantics {
                @Suppress("DEPRECATION")
                isContainer = true
            },
    ) { mode ->
        when (mode) {
            DisplayMode.Picker ->
                DateRangePickerContent(
                    selectedStartDateMillis = selectedStartDateMillis,
                    selectedEndDateMillis = selectedEndDateMillis,
                    displayedMonthMillis = displayedMonthMillis,
                    onDatesSelectionChange = onDatesSelectionChange,
                    onDisplayedMonthChange = onDisplayedMonthChange,
                    calendarModel = calendarModel,
                    yearRange = yearRange,
                    dateFormatter = dateFormatter,
                    selectableDates = selectableDates,
                    colors = colors,
                )
            DisplayMode.Input ->
                DateRangeInputContent(
                    selectedStartDateMillis = selectedStartDateMillis,
                    selectedEndDateMillis = selectedEndDateMillis,
                    onDatesSelectionChange = onDatesSelectionChange,
                    calendarModel = calendarModel,
                    yearRange = yearRange,
                    dateFormatter = dateFormatter,
                    selectableDates = selectableDates,
                    colors = colors,
                    focusRequester = focusRequester,
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerContent(
    selectedStartDateMillis: Long?,
    selectedEndDateMillis: Long?,
    displayedMonthMillis: Long,
    onDatesSelectionChange: (startDateMillis: Long?, endDateMillis: Long?) -> Unit,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
) {
    val displayedMonth = calendarModel.getMonth(displayedMonthMillis)
    val monthIndex = displayedMonth.indexIn(yearRange).coerceAtLeast(0)
    val monthsListState = rememberLazyListState(initialFirstVisibleItemIndex = monthIndex)

    // Scroll to the resolved displayedMonth, if needed.
    LaunchedEffect(monthIndex) {
        // Unlike the DatePicker, we don't have to check here for isScrollInProgress and scroll
        // to the monthIndex even when there is a current scroll operation.
        if (monthsListState.firstVisibleItemIndex != monthIndex) {
            monthsListState.scrollToItem(monthIndex)
        }
    }

    Column(modifier = Modifier.padding(horizontal = DatePickerHorizontalPadding)) {
        WeekDays(colors, calendarModel)
        VerticalMonthsList(
            lazyListState = monthsListState,
            selectedStartDateMillis = selectedStartDateMillis,
            selectedEndDateMillis = selectedEndDateMillis,
            onDatesSelectionChange = onDatesSelectionChange,
            onDisplayedMonthChange = onDisplayedMonthChange,
            calendarModel = calendarModel,
            yearRange = yearRange,
            dateFormatter = dateFormatter,
            selectableDates = selectableDates,
            colors = colors,
        )
    }
}

/**
 * Composes a continuous vertical scrollable list of calendar months. Each month will appear with a
 * header text indicating the month and the year.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalMonthsList(
    lazyListState: LazyListState,
    selectedStartDateMillis: Long?,
    selectedEndDateMillis: Long?,
    onDatesSelectionChange: (startDateMillis: Long?, endDateMillis: Long?) -> Unit,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
) {
    val today = calendarModel.today
    val firstMonth =
        remember(yearRange) {
            calendarModel.getMonth(
                year = yearRange.first,
                month = 1, // January
            )
        }
    ProvideTextStyle(DatePickerModalTokens.DateLabelTextFont.value) {
        val coroutineScope = rememberCoroutineScope()
        val scrollToPreviousMonthLabel = getString(Strings.DateRangePickerScrollToShowPreviousMonth)
        val scrollToNextMonthLabel = getString(Strings.DateRangePickerScrollToShowNextMonth)

        // The updateDateSelection will invoke the onDatesSelectionChange with the proper
        // selection according to the current state.
        val onDateSelectionChange = { dateInMillis: Long ->
            updateDateSelection(
                dateInMillis = dateInMillis,
                currentStartDateMillis = selectedStartDateMillis,
                currentEndDateMillis = selectedEndDateMillis,
                onDatesSelectionChange = onDatesSelectionChange,
            )
        }

        val customAccessibilityAction =
            customScrollActions(
                state = lazyListState,
                coroutineScope = coroutineScope,
                scrollUpLabel = scrollToPreviousMonthLabel,
                scrollDownLabel = scrollToNextMonthLabel,
            )

        LazyColumn(
            // Apply this to have the screen reader traverse outside the visible list of months
            // and not scroll them by default.
            modifier =
                Modifier.semantics {
                    verticalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
                },
            state = lazyListState,
        ) {
            items(numberOfMonthsInRange(yearRange)) {
                val month = calendarModel.plusMonths(from = firstMonth, addedMonthsCount = it)
                Column(modifier = Modifier.fillParentMaxWidth()) {
                    ProvideTextStyle(DatePickerModalTokens.RangeSelectionMonthSubheadFont.value) {
                        Text(
                            text =
                                dateFormatter.formatMonthYear(
                                    monthMillis = month.startUtcTimeMillis,
                                    locale = calendarModel.locale,
                                ) ?: "-",
                            modifier =
                                Modifier.padding(paddingValues = CalendarMonthSubheadPadding)
                                    .semantics { customActions = customAccessibilityAction },
                            color = colors.subheadContentColor,
                        )
                    }
                    val rangeSelectionInfo: SelectedRangeInfo? =
                        if (selectedStartDateMillis != null && selectedEndDateMillis != null) {
                            remember(selectedStartDateMillis, selectedEndDateMillis) {
                                SelectedRangeInfo.calculateRangeInfo(
                                    month = month,
                                    startDate =
                                        calendarModel.getCanonicalDate(selectedStartDateMillis),
                                    endDate = calendarModel.getCanonicalDate(selectedEndDateMillis),
                                )
                            }
                        } else {
                            null
                        }
                    Month(
                        month = month,
                        onDateSelectionChange = onDateSelectionChange,
                        todayMillis = today.utcTimeMillis,
                        startDateMillis = selectedStartDateMillis,
                        endDateMillis = selectedEndDateMillis,
                        rangeSelectionInfo = rangeSelectionInfo,
                        dateFormatter = dateFormatter,
                        selectableDates = selectableDates,
                        colors = colors,
                        locale = calendarModel.locale,
                    )
                }
            }
        }
    }
    LaunchedEffect(lazyListState) {
        updateDisplayedMonth(
            lazyListState = lazyListState,
            onDisplayedMonthChange = onDisplayedMonthChange,
            calendarModel = calendarModel,
            yearRange = yearRange,
        )
    }
}

private fun updateDateSelection(
    dateInMillis: Long,
    currentStartDateMillis: Long?,
    currentEndDateMillis: Long?,
    onDatesSelectionChange: (startDateMillis: Long?, endDateMillis: Long?) -> Unit,
) {
    if (
        (currentStartDateMillis == null && currentEndDateMillis == null) ||
            (currentStartDateMillis != null && currentEndDateMillis != null)
    ) {
        // Set the selection to "start" only.
        onDatesSelectionChange(dateInMillis, null)
    } else if (currentStartDateMillis != null && dateInMillis >= currentStartDateMillis) {
        // Set the end date.
        onDatesSelectionChange(currentStartDateMillis, dateInMillis)
    } else {
        // The user selected an earlier date than the start date, so reset the start.
        onDatesSelectionChange(dateInMillis, null)
    }
}

internal val CalendarMonthSubheadPadding = PaddingValues(start = 24.dp, top = 20.dp, bottom = 8.dp)

/**
 * a helper class for drawing a range selection. The class holds information about the selected
 * start and end dates as coordinates within the 7 x 6 calendar month grid, as well as information
 * regarding the first and last selected items.
 *
 * A SelectedRangeInfo is created when a [Month] is composed with an `rangeSelectionEnabled` flag.
 */
internal class SelectedRangeInfo(
    val gridStartCoordinates: IntOffset,
    val gridEndCoordinates: IntOffset,
    val firstIsSelectionStart: Boolean,
    val lastIsSelectionEnd: Boolean,
) {
    companion object {
        /**
         * Calculates the selection coordinates within the current month's grid. The returned [Pair]
         * holds the actual item x & y coordinates within the LazyVerticalGrid, and is later used to
         * calculate the exact offset for drawing the selection rectangles when in range-selection
         * mode.
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun calculateRangeInfo(
            month: CalendarMonth,
            startDate: CalendarDate,
            endDate: CalendarDate,
        ): SelectedRangeInfo? {
            if (
                startDate.utcTimeMillis > month.endUtcTimeMillis ||
                    endDate.utcTimeMillis < month.startUtcTimeMillis
            ) {
                return null
            }
            val firstIsSelectionStart = startDate.utcTimeMillis >= month.startUtcTimeMillis
            val lastIsSelectionEnd = endDate.utcTimeMillis <= month.endUtcTimeMillis
            val startGridItemOffset =
                if (firstIsSelectionStart) {
                    month.daysFromStartOfWeekToFirstOfMonth + startDate.dayOfMonth - 1
                } else {
                    month.daysFromStartOfWeekToFirstOfMonth
                }
            val endGridItemOffset =
                if (lastIsSelectionEnd) {
                    month.daysFromStartOfWeekToFirstOfMonth + endDate.dayOfMonth - 1
                } else {
                    month.daysFromStartOfWeekToFirstOfMonth + month.numberOfDays - 1
                }

            // Calculate the selected coordinates within the cells grid.
            val gridStartCoordinates =
                IntOffset(
                    x = startGridItemOffset % DaysInWeek,
                    y = startGridItemOffset / DaysInWeek,
                )
            val gridEndCoordinates =
                IntOffset(x = endGridItemOffset % DaysInWeek, y = endGridItemOffset / DaysInWeek)
            return SelectedRangeInfo(
                gridStartCoordinates,
                gridEndCoordinates,
                firstIsSelectionStart,
                lastIsSelectionEnd,
            )
        }
    }
}

/**
 * Draws the range selection background.
 *
 * This function is called during a [Modifier.drawWithContent] call when a [Month] is composed with
 * an `rangeSelectionEnabled` flag.
 */
internal fun ContentDrawScope.drawRangeBackground(
    selectedRangeInfo: SelectedRangeInfo,
    color: Color,
) {
    // The LazyVerticalGrid is defined to space the items horizontally by
    // DaysHorizontalPadding (e.g. 4.dp). However, as the grid is not limited in
    // width, the spacing can go beyond that value, so this drawing takes this into
    // account.
    // TODO: Use the date's container width and height from the tokens once b/247694457 is resolved.
    val itemContainerWidth = RecommendedSizeForAccessibility.toPx()
    val itemContainerHeight = RecommendedSizeForAccessibility.toPx()
    val itemStateLayerHeight = DatePickerModalTokens.DateStateLayerHeight.toPx()
    val stateLayerVerticalPadding = (itemContainerHeight - itemStateLayerHeight) / 2
    val horizontalSpaceBetweenItems =
        (this.size.width - DaysInWeek * itemContainerWidth) / DaysInWeek

    val (x1, y1) = selectedRangeInfo.gridStartCoordinates
    val (x2, y2) = selectedRangeInfo.gridEndCoordinates
    // The endX and startX are offset to include only half the item's width when dealing with first
    // and last items in the selection in order to keep the selection edges rounded.
    var startX =
        x1 * (itemContainerWidth + horizontalSpaceBetweenItems) +
            (if (selectedRangeInfo.firstIsSelectionStart) itemContainerWidth / 2 else 0f) +
            horizontalSpaceBetweenItems / 2
    val startY = y1 * itemContainerHeight + stateLayerVerticalPadding
    var endX =
        x2 * (itemContainerWidth + horizontalSpaceBetweenItems) +
            (if (selectedRangeInfo.lastIsSelectionEnd) itemContainerWidth / 2
            else itemContainerWidth) +
            horizontalSpaceBetweenItems / 2
    val endY = y2 * itemContainerHeight + stateLayerVerticalPadding

    val isRtl = layoutDirection == LayoutDirection.Rtl
    // Adjust the start and end in case the layout is RTL.
    if (isRtl) {
        startX = this.size.width - startX
        endX = this.size.width - endX
    }

    // Draw the first row background
    drawRect(
        color = color,
        topLeft = Offset(startX, startY),
        size =
            Size(
                width =
                    when {
                        y1 == y2 -> endX - startX
                        isRtl -> -startX
                        else -> this.size.width - startX
                    },
                height = itemStateLayerHeight,
            ),
    )

    if (y1 != y2) {
        for (y in y2 - y1 - 1 downTo 1) {
            // Draw background behind the rows in between.
            drawRect(
                color = color,
                topLeft = Offset(0f, startY + (y * itemContainerHeight)),
                size = Size(width = this.size.width, height = itemStateLayerHeight),
            )
        }
        // Draw the last row selection background
        val topLeftX = if (layoutDirection == LayoutDirection.Ltr) 0f else this.size.width
        drawRect(
            color = color,
            topLeft = Offset(topLeftX, endY),
            size =
                Size(
                    width = if (isRtl) endX - this.size.width else endX,
                    height = itemStateLayerHeight,
                ),
        )
    }
}

private fun customScrollActions(
    state: LazyListState,
    coroutineScope: CoroutineScope,
    scrollUpLabel: String,
    scrollDownLabel: String,
): List<CustomAccessibilityAction> {
    val scrollUpAction = {
        if (!state.canScrollBackward) {
            false
        } else {
            coroutineScope.launch { state.scrollToItem(state.firstVisibleItemIndex - 1) }
            true
        }
    }
    val scrollDownAction = {
        if (!state.canScrollForward) {
            false
        } else {
            coroutineScope.launch { state.scrollToItem(state.firstVisibleItemIndex + 1) }
            true
        }
    }
    return listOf(
        CustomAccessibilityAction(label = scrollUpLabel, action = scrollUpAction),
        CustomAccessibilityAction(label = scrollDownLabel, action = scrollDownAction),
    )
}

private val DateRangePickerTitlePadding = PaddingValues(start = 64.dp, end = 12.dp)
private val DateRangePickerHeadlinePadding =
    PaddingValues(start = 64.dp, end = 12.dp, bottom = 12.dp)

// An offset that is applied to the token value for the RangeSelectionHeaderContainerHeight. The
// implementation does not render a "Save" and "X" buttons by default, so we don't take those into
// account when setting the header's max height.
private val HeaderHeightOffset = 60.dp
