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

package androidx.car.app.messaging.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.model.CarText;
import androidx.core.app.Person;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarMessage}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarMessageTest {
    /** Ensure the builder does not fail for the minimum set of required fields. */
    @Test
    public void build_withRequiredFieldsOnly() {
        TestConversationFactory.createMinimalMessage();

        // assert no crash
    }

    /** Ensure the builder does not fail when all fields are assigned. */
    @Test
    public void build_withAllFields() {
        TestConversationFactory.createFullyPopulatedMessage();

        // assert no crash
    }

    // Ignore nullability, so we can null out a builder field
    @SuppressWarnings("ConstantConditions")
    @Test
    public void build_throwsException_ifMessageBodyMissing() {
        assertThrows(
                NullPointerException.class,
                () -> TestConversationFactory.createMinimalMessageBuilder()
                        .setBody(null)
                        .build()
        );
    }

    // region .equals() & .hashCode()
    @Test
    public void equalsAndHashCode_areEqual_forMinimalMessage() {
        CarMessage message1 =
                TestConversationFactory.createMinimalMessage();
        CarMessage message2 =
                TestConversationFactory.createMinimalMessage();

        assertEqual(message1, message2);
    }

    @Test
    public void equalsAndHashCode_areEqual_forFullyPopulatedMessage() {
        CarMessage message1 =
                TestConversationFactory.createFullyPopulatedMessage();
        CarMessage message2 =
                TestConversationFactory.createFullyPopulatedMessage();

        assertEqual(message1, message2);
    }

    @Test
    public void equalsAndHashCode_produceCorrectResult_ifIndividualFieldDiffers() {
        // Create base item, for comparison
        CarMessage fullyPopulatedMessage =
                TestConversationFactory.createFullyPopulatedMessage();

        // Create various non-equal items
        CarMessage modifiedSender =
                TestConversationFactory
                        .createFullyPopulatedMessageBuilder()
                        .setSender(
                                TestConversationFactory
                                        .createMinimalPersonBuilder()
                                        .setKey("Modified Key")
                                        .build()
                        )
                        .build();
        CarMessage modifiedBody =
                TestConversationFactory
                        .createFullyPopulatedMessageBuilder()
                        .setBody(CarText.create("Modified Message Body"))
                        .build();
        CarMessage modifiedReceivedTimeEpochMillis =
                TestConversationFactory
                        .createFullyPopulatedMessageBuilder()
                        .setReceivedTimeEpochMillis(
                                // Guaranteed to be different :)
                                fullyPopulatedMessage.getReceivedTimeEpochMillis() + 1
                        )
                        .build();
        CarMessage modifiedIsRead =
                TestConversationFactory
                        .createFullyPopulatedMessageBuilder()
                        .setRead(!fullyPopulatedMessage.isRead())
                        .build();

        // Verify (lack of) equality
        assertNotEqual(fullyPopulatedMessage, modifiedSender);
        assertNotEqual(fullyPopulatedMessage, modifiedBody);
        assertNotEqual(fullyPopulatedMessage, modifiedReceivedTimeEpochMillis);
        assertNotEqual(fullyPopulatedMessage, modifiedIsRead);
    }

    @Test
    public void equalsAndHashCode_produceCorrectResult_ifSenderIdMissing() {
        Person.Builder senderWithoutId = TestConversationFactory
                .createMinimalPersonBuilder()
                .setKey(null);
        Person.Builder senderWithId = TestConversationFactory
                .createMinimalPersonBuilder()
                .setKey("Test Sender Key");

        // Create Test Data
        CarMessage senderKeyMissingWithStandardData =
                TestConversationFactory.createMinimalMessageBuilder()
                        .setSender(senderWithoutId.build())
                        .build();
        CarMessage senderKeyMissingWithStandardData2 =
                TestConversationFactory.createMinimalMessageBuilder()
                        .setSender(senderWithoutId.build())
                        .build();
        CarMessage senderKeyMissingWithModifiedData =
                TestConversationFactory.createMinimalMessageBuilder()
                        .setSender(senderWithoutId.build())
                        .setBody(CarText.create("Modified Message Body"))
                        .build();
        CarMessage senderKeyProvidedWithStandardData =
                TestConversationFactory.createMinimalMessageBuilder()
                        .setSender(senderWithId.build())
                        .build();
        CarMessage senderKeyProvidedWithStandardData2 =
                TestConversationFactory.createMinimalMessageBuilder()
                        .setSender(senderWithId.build())
                        .build();
        CarMessage senderKeyProvidedWithModifiedData =
                TestConversationFactory.createMinimalMessageBuilder()
                        .setSender(senderWithId.build())
                        .setBody(CarText.create("Modified Message Body"))
                        .build();

        // Verify (in)equality

        // Sender & message content are equal: == EQUAL ==
        assertEqual(senderKeyMissingWithStandardData, senderKeyMissingWithStandardData2);
        assertEqual(senderKeyProvidedWithStandardData, senderKeyProvidedWithStandardData2);

        // One sender is missing a key: == NOT EQUAL ==
        assertNotEqual(senderKeyMissingWithStandardData, senderKeyProvidedWithStandardData);

        // Sender is equal, but message content is not: == NOT EQUAL ==
        assertNotEqual(senderKeyMissingWithStandardData, senderKeyMissingWithModifiedData);
        assertNotEqual(senderKeyProvidedWithStandardData, senderKeyProvidedWithModifiedData);
    }

    private void assertEqual(CarMessage message1, CarMessage message2) {
        assertThat(message1).isEqualTo(message2);
        assertThat(message1.hashCode()).isEqualTo(message2.hashCode());
    }

    private void assertNotEqual(CarMessage message1, CarMessage message2) {
        assertThat(message1).isNotEqualTo(message2);
        assertThat(message1.hashCode()).isNotEqualTo(message2.hashCode());
    }
    // endregion
}
