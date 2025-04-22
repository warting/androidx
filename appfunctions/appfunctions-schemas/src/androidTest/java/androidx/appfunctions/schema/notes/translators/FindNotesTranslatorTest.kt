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

package androidx.appfunctions.schema.notes.translators

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.appfunctions.LegacyAttachment
import androidx.appfunctions.LegacyDate
import androidx.appfunctions.LegacyDateTime
import androidx.appfunctions.LegacyFindNotesParams
import androidx.appfunctions.LegacyNote
import androidx.appfunctions.LegacyTimeOfDay
import androidx.appfunctions.LegacyUri
import androidx.appfunctions.schema.TranslatorTestUtils
import androidx.test.filters.SdkSuppress
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = 33)
class FindNotesTranslatorTest {

    private val translatorTestUtils = TranslatorTestUtils(FindNotesTranslator())

    @Before
    fun setup() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 13)
    }

    @Test
    fun upgradeRequest_allFields() {
        val legacyParams =
            LegacyFindNotesParams(
                query = "query",
                startDate =
                    LegacyDateTime(
                        date = LegacyDate(year = 2024, month = 1, day = 1),
                        timeOfDay =
                            LegacyTimeOfDay(hours = 12, minutes = 7, seconds = 8, nanos = 9),
                        timeZone = ZoneId.systemDefault().toString()
                    ),
                endDate =
                    LegacyDateTime(
                        date = LegacyDate(year = 2024, month = 1, day = 2),
                        timeOfDay =
                            LegacyTimeOfDay(hours = 12, minutes = 7, seconds = 8, nanos = 9),
                        timeZone = ZoneId.systemDefault().toString()
                    ),
                maxCount = Int.MAX_VALUE,
            )

        val expectedUpgradedParams =
            FindNotesAppFunctionParams(
                query = "query",
                modifiedAfter = legacyParams.startDate?.toZonedDateTime()?.toInstant(),
                modifiedBefore = legacyParams.endDate?.toZonedDateTime()?.toInstant(),
            )
        translatorTestUtils.assertUpgradeRequestTranslation(
            legacyParameterName = "findNotesParams",
            legacyInput = legacyParams,
            expectedJetpackOutput = expectedUpgradedParams
        )
    }

    @Test
    fun upgradeRequest_optionalFieldsNotSet() {
        val legacyParams = LegacyFindNotesParams(maxCount = Int.MAX_VALUE)

        val expectedUpgradedParams = FindNotesAppFunctionParams()
        translatorTestUtils.assertUpgradeRequestTranslation(
            legacyParameterName = "findNotesParams",
            legacyInput = legacyParams,
            expectedJetpackOutput = expectedUpgradedParams
        )
    }

    @Test
    fun downgradeRequest_allFields() {
        val jetpackParams =
            FindNotesAppFunctionParams(
                query = "query",
                modifiedAfter =
                    ZonedDateTime.of(
                            LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 1),
                            LocalTime.of(
                                /* hour= */ 12,
                                /* minute= */ 7,
                                /* second= */ 8,
                                /* nanoOfSecond= */ 9
                            ),
                            ZoneId.systemDefault()
                        )
                        .toInstant(),
                modifiedBefore =
                    ZonedDateTime.of(
                            LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 2),
                            LocalTime.of(
                                /* hour= */ 12,
                                /* minute= */ 7,
                                /* second= */ 8,
                                /* nanoOfSecond= */ 9
                            ),
                            ZoneId.systemDefault()
                        )
                        .toInstant(),
            )

        val expectedDowngradedParams =
            LegacyFindNotesParams(
                query = "query",
                startDate =
                    LegacyDateTime(
                        date = LegacyDate(year = 2024, month = 1, day = 1),
                        timeOfDay =
                            LegacyTimeOfDay(hours = 12, minutes = 7, seconds = 8, nanos = 9),
                        timeZone = ZoneId.systemDefault().toString()
                    ),
                endDate =
                    LegacyDateTime(
                        date = LegacyDate(year = 2024, month = 1, day = 2),
                        timeOfDay =
                            LegacyTimeOfDay(hours = 12, minutes = 7, seconds = 8, nanos = 9),
                        timeZone = ZoneId.systemDefault().toString()
                    ),
                maxCount = Int.MAX_VALUE,
            )
        translatorTestUtils.assertDowngradeRequestTranslation(
            "findNotesParams",
            jetpackParams,
            expectedDowngradedParams
        )
    }

    @Test
    fun downgradeRequest_nullableFieldsNotSet() {
        val jetpackParams = FindNotesAppFunctionParams()

        val expectedDowngradedParams = LegacyFindNotesParams(maxCount = Int.MAX_VALUE)
        translatorTestUtils.assertDowngradeRequestTranslation(
            "findNotesParams",
            jetpackParams,
            expectedDowngradedParams
        )
    }

    @Test
    fun upgradeResponse_allFields() {
        val legacyNotes =
            listOf(
                LegacyNote(
                    id = "id",
                    title = "title",
                    content = "content",
                    attachments =
                        listOf(
                            LegacyAttachment(
                                displayName = "name",
                                mimeType = "text/html",
                                uri = LegacyUri(uri = "content://xxx")
                            )
                        ),
                    folderId = "folderId"
                )
            )
        val expectedUpgradedResponse =
            FindNotesAppFunctionResponse(
                notes =
                    listOf(
                        AppFunctionNoteImpl(
                            id = "id",
                            title = "title",
                            content = "content",
                            attachments =
                                listOf(
                                    AttachmentImpl(
                                        displayName = "name",
                                        mimeType = "text/html",
                                        uri = Uri.parse("content://xxx")
                                    )
                                ),
                            groupId = "folderId"
                        )
                    )
            )
        translatorTestUtils.assertUpgradeResponseTranslation(
            legacyInput = legacyNotes,
            expectedJetpackOutput = expectedUpgradedResponse
        )
    }

    @Test
    fun upgradeResponse_optionalFieldsNotSet() {
        val legacyNotes =
            listOf(
                LegacyNote(
                    id = "id",
                    title = "title",
                )
            )
        val expectedUpgradedResponse =
            FindNotesAppFunctionResponse(
                notes =
                    listOf(
                        AppFunctionNoteImpl(
                            id = "id",
                            title = "title",
                            content = null,
                            attachments = emptyList()
                        )
                    )
            )
        translatorTestUtils.assertUpgradeResponseTranslation(
            legacyInput = legacyNotes,
            expectedJetpackOutput = expectedUpgradedResponse
        )
    }

    @Test
    fun downgradeResponse_allFields() {
        val jetpackResponse =
            FindNotesAppFunctionResponse(
                notes =
                    listOf(
                        AppFunctionNoteImpl(
                            id = "id",
                            title = "title",
                            content = "content",
                            attachments =
                                listOf(
                                    AttachmentImpl(
                                        displayName = "name",
                                        mimeType = "text/html",
                                        uri = Uri.parse("content://xxx")
                                    )
                                ),
                            groupId = "groupId",
                        )
                    )
            )

        val expectedDowngradedResponse =
            listOf(
                LegacyNote(
                    id = "id",
                    title = "title",
                    content = "content",
                    attachments =
                        listOf(
                            LegacyAttachment(
                                displayName = "name",
                                mimeType = "text/html",
                                uri = LegacyUri(uri = "content://xxx")
                            )
                        ),
                    folderId = "groupId",
                )
            )

        translatorTestUtils.assertDowngradeResponseTranslation(
            jetpackResponse,
            expectedDowngradedResponse
        )
    }

    @Test
    fun downgradeResponse_optionalFieldsNotSet() {
        val jetpackResponse =
            FindNotesAppFunctionResponse(
                notes =
                    listOf(
                        AppFunctionNoteImpl(
                            id = "id",
                            title = "title",
                        )
                    )
            )

        val expectedDowngradedResponse = listOf(LegacyNote(id = "id", title = "title"))
        translatorTestUtils.assertDowngradeResponseTranslation(
            jetpackResponse,
            expectedDowngradedResponse
        )
    }
}
