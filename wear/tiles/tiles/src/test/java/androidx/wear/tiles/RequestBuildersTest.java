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

package androidx.wear.tiles;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.DeviceParametersBuilders;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicDataProto;
import androidx.wear.protolayout.expression.proto.FixedProto;
import androidx.wear.protolayout.proto.DeviceParametersProto;
import androidx.wear.protolayout.proto.StateProto;
import androidx.wear.tiles.RequestBuilders.ResourcesRequest;
import androidx.wear.tiles.RequestBuilders.TileRequest;
import androidx.wear.tiles.proto.RequestProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public final class RequestBuildersTest {

    @Test
    public void buildTileRequest_ifSetLastVisibleInstant_setsLastVisibleMillis() {
        long timestamp = 1000L;
        Instant instant = Instant.ofEpochMilli(timestamp);
        TileRequest tileRequest = new TileRequest.Builder().setLastVisibleTime(instant).build();

        RequestProto.TileRequest protoRequest = tileRequest.toProto();

        assertThat(protoRequest.getLastVisibleMillis()).isEqualTo(timestamp);
    }

    @Test
    public void buildTileRequest_ifNotSetLastVisibleInstant_setsLastVisibleMillisToZero() {
        TileRequest tileRequest = new TileRequest.Builder().build();

        RequestProto.TileRequest protoRequest = tileRequest.toProto();

        assertThat(protoRequest.getLastVisibleMillis()).isEqualTo(0L);
    }

    @Test
    public void canBuildBasicTileRequest() {
        // Build the tile request using the RequestBuilders wrapper library.
        TileRequest tileRequest =
                new TileRequest.Builder()
                        .setCurrentState(
                                new State.Builder()
                                        .addKeyToValueMapping(
                                                new AppDataKey<>("entry_id"),
                                                DynamicDataValue.fromInt(13))
                                        .build())
                        .setDeviceConfiguration(
                                new DeviceParameters.Builder()
                                        .setDevicePlatform(
                                                DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.TileRequest protoTileRequest = buildBasicProtoTileRequest();

        assertThat(tileRequest.toProto()).isEqualTo(protoTileRequest);
    }

    @Test
    @SuppressWarnings("deprecation") // for backward compatibility
    public void canBuildBasicTileRequest_compatibleDeviceConfiguration() {
        TileRequest tileRequest =
                new TileRequest.Builder()
                        .setDeviceParameters(
                                new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                                .Builder()
                                        .setDevicePlatform(
                                                androidx.wear.tiles.DeviceParametersBuilders
                                                        .DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.TileRequest protoTileRequest = buildBasicProtoTileRequest();

        assertThat(tileRequest.toProto().getDeviceConfiguration())
                .isEqualTo(protoTileRequest.getDeviceConfiguration());
    }

    @Test
    public void canBuildBasicResourcesRequest() {
        // Build the tile request using the RequestBuilders wrapper library.
        ResourcesRequest resourcesRequest =
                new ResourcesRequest.Builder()
                        .addResourceId("resource_id_1")
                        .addResourceId("resource_id_2")
                        .setVersion("some_version")
                        .setDeviceConfiguration(
                                new DeviceParameters.Builder()
                                        .setDevicePlatform(
                                                DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.ResourcesRequest protoResourcesRequest = buildBasicProtoResourcesRequest();

        assertThat(resourcesRequest.toProto()).isEqualTo(protoResourcesRequest);
    }

    @Test
    @SuppressWarnings("deprecation") // for backward compatibility
    public void canBuildBasicResourcesRequest_compatibleDeviceConfiguration() {
        ResourcesRequest resourcesRequest =
                new ResourcesRequest.Builder()
                        .addResourceId("resource_id_1")
                        .addResourceId("resource_id_2")
                        .setVersion("some_version")
                        .setDeviceParameters(
                                new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                                .Builder()
                                        .setDevicePlatform(
                                                androidx.wear.tiles.DeviceParametersBuilders
                                                        .DEVICE_PLATFORM_WEAR_OS)
                                        .build())
                        .build();

        // Build same request in proto format.
        RequestProto.ResourcesRequest protoResourcesRequest = buildBasicProtoResourcesRequest();

        assertThat(resourcesRequest.toProto()).isEqualTo(protoResourcesRequest);
    }

    private RequestProto.TileRequest buildBasicProtoTileRequest() {
        return RequestProto.TileRequest.newBuilder()
                .setCurrentState(
                        StateProto.State.newBuilder()
                                .putIdToValue(
                                        "entry_id",
                                        DynamicDataProto.DynamicDataValue.newBuilder()
                                                .setInt32Val(
                                                        FixedProto.FixedInt32.newBuilder()
                                                                .setValue(13))
                                                .build()))
                .setDeviceConfiguration(
                        DeviceParametersProto.DeviceParameters.newBuilder()
                                .setDevicePlatform(
                                        DeviceParametersProto.DevicePlatform
                                                .DEVICE_PLATFORM_WEAR_OS))
                .build();
    }

    private RequestProto.ResourcesRequest buildBasicProtoResourcesRequest() {
        return RequestProto.ResourcesRequest.newBuilder()
                .addResourceIds("resource_id_1")
                .addResourceIds("resource_id_2")
                .setVersion("some_version")
                .setDeviceConfiguration(
                        DeviceParametersProto.DeviceParameters.newBuilder()
                                .setDevicePlatform(
                                        DeviceParametersProto.DevicePlatform
                                                .DEVICE_PLATFORM_WEAR_OS))
                .build();
    }
}
