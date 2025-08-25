package com.bvc.network

import android.util.Log
import com.bvc.bluetooth.AndroidBluetoothAdapter
import kotlinx.coroutines.delay
import java.util.*

/**
 * AndroidNetworkManager - Android-specific network management
 * 
 * Handles:
 * - Device discovery using Android Bluetooth APIs
 * - Network topology construction
 * - Route finding optimized for mobile devices
 * - Battery-aware routing
 */
class AndroidNetworkManager(
    private val deviceId: String,
    private val bluetoothAdapter: AndroidBluetoothAdapter,
    private val maxHops: Int = 3
) {
    
    companion object {
        private const val TAG = "AndroidNetworkManager"
    }
    
    private val topology = NetworkTopology()
    private val routeCache = mutableMapOf<String, Route>()
    private val routeCacheTtl = 30000L // 30 seconds
    
    /**
     * Discover nearby BVC devices
     */
    suspend fun discoverDevices(timeoutSeconds: Int): List<Device> {
        Log.d(TAG, "Starting device discovery for $timeoutSeconds seconds")
        
        val discoveredDevices = mutableListOf<Device>()
        
        // Start Bluetooth scanning
        bluetoothAdapter.startScanning { devices ->
            discoveredDevices.addAll(devices)
            updateTopology(devices)
        }
        
        // Wait for discovery timeout
        delay(timeoutSeconds * 1000L)
        
        // Stop scanning and get final results
        val finalDevices = bluetoothAdapter.stopScanning()
        
        Log.d(TAG, "Discovery completed: found ${finalDevices.size} devices")
        return finalDevices
    }
    
    /**
     * Find optimal route to target device
     */
    fun findRoute(targetDeviceId: String): Route? {
        // Check cache first
        val cachedRoute = routeCache[targetDeviceId]
        if (cachedRoute != null && isRouteCacheValid(cachedRoute)) {
            Log.d(TAG, "Using cached route to $targetDeviceId")
            return cachedRoute
        }
        
        Log.d(TAG, "Finding route to $targetDeviceId")
        
        val route = findBestRoute(targetDeviceId)
        if (route != null) {
            routeCache[targetDeviceId] = route
            Log.d(TAG, "Found route to $targetDeviceId: ${route.hops.joinToString(" -> ")}")
        } else {
            Log.w(TAG, "No route found to $targetDeviceId")
        }
        
        return route
    }
    
    /**
     * Update network topology
     */
    fun updateTopology(): NetworkTopology {
        val currentTime = System.currentTimeMillis()
        
        // Remove stale devices (older than 2 minutes)
        val staleDevices = topology.devices.filter { (_, device) ->
            currentTime - device.lastSeen.time > 120000L
        }.keys
        
        staleDevices.forEach { deviceId ->
            removeDevice(deviceId)
        }
        
        // Clear stale route cache
        cleanupRouteCache()
        
        topology.lastUpdated = Date()
        return topology
    }
    
    /**
     * Get network statistics
     */
    fun getNetworkStats(): Map<String, Any> {
        val onlineDevices = topology.devices.values.count { device ->
            System.currentTimeMillis() - device.lastSeen.time < 60000L
        }
        
        return mapOf(
            "total_devices" to topology.devices.size,
            "online_devices" to onlineDevices,
            "total_connections" to (topology.connections.values.sumOf { it.size } / 2),
            "cached_routes" to routeCache.size,
            "last_updated" to topology.lastUpdated.time
        )
    }
    
    /**
     * Update topology with discovered devices
     */
    private fun updateTopology(devices: List<Device>) {
        // Add/update devices
        devices.forEach { device ->
            topology.devices[device.deviceId] = device
        }
        
        // Update connections based on proximity
        updateConnections(devices)
        
        topology.lastUpdated = Date()
    }
    
    /**
     * Update connection graph based on device proximity
     */
    private fun updateConnections(devices: List<Device>) {
        devices.forEach { device ->
            if (!topology.connections.containsKey(device.deviceId)) {
                topology.connections[device.deviceId] = mutableSetOf()
            }
            
            // Connect devices within reasonable range (RSSI > -70 dBm)
            devices.forEach { otherDevice ->
                if (device.deviceId != otherDevice.deviceId &&
                    device.rssi > -70 && otherDevice.rssi > -70) {
                    
                    topology.connections[device.deviceId]?.add(otherDevice.deviceId)
                    
                    if (!topology.connections.containsKey(otherDevice.deviceId)) {
                        topology.connections[otherDevice.deviceId] = mutableSetOf()
                    }
                    topology.connections[otherDevice.deviceId]?.add(device.deviceId)
                }
            }
        }
    }
    
    /**
     * Find best route using Dijkstra's algorithm optimized for mobile
     */
    private fun findBestRoute(targetDeviceId: String): Route? {
        if (!topology.devices.containsKey(targetDeviceId)) {
            return null
        }
        
        val distances = mutableMapOf<String, Double>()
        val previous = mutableMapOf<String, String>()
        val unvisited = mutableSetOf<String>()
        
        // Initialize distances
        topology.devices.keys.forEach { deviceId ->
            distances[deviceId] = if (deviceId == this.deviceId) 0.0 else Double.MAX_VALUE
            unvisited.add(deviceId)
        }
        
        while (unvisited.isNotEmpty()) {
            // Find unvisited node with minimum distance
            val current = unvisited.minByOrNull { distances[it] ?: Double.MAX_VALUE }
                ?: break
            
            if (distances[current] == Double.MAX_VALUE) {
                break // No more reachable nodes
            }
            
            if (current == targetDeviceId) {
                // Reconstruct path
                val path = mutableListOf<String>()
                var temp: String? = current
                while (temp != null) {
                    path.add(0, temp)
                    temp = previous[temp]
                }
                
                if (path.size - 1 <= maxHops) {
                    return Route(
                        target = targetDeviceId,
                        hops = path,
                        totalDistance = distances[current] ?: Double.MAX_VALUE,
                        qualityScore = calculateRouteQuality(path),
                        estimatedLatency = estimateLatency(path)
                    )
                }
                break
            }
            
            unvisited.remove(current)
            
            // Check neighbors
            topology.connections[current]?.forEach { neighbor ->
                if (neighbor in unvisited) {
                    val weight = calculateEdgeWeight(current, neighbor)
                    val altDistance = (distances[current] ?: Double.MAX_VALUE) + weight
                    
                    if (altDistance < (distances[neighbor] ?: Double.MAX_VALUE)) {
                        distances[neighbor] = altDistance
                        previous[neighbor] = current
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Calculate edge weight based on signal strength, battery, etc.
     */
    private fun calculateEdgeWeight(fromDevice: String, toDevice: String): Double {
        val toDeviceInfo = topology.devices[toDevice] ?: return Double.MAX_VALUE
        
        // Base weight based on RSSI (higher RSSI = lower weight)
        val rssiWeight = kotlin.math.max(0.1, (-toDeviceInfo.rssi) / 100.0)
        
        // Battery penalty for mobile devices
        val batteryWeight = when {
            toDeviceInfo.batteryLevel == null -> 1.0 // Unknown battery (assume good)
            toDeviceInfo.batteryLevel < 20 -> 3.0    // Very low battery
            toDeviceInfo.batteryLevel < 50 -> 2.0    // Low battery
            else -> 1.0                              // Good battery
        }
        
        return rssiWeight * batteryWeight
    }
    
    /**
     * Calculate overall route quality score (0.0 - 1.0)
     */
    private fun calculateRouteQuality(path: List<String>): Double {
        if (path.size < 2) return 1.0
        
        var totalQuality = 0.0
        for (i in 1 until path.size) {
            val device = topology.devices[path[i]]
            if (device != null) {
                // Quality based on RSSI and battery
                val rssiQuality = kotlin.math.max(0.0, kotlin.math.min(1.0, (-device.rssi + 100) / 50.0))
                val batteryQuality = when {
                    device.batteryLevel == null -> 1.0
                    else -> device.batteryLevel / 100.0
                }
                
                val hopQuality = (rssiQuality + batteryQuality) / 2.0
                totalQuality += hopQuality
            }
        }
        
        return totalQuality / (path.size - 1)
    }
    
    /**
     * Estimate end-to-end latency in milliseconds
     */
    private fun estimateLatency(path: List<String>): Double {
        // Base latency per hop + processing delay for mobile
        val baseLatencyPerHop = 25.0 // ms (slightly higher for mobile)
        val processingDelay = 15.0   // ms per hop (mobile processing overhead)
        
        return (path.size - 1) * (baseLatencyPerHop + processingDelay)
    }
    
    /**
     * Remove device from topology
     */
    private fun removeDevice(deviceId: String) {
        topology.devices.remove(deviceId)
        topology.connections.remove(deviceId)
        
        // Remove connections to this device
        topology.connections.values.forEach { connections ->
            connections.remove(deviceId)
        }
    }
    
    /**
     * Check if cached route is still valid
     */
    private fun isRouteCacheValid(route: Route): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Check if all devices in route are still online
        return route.hops.all { deviceId ->
            val device = topology.devices[deviceId]
            device != null && (currentTime - device.lastSeen.time) < 60000L
        }
    }
    
    /**
     * Remove stale entries from route cache
     */
    private fun cleanupRouteCache() {
        val staleRoutes = routeCache.filter { (_, route) ->
            !isRouteCacheValid(route)
        }.keys
        
        staleRoutes.forEach { target ->
            routeCache.remove(target)
        }
    }
}
