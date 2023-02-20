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

package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import static java.lang.Integer.MAX_VALUE;

import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.AnimatableFixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.DynamicAnimatedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.GetDurationPartOpNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.StateInt32SourceNode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DurationPartType;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetDurationPartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class Int32NodesTest {
    @Test
    public void testFixedInt32Node() {
        List<Integer> results = new ArrayList<>();

        FixedInt32 protoNode = FixedInt32.newBuilder().setValue(56).build();
        FixedInt32Node node = new FixedInt32Node(protoNode, new AddToListCallback<>(results));

        node.init();

        assertThat(results).containsExactly(56);
    }

    @Test
    public void testGetDurationPartOpNode_positiveDuration() {

        // Equivalent to 1day and 10h:17m:36s
        Duration duration = Duration.ofSeconds(123456);

        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_DAYS))
                .isEqualTo(1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_HOURS))
                .isEqualTo(10);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_MINUTES))
                .isEqualTo(17);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_SECONDS))
                .isEqualTo(36);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_DAYS))
                .isEqualTo(1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_HOURS))
                .isEqualTo(34);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_MINUTES))
                .isEqualTo(2057);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_SECONDS))
                .isEqualTo(123456);
    }

    @Test
    public void testGetDurationPartOpNode_negativeDuration() {

        // Equivalent to negative 1day and 10h:17m:36s
        Duration duration = Duration.ofSeconds(-123456);

        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_DAYS))
                .isEqualTo(1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_HOURS))
                .isEqualTo(10);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_MINUTES))
                .isEqualTo(17);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_SECONDS))
                .isEqualTo(36);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_DAYS))
                .isEqualTo(-1);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_HOURS))
                .isEqualTo(-34);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_MINUTES))
                .isEqualTo(-2057);
        assertThat(
                        createGetDurationPartOpNodeAndGetPart(
                                duration, DurationPartType.DURATION_PART_TYPE_TOTAL_SECONDS))
                .isEqualTo(-123456);
    }

    private int createGetDurationPartOpNodeAndGetPart(Duration duration, DurationPartType part) {
        List<Integer> results = new ArrayList<>();
        GetDurationPartOpNode node =
                new GetDurationPartOpNode(
                        GetDurationPartOp.newBuilder().setDurationPart(part).build(),
                        new AddToListCallback<>(results));
        node.getIncomingCallback().onData(duration);
        return results.get(0);
    }

    @Test
    public void stateInt32NodeTest() {
        List<Integer> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setInt32Val(FixedInt32.newBuilder().setValue(65))
                                        .build()));

        StateInt32Source protoNode = StateInt32Source.newBuilder().setSourceKey("foo").build();
        StateInt32SourceNode node =
                new StateInt32SourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(65);
    }

    @Test
    public void stateInt32UpdatesWithStateChanges() {
        List<Integer> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setInt32Val(FixedInt32.newBuilder().setValue(65))
                                        .build()));

        StateInt32Source protoNode = StateInt32Source.newBuilder().setSourceKey("foo").build();
        StateInt32SourceNode node =
                new StateInt32SourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        results.clear();

        oss.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(12))
                                .build()));

        assertThat(results).containsExactly(12);
    }

    @Test
    public void animatableFixedInt32_animates() {
        int startValue = 3;
        int endValue = 33;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        AnimatableFixedInt32 protoNode =
                AnimatableFixedInt32.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedInt32Node node =
                new AnimatableFixedInt32Node(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(true);

        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(startValue);
        assertThat(Iterables.getLast(results)).isEqualTo(endValue);
    }

    @Test
    public void animatableFixedInt32_whenInvisible_skipToEnd() {
        int startValue = 3;
        int endValue = 33;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        AnimatableFixedInt32 protoNode =
                AnimatableFixedInt32.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedInt32Node node =
                new AnimatableFixedInt32Node(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(false);

        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(endValue);
    }

    @Test
    public void animatableFixedInt32_whenNoQuota_skip() {
        int startValue = 3;
        int endValue = 33;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(0);
        AnimatableFixedInt32 protoNode =
                AnimatableFixedInt32.newBuilder()
                        .setFromValue(startValue)
                        .setToValue(endValue)
                        .build();
        AnimatableFixedInt32Node node =
                new AnimatableFixedInt32Node(
                        protoNode, new AddToListCallback<>(results), quotaManager);
        node.setVisibility(true);

        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(endValue);
    }

    @Test
    public void dynamicAnimatedInt32_onlyAnimateWhenVisible() {
        int value1 = 3;
        int value2 = 11;
        int value3 = 17;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(MAX_VALUE);
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setInt32Val(
                                                FixedInt32.newBuilder().setValue(value1).build())
                                        .build()));
        DynamicAnimatedInt32Node int32Node =
                new DynamicAnimatedInt32Node(
                        new AddToListCallback<>(results),
                        AnimationSpec.getDefaultInstance(),
                        quotaManager);
        int32Node.setVisibility(false);
        StateInt32SourceNode stateNode =
                new StateInt32SourceNode(
                        oss,
                        StateInt32Source.newBuilder().setSourceKey("foo").build(),
                        int32Node.getInputCallback());

        stateNode.preInit();
        stateNode.init();

        results.clear();
        oss.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(value2))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Only contains last value.
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(value2);

        int32Node.setVisibility(true);
        results.clear();
        oss.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setInt32Val(FixedInt32.newBuilder().setValue(value3))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Contains intermediate values besides the initial and last.
        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(value2);
        assertThat(Iterables.getLast(results)).isEqualTo(value3);
        assertThat(results).isInOrder();
    }
}
