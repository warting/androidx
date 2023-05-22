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

package androidx.bluetooth.integration.testapp.ui.scanner

// TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentScannerBinding
import androidx.bluetooth.integration.testapp.experimental.BluetoothLe
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScannerFragment : Fragment() {

    internal companion object {
        private const val TAG = "ScannerFragment"

        private const val TAB_RESULTS_POSITION = 0

        internal const val MANUAL_DISCONNECT = "MANUAL_DISCONNECT"
    }

    private lateinit var scannerViewModel: ScannerViewModel

    // TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
    private lateinit var bluetoothLe: BluetoothLe

    private var scannerAdapter: ScannerAdapter? = null

    private var deviceServicesAdapter: DeviceServicesAdapter? = null

    private val scanScope = CoroutineScope(Dispatchers.Main + Job())
    private var scanJob: Job? = null

    private val connectScope = CoroutineScope(Dispatchers.Default + Job())

    private var isScanning: Boolean = false
        set(value) {
            field = value
            if (value) {
                _binding?.buttonScan?.text = getString(R.string.stop_scanning)
                _binding?.buttonScan?.backgroundTintList = getColor(R.color.red_500)
            } else {
                _binding?.buttonScan?.text = getString(R.string.start_scanning)
                _binding?.buttonScan?.backgroundTintList = getColor(R.color.indigo_500)
                scanJob?.cancel()
                scanJob = null
            }
        }

    private var showingScanResults: Boolean = false
        set(value) {
            field = value
            _binding?.layoutScanResults?.isVisible = value
            _binding?.layoutDevice?.isVisible = !value
        }

    private val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: Tab) {
            showingScanResults = tab.position == TAB_RESULTS_POSITION
            if (tab.position != TAB_RESULTS_POSITION) {
                updateDeviceUI(scannerViewModel.deviceConnection(tab.position))
            }
        }

        override fun onTabUnselected(tab: Tab) {
        }

        override fun onTabReselected(tab: Tab) {
        }
    }

    private val onClickReadCharacteristic = object : OnClickReadCharacteristic {
        override fun onClick(
            deviceConnection: DeviceConnection,
            characteristic: BluetoothGattCharacteristic
        ) {
            deviceConnection.onClickReadCharacteristic?.onClick(deviceConnection, characteristic)
        }
    }

    private var _binding: FragmentScannerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        scannerViewModel = ViewModelProvider(this)[ScannerViewModel::class.java]

        bluetoothLe = BluetoothLe(requireContext())

        _binding = FragmentScannerBinding.inflate(inflater, container, false)

        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)

        scannerAdapter = ScannerAdapter { bluetoothDevice -> onClickScanResult(bluetoothDevice) }
        binding.recyclerViewScanResults.adapter = scannerAdapter
        binding.recyclerViewScanResults.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        deviceServicesAdapter = DeviceServicesAdapter(null, onClickReadCharacteristic)
        binding.recyclerViewDeviceServices.adapter = deviceServicesAdapter
        binding.recyclerViewDeviceServices.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        binding.buttonScan.setOnClickListener {
            if (scanJob?.isActive == true) {
                isScanning = false
            } else {
                startScan()
            }
        }

        binding.buttonReconnect.setOnClickListener {
            connectTo(scannerViewModel.deviceConnection(binding.tabLayout.selectedTabPosition))
        }

        binding.buttonDisconnect.setOnClickListener {
            disconnect(scannerViewModel.deviceConnection(binding.tabLayout.selectedTabPosition))
        }

        initData()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isScanning = false
    }

    private fun initData() {
        scannerAdapter?.submitList(scannerViewModel.scanResults)
        scannerAdapter?.notifyItemRangeChanged(0, scannerViewModel.scanResults.size)

        scannerViewModel.deviceConnections.map { it.bluetoothDevice }.forEach(::addNewTab)
    }

    private fun startScan() {
        // TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
        val scanSettings = ScanSettings.Builder()
            .build()

        scanJob = scanScope.launch {
            isScanning = true

            bluetoothLe.scan(scanSettings)
                .collect {
                    Log.d(TAG, "ScanResult collected: $it")

                    if (scannerViewModel.addScanResultIfNew(it)) {
                        scannerAdapter?.submitList(scannerViewModel.scanResults)
                        scannerAdapter?.notifyItemInserted(scannerViewModel.scanResults.size)
                    }
                }
        }
    }

    private fun onClickScanResult(bluetoothDevice: BluetoothDevice) {
        isScanning = false

        val index = scannerViewModel.addDeviceConnectionIfNew(bluetoothDevice)

        val deviceTab = if (index == ScannerViewModel.NEW_DEVICE) {
            addNewTab(bluetoothDevice)
        } else {
            binding.tabLayout.getTabAt(index)
        }

        // To prevent TabSelectedListener being triggered when a tab is programmatically selected.
        binding.tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
        binding.tabLayout.selectTab(deviceTab)
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)

        showingScanResults = false

        connectTo(scannerViewModel.deviceConnection(binding.tabLayout.selectedTabPosition))
    }

    @SuppressLint("MissingPermission")
    private fun addNewTab(bluetoothDevice: BluetoothDevice): Tab {
        val deviceAddress = bluetoothDevice.address
        val deviceName = bluetoothDevice.name

        val newTab = binding.tabLayout.newTab()
        newTab.setCustomView(R.layout.tab_item_device)

        val customView = newTab.customView
        customView?.findViewById<TextView>(R.id.text_view_address)?.text = deviceAddress
        val textViewName = customView?.findViewById<TextView>(R.id.text_view_name)
        textViewName?.text = deviceName
        textViewName?.isVisible = deviceName.isNullOrEmpty().not()
        customView?.findViewById<Button>(R.id.image_button_remove)?.setOnClickListener {
            scannerViewModel.remove(bluetoothDevice)
            binding.tabLayout.removeTab(newTab)
        }

        binding.tabLayout.addTab(newTab)
        return newTab
    }

    private fun connectTo(deviceConnection: DeviceConnection) {
        Log.d(TAG, "connectTo() called with: deviceConnection = $deviceConnection")

        deviceConnection.job = connectScope.launch {
            deviceConnection.status = Status.CONNECTING
            launch(Dispatchers.Main) {
                updateDeviceUI(deviceConnection)
            }

            try {
                bluetoothLe.connectGatt(requireContext(), deviceConnection.bluetoothDevice) {
                    Log.d(TAG, "connectGatt result: getServices() = ${getServices()}")

                    deviceConnection.status = Status.CONNECTED
                    deviceConnection.services = getServices()
                    launch(Dispatchers.Main) {
                        updateDeviceUI(deviceConnection)
                    }

                    // TODO(ofy) Improve this. Remove OnClickReadCharacteristic as it's not ideal
                    // to hold so many OnClickReadCharacteristic and difficult to use with Compose.
                    deviceConnection.onClickReadCharacteristic =
                        object : OnClickReadCharacteristic {
                        override fun onClick(
                            deviceConnection: DeviceConnection,
                            characteristic: BluetoothGattCharacteristic
                        ) {
                            connectScope.launch {
                                val result = readCharacteristic(characteristic)
                                Log.d(TAG, "readCharacteristic() called with: result = $result")

                                deviceConnection.storeValueFor(characteristic, result.getOrNull())
                                launch(Dispatchers.Main) {
                                    updateDeviceUI(deviceConnection)
                                }
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    Log.d(TAG, "connectGatt() CancellationException")
                } else {
                    Log.e(TAG, "connectGatt() exception", exception)
                    deviceConnection.status = Status.CONNECTION_FAILED
                    launch(Dispatchers.Main) {
                        updateDeviceUI(deviceConnection)
                    }
                }
            }
        }
    }

    private fun disconnect(deviceConnection: DeviceConnection) {
        Log.d(TAG, "disconnect() called with: deviceConnection = $deviceConnection")

        deviceConnection.job?.cancel(MANUAL_DISCONNECT)
        deviceConnection.job = null
        deviceConnection.status = Status.NOT_CONNECTED
        updateDeviceUI(deviceConnection)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeviceUI(deviceConnection: DeviceConnection) {
        binding.progressIndicatorDeviceConnection.isVisible = false
        binding.buttonReconnect.isVisible = false
        binding.buttonDisconnect.isVisible = false

        when (deviceConnection.status) {
            Status.NOT_CONNECTED -> {
                binding.textViewDeviceConnectionStatus.text = getString(R.string.not_connected)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.green_500))
                binding.buttonReconnect.isVisible = true
            }
            Status.CONNECTING -> {
                binding.progressIndicatorDeviceConnection.isVisible = true
                binding.textViewDeviceConnectionStatus.text = getString(R.string.connecting)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.indigo_500))
            }
            Status.CONNECTED -> {
                binding.textViewDeviceConnectionStatus.text = getString(R.string.connected)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.indigo_500))
                binding.buttonDisconnect.isVisible = true
            }
            Status.CONNECTION_FAILED -> {
                binding.textViewDeviceConnectionStatus.text = getString(R.string.connection_failed)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.red_500))
                binding.buttonReconnect.isVisible = true
            }
        }
        deviceServicesAdapter?.deviceConnection = deviceConnection
        deviceServicesAdapter?.notifyDataSetChanged()
    }
}
