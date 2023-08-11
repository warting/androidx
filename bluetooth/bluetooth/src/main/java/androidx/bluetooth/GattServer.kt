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

package androidx.bluetooth

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice as FwkDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService as FwkService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Class for handling operations as a GATT server role
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class GattServer(private val context: Context) {
    interface FrameworkAdapter {
        var gattServer: BluetoothGattServer?
        fun openGattServer(context: Context, callback: BluetoothGattServerCallback)
        fun closeGattServer()
        fun clearServices()
        fun addService(service: FwkService)
        fun notifyCharacteristicChanged(
            device: FwkDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        )
        fun sendResponse(
            device: FwkDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        )
    }

    internal interface Session {
        companion object {
            const val STATE_DISCONNECTED = 0
            const val STATE_CONNECTING = 1
            const val STATE_CONNECTED = 2
        }

        val device: BluetoothDevice

        suspend fun acceptConnection(block: suspend BluetoothLe.GattServerSessionScope.() -> Unit)
        fun rejectConnection()

        fun sendResponse(requestId: Int, status: Int, offset: Int, value: ByteArray?)
    }

    private companion object {
        private const val TAG = "GattServer"
    }

    @SuppressLint("ObsoleteSdkInt")
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkAdapter: FrameworkAdapter =
        if (Build.VERSION.SDK_INT >= 33) FrameworkAdapterApi33()
        else FrameworkAdapterBase()

    suspend fun <R> open(
        services: List<GattService>,
        block: suspend BluetoothLe.GattServerConnectScope.() -> R
    ): Result<R> {
        return Result.success(createServerScope(services).block())
    }

    private fun createServerScope(services: List<GattService>): BluetoothLe.GattServerConnectScope {
        return object : BluetoothLe.GattServerConnectScope {
            private val attributeMap = AttributeMap()
            // Should be accessed only from the callback thread
            private val sessions: MutableMap<FwkDevice, Session> = mutableMapOf()

            override val connectRequest = callbackFlow {
                    attributeMap.updateWithServices(services)
                    val callback = object : BluetoothGattServerCallback() {
                        override fun onConnectionStateChange(
                            device: FwkDevice,
                            status: Int,
                            newState: Int
                        ) {
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    trySend(
                                        BluetoothLe.GattServerConnectRequest(
                                            addSession(device)
                                        )
                                    )
                                }

                                BluetoothProfile.STATE_DISCONNECTED -> removeSession(device)
                            }
                        }

                        override fun onCharacteristicReadRequest(
                            device: FwkDevice,
                            requestId: Int,
                            offset: Int,
                            characteristic: FwkCharacteristic
                        ) {
                            attributeMap.fromFwkCharacteristic(characteristic)?.let { char ->
                                findActiveSessionWithDevice(device)?.run {
                                    requestChannel.trySend(
                                        GattServerRequest.ReadCharacteristicRequest(
                                            this, requestId, offset, char
                                        )
                                    )
                                }
                            } ?: run {
                                fwkAdapter.sendResponse(
                                    device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED,
                                    offset, /*value=*/null
                                )
                            }
                        }

                        override fun onCharacteristicWriteRequest(
                            device: FwkDevice,
                            requestId: Int,
                            characteristic: FwkCharacteristic,
                            preparedWrite: Boolean,
                            responseNeeded: Boolean,
                            offset: Int,
                            value: ByteArray?
                        ) {
                            // TODO(b/296505524): handle preparedWrite == true
                            attributeMap.fromFwkCharacteristic(characteristic)?.let {
                                findActiveSessionWithDevice(device)?.run {
                                    requestChannel.trySend(
                                        GattServerRequest.WriteCharacteristicRequest(
                                            this,
                                            requestId,
                                            it,
                                            value
                                        )
                                    )
                                }
                            } ?: run {
                                fwkAdapter.sendResponse(
                                    device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                    offset, /*value=*/null
                                )
                            }
                        }
                    }
                    fwkAdapter.openGattServer(context, callback)
                    services.forEach { fwkAdapter.addService(it.fwkService) }

                    awaitClose {
                        fwkAdapter.closeGattServer()
                    }
                }

            override fun updateServices(services: List<GattService>) {
                fwkAdapter.clearServices()
                services.forEach { fwkAdapter.addService(it.fwkService) }
            }

            fun addSession(device: FwkDevice): Session {
                return Session(BluetoothDevice(device)).apply {
                    sessions[device] = this
                }
            }

            fun removeSession(device: FwkDevice) {
                sessions.remove(device)
            }

            fun findActiveSessionWithDevice(device: FwkDevice): Session? {
                return sessions[device]?.takeIf {
                    it.state.get() != GattServer.Session.STATE_DISCONNECTED
                }
            }

            inner class Session(override val device: BluetoothDevice) : GattServer.Session {

                val state: AtomicInteger = AtomicInteger(GattServer.Session.STATE_CONNECTING)
                val requestChannel = Channel<GattServerRequest>(Channel.UNLIMITED)

                override suspend fun acceptConnection(
                    block: suspend BluetoothLe.GattServerSessionScope.() -> Unit
                ) {
                    if (!state.compareAndSet(
                            GattServer.Session.STATE_CONNECTING,
                            GattServer.Session.STATE_CONNECTED
                        )
                    ) {
                        throw IllegalStateException("the request is already handled")
                    }

                    val scope = object : BluetoothLe.GattServerSessionScope {
                        override val device: BluetoothDevice
                            get() = this@Session.device
                        override val requests = requestChannel.receiveAsFlow()

                        override fun notify(
                            characteristic: GattCharacteristic,
                            value: ByteArray
                        ) {
                            fwkAdapter.notifyCharacteristicChanged(
                                device.fwkDevice,
                                characteristic.fwkCharacteristic,
                                false,
                                value
                            )
                        }
                    }
                    scope.block()
                }

                override fun rejectConnection() {
                    if (!state.compareAndSet(
                            GattServer.Session.STATE_CONNECTING,
                            GattServer.Session.STATE_DISCONNECTED
                        )
                    ) {
                        throw IllegalStateException("the request is already handled")
                    }
                }

                override fun sendResponse(
                    requestId: Int,
                    status: Int,
                    offset: Int,
                    value: ByteArray?
                ) {
                    fwkAdapter.sendResponse(device.fwkDevice, requestId, status, offset, value)
                }
            }
        }
    }

    private open class FrameworkAdapterBase : FrameworkAdapter {
        override var gattServer: BluetoothGattServer? = null
        private val isOpen = AtomicBoolean(false)
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun openGattServer(context: Context, callback: BluetoothGattServerCallback) {
            if (!isOpen.compareAndSet(false, true))
                throw IllegalStateException("GATT server is already opened")
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            gattServer = bluetoothManager?.openGattServer(context, callback)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun closeGattServer() {
            if (!isOpen.compareAndSet(true, false))
                throw IllegalStateException("GATT server is already closed")
            gattServer?.close()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun clearServices() {
            gattServer?.clearServices()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun addService(service: FwkService) {
            gattServer?.addService(service)
        }

        @Suppress("DEPRECATION")
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun notifyCharacteristicChanged(
            device: FwkDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ) {
            characteristic.value = value
            gattServer?.notifyCharacteristicChanged(device, characteristic, confirm)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun sendResponse(
            device: FwkDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        ) {
            gattServer?.sendResponse(device, requestId, status, offset, value)
        }
    }

    private open class FrameworkAdapterApi33 : FrameworkAdapterBase() {
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun notifyCharacteristicChanged(
            device: FwkDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ) {
            gattServer?.notifyCharacteristicChanged(device, characteristic, confirm, value)
        }
    }
}
