package com.bvc.core

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bvc.bluetooth.AndroidBluetoothAdapter
import com.bvc.network.AndroidNetworkManager
import com.bvc.audio.AndroidAudioEngine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * BVCViewModel - Main ViewModel for BVC Android application
 * 
 * Manages:
 * - Application state and lifecycle
 * - Bluetooth operations
 * - Network management
 * - Audio engine
 * - UI state updates
 */
class BVCViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "BVCViewModel"
    }
    
    // Core components
    private var bluetoothAdapter: AndroidBluetoothAdapter? = null
    private var networkManager: AndroidNetworkManager? = null
    private var audioEngine: AndroidAudioEngine? = null
    private var voiceManager: VoiceManager? = null
    
    // Device info
    private val deviceId = generateDeviceId()
    private val deviceName = "BVC-Android-${deviceId.substring(0, 6)}"
    
    // LiveData for UI
    private val _deviceStatus = MutableLiveData<DeviceStatus>()
    val deviceStatus: LiveData<DeviceStatus> = _deviceStatus
    
    private val _networkStats = MutableLiveData<NetworkStats>()
    val networkStats: LiveData<NetworkStats> = _networkStats
    
    private val _isServiceRunning = MutableLiveData<Boolean>()
    val isServiceRunning: LiveData<Boolean> = _isServiceRunning
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _discoveredDevices = MutableLiveData<List<com.bvc.network.Device>>()
    val discoveredDevices: LiveData<List<com.bvc.network.Device>> = _discoveredDevices
    
    private val _activeCalls = MutableLiveData<List<CallSession>>()
    val activeCalls: LiveData<List<CallSession>> = _activeCalls
    
    init {
        _deviceStatus.value = DeviceStatus(deviceId, deviceName, false)
        _networkStats.value = NetworkStats(0, 0, 0)
        _isServiceRunning.value = false
        _errorMessage.value = ""
        _discoveredDevices.value = emptyList()
        _activeCalls.value = emptyList()
    }
    
    /**
     * Initialize BVC components
     */
    fun initializeBVC(context: Context) {
        viewModelScope.launch {
            try {
                // Initialize Bluetooth adapter
                bluetoothAdapter = AndroidBluetoothAdapter(context, deviceId, deviceName)
                if (!bluetoothAdapter!!.initialize()) {
                    _errorMessage.value = "Failed to initialize Bluetooth adapter"
                    return@launch
                }
                
                // Initialize network manager
                networkManager = AndroidNetworkManager(deviceId, bluetoothAdapter!!)
                
                // Initialize audio engine
                audioEngine = AndroidAudioEngine(context)
                if (!audioEngine!!.initialize()) {
                    _errorMessage.value = "Failed to initialize audio engine"
                    return@launch
                }
                
                // Initialize voice manager
                voiceManager = VoiceManager(deviceId)
                
                // Start advertising
                bluetoothAdapter!!.startAdvertising()
                
                _isServiceRunning.value = true
                _deviceStatus.value = DeviceStatus(deviceId, deviceName, true)
                
                // Start periodic status updates
                startStatusUpdates()
                
            } catch (e: Exception) {
                _errorMessage.value = "Initialization failed: ${e.message}"
            }
        }
    }
    
    /**
     * Start device discovery
     */
    fun startDiscovery(timeoutSeconds: Int = 30) {
        viewModelScope.launch {
            try {
                networkManager?.let { manager ->
                    val devices = manager.discoverDevices(timeoutSeconds)
                    _discoveredDevices.value = devices
                    updateNetworkStats()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Discovery failed: ${e.message}"
            }
        }
    }
    
    /**
     * Start voice call to target device
     */
    fun startVoiceCall(targetDeviceId: String) {
        viewModelScope.launch {
            try {
                voiceManager?.let { manager ->
                    val session = manager.startCall(targetDeviceId)
                    _activeCalls.value = manager.getActiveCalls()
                    
                    // Start audio capture
                    audioEngine?.startCapture()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start call: ${e.message}"
            }
        }
    }
    
    /**
     * End voice call
     */
    fun endVoiceCall(callId: String) {
        viewModelScope.launch {
            try {
                voiceManager?.let { manager ->
                    manager.endCall(callId)
                    _activeCalls.value = manager.getActiveCalls()
                    
                    // Stop audio if no active calls
                    if (manager.getActiveCalls().isEmpty()) {
                        audioEngine?.stopCapture()
                        audioEngine?.stopPlayback()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to end call: ${e.message}"
            }
        }
    }
    
    /**
     * Initiate emergency call (broadcast to all devices)
     */
    fun initiateEmergencyCall() {
        viewModelScope.launch {
            try {
                val devices = _discoveredDevices.value ?: emptyList()
                if (devices.isNotEmpty()) {
                    // For emergency, try to call the first available device
                    startVoiceCall(devices.first().deviceId)
                } else {
                    _errorMessage.value = "No devices available for emergency call"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Emergency call failed: ${e.message}"
            }
        }
    }
    
    /**
     * Check if any devices have been discovered
     */
    fun hasDiscoveredDevices(): Boolean {
        return _discoveredDevices.value?.isNotEmpty() == true
    }
    
    /**
     * Refresh status information
     */
    fun refreshStatus() {
        updateNetworkStats()
        updateActiveCalls()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        viewModelScope.launch {
            audioEngine?.cleanup()
            bluetoothAdapter?.cleanup()
            _isServiceRunning.value = false
        }
    }
    
    /**
     * Start periodic status updates
     */
    private fun startStatusUpdates() {
        viewModelScope.launch {
            while (_isServiceRunning.value == true) {
                updateNetworkStats()
                updateActiveCalls()
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    /**
     * Update network statistics
     */
    private fun updateNetworkStats() {
        networkManager?.let { manager ->
            val stats = manager.getNetworkStats()
            _networkStats.value = NetworkStats(
                totalDevices = stats["total_devices"] as? Int ?: 0,
                onlineDevices = stats["online_devices"] as? Int ?: 0,
                totalConnections = stats["total_connections"] as? Int ?: 0
            )
        }
    }
    
    /**
     * Update active calls
     */
    private fun updateActiveCalls() {
        voiceManager?.let { manager ->
            _activeCalls.value = manager.getActiveCalls()
        }
    }
    
    /**
     * Generate unique device ID
     */
    private fun generateDeviceId(): String {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }
    
    // Data classes for UI state
    data class DeviceStatus(
        val deviceId: String,
        val deviceName: String,
        val isInitialized: Boolean
    )
    
    data class NetworkStats(
        val totalDevices: Int,
        val onlineDevices: Int,
        val totalConnections: Int
    )
}
