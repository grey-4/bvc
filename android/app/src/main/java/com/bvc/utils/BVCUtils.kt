package com.bvc.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest
import java.util.*

/**
 * Utility functions for BVC Android app
 */
object BVCUtils {
    
    private const val TAG = "BVCUtils"
    private const val PREFS_NAME = "bvc_preferences"
    
    /**
     * Generate unique device identifier
     */
    fun generateDeviceId(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return uuid.substring(0, 16)
    }
    
    /**
     * Calculate RSSI-based distance estimation
     */
    fun calculateDistance(rssi: Int): Double {
        if (rssi == 0) return -1.0
        
        // Simple path loss model for Bluetooth
        val ratio = (-40 - rssi) / 20.0
        return if (ratio < 1.0) 1.0 else Math.pow(10.0, ratio)
    }
    
    /**
     * Format byte count as human readable string
     */
    fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
    
    /**
     * Format duration in seconds as human readable string
     */
    fun formatDuration(seconds: Double): String {
        return when {
            seconds < 60 -> "${seconds.toInt()}s"
            seconds < 3600 -> "${(seconds / 60).toInt()}m ${(seconds % 60).toInt()}s"
            else -> "${(seconds / 3600).toInt()}h ${((seconds % 3600) / 60).toInt()}m"
        }
    }
    
    /**
     * Get shared preferences for BVC
     */
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save device configuration
     */
    fun saveDeviceConfig(context: Context, deviceName: String, deviceId: String) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putString("device_name", deviceName)
            .putString("device_id", deviceId)
            .apply()
    }
    
    /**
     * Load device configuration
     */
    fun loadDeviceConfig(context: Context): Pair<String, String> {
        val prefs = getPreferences(context)
        val deviceName = prefs.getString("device_name", "BVC-Android") ?: "BVC-Android"
        val deviceId = prefs.getString("device_id", generateDeviceId()) ?: generateDeviceId()
        
        // Save device ID if it was generated
        if (!prefs.contains("device_id")) {
            saveDeviceConfig(context, deviceName, deviceId)
        }
        
        return Pair(deviceName, deviceId)
    }
    
    /**
     * Validate device name format
     */
    fun validateDeviceName(name: String): Boolean {
        return name.isNotEmpty() && 
               name.length <= 32 && 
               name.matches(Regex("[a-zA-Z0-9\\s\\-_]+"))
    }
    
    /**
     * Generate hash for data integrity
     */
    fun generateHash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) 
            as? android.bluetooth.BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }
    
    /**
     * Log network statistics
     */
    fun logNetworkStats(stats: Map<String, Any>) {
        Log.d(TAG, "Network Stats: $stats")
    }
    
    /**
     * Create BVC advertisement data
     */
    fun createAdvertisementData(deviceName: String, capabilities: List<String>): ByteArray {
        val capabilityString = capabilities.joinToString(",")
        val data = "$deviceName:$capabilityString"
        return data.toByteArray().take(20).toByteArray() // Limit to 20 bytes
    }
    
    /**
     * Parse BVC advertisement data
     */
    fun parseAdvertisementData(data: ByteArray): Pair<String, List<String>>? {
        return try {
            val dataString = String(data)
            val parts = dataString.split(":")
            if (parts.size >= 2) {
                val deviceName = parts[0]
                val capabilities = parts[1].split(",").filter { it.isNotEmpty() }
                Pair(deviceName, capabilities)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse advertisement data", e)
            null
        }
    }
}
