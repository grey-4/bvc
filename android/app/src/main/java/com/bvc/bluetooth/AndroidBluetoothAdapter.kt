package com.bvc.core

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.bvc.network.Device
import java.util.*

/**
 * AndroidBluetoothAdapter - Android-specific Bluetooth implementation
 * 
 * Handles:
 * - Bluetooth LE advertising and scanning
 * - Device discovery using Android Bluetooth APIs
 * - Connection management
 * - BVC protocol implementation
 */
class AndroidBluetoothAdapter(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: String
) {
    
    companion object {
        private const val TAG = "AndroidBluetoothAdapter"
        
        // BVC Service UUID
        val BVC_SERVICE_UUID: ParcelUuid = ParcelUuid.fromString("12345678-1234-5678-9abc-123456789abc")
        
        // Advertisement data constants
        private const val BVC_MANUFACTURER_ID = 0xFFFF
        private const val PROTOCOL_VERSION = 1.toByte()
        private const val DEVICE_TYPE_VOICE = 1.toByte()
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var isAdvertising = false
    private var isScanning = false
    
    private val discoveredDevices = mutableMapOf<String, Device>()
    private var discoveryCallback: ((List<Device>) -> Unit)? = null
    
    // Scan callback for device discovery
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            val device = result.device
            val rssi = result.rssi
            val scanRecord = result.scanRecord
            
            // Parse BVC advertisement data
            scanRecord?.let { record ->
                val bvcData = parseBVCAdvertisement(record)
                if (bvcData != null) {
                    val bvcDevice = Device(
                        deviceId = device.address,
                        name = device.name ?: "BVC-Device",
                        rssi = rssi,
                        lastSeen = Date(),
                        capabilities = bvcData.capabilities,
                        batteryLevel = bvcData.batteryLevel
                    )
                    
                    discoveredDevices[device.address] = bvcDevice
                    Log.d(TAG, "Discovered BVC device: ${bvcDevice.name} (${bvcDevice.deviceId})")
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error code: $errorCode")
            isScanning = false
        }
    }
    
    // Advertise callback
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertisement started successfully")
            isAdvertising = true
        }
        
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Advertisement failed with error code: $errorCode")
            isAdvertising = false
        }
    }
    
    /**
     * Initialize Bluetooth adapter
     */
    fun initialize(): Boolean {
        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return false
        }
        
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        scanner = bluetoothAdapter.bluetoothLeScanner
        
        if (advertiser == null) {
            Log.e(TAG, "Bluetooth LE Advertiser not available")
            return false
        }
        
        if (scanner == null) {
            Log.e(TAG, "Bluetooth LE Scanner not available")
            return false
        }
        
        Log.d(TAG, "Bluetooth adapter initialized successfully")
        return true
    }
    
    /**
     * Start advertising BVC service
     */
    fun startAdvertising(customData: ByteArray = byteArrayOf()): Boolean {
        if (isAdvertising) {
            Log.w(TAG, "Already advertising")
            return false
        }
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(BVC_SERVICE_UUID)
            .addManufacturerData(BVC_MANUFACTURER_ID, createBVCAdvertisementData(customData))
            .build()
        
        advertiser?.startAdvertising(settings, data, advertiseCallback)
        
        Log.d(TAG, "Started BVC advertisement")
        return true
    }
    
    /**
     * Stop advertising
     */
    fun stopAdvertising() {
        if (isAdvertising) {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Log.d(TAG, "Stopped advertisement")
        }
    }
    
    /**
     * Start scanning for BVC devices
     */
    fun startScanning(callback: (List<Device>) -> Unit): Boolean {
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return false
        }
        
        discoveryCallback = callback
        discoveredDevices.clear()
        
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(BVC_SERVICE_UUID)
            .build()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        
        scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        isScanning = true
        
        Log.d(TAG, "Started BVC device scanning")
        return true
    }
    
    /**
     * Stop scanning
     */
    fun stopScanning(): List<Device> {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Stopped scanning")
        }
        
        val devices = discoveredDevices.values.toList()
        discoveryCallback?.invoke(devices)
        return devices
    }
    
    /**
     * Get connection statistics
     */
    fun getConnectionStats(): Map<String, Any> {
        return mapOf(
            "isAdvertising" to isAdvertising,
            "isScanning" to isScanning,
            "discoveredDevices" to discoveredDevices.size,
            "bluetoothEnabled" to bluetoothAdapter.isEnabled
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopAdvertising()
        stopScanning()
        Log.d(TAG, "Bluetooth adapter cleanup completed")
    }
    
    /**
     * Create BVC advertisement data
     */
    private fun createBVCAdvertisementData(customData: ByteArray): ByteArray {
        // BVC Advertisement Structure:
        // [1 byte: Protocol Version] [1 byte: Device Type] [N bytes: Custom Data]
        val data = byteArrayOf(PROTOCOL_VERSION, DEVICE_TYPE_VOICE) + customData.take(20)
        return data
    }
    
    /**
     * Parse BVC advertisement data
     */
    private fun parseBVCAdvertisement(scanRecord: ScanRecord): BVCAdvertisementData? {
        val manufacturerData = scanRecord.getManufacturerSpecificData(BVC_MANUFACTURER_ID)
            ?: return null
        
        if (manufacturerData.size < 2) return null
        
        val protocolVersion = manufacturerData[0]
        val deviceType = manufacturerData[1]
        val customData = if (manufacturerData.size > 2) {
            manufacturerData.sliceArray(2 until manufacturerData.size)
        } else {
            byteArrayOf()
        }
        
        // Parse capabilities from custom data
        val capabilities = mutableListOf<String>()
        when (deviceType) {
            DEVICE_TYPE_VOICE -> {
                capabilities.addAll(listOf("voice", "relay"))
            }
        }
        
        return BVCAdvertisementData(
            protocolVersion = protocolVersion.toInt(),
            deviceType = deviceType.toInt(),
            capabilities = capabilities,
            batteryLevel = null // Could be extracted from custom data
        )
    }
    
    /**
     * Data class for parsed BVC advertisement
     */
    private data class BVCAdvertisementData(
        val protocolVersion: Int,
        val deviceType: Int,
        val capabilities: List<String>,
        val batteryLevel: Int?
    )
}
