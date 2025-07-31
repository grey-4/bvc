package com.bvc.network

import java.util.*

/**
 * Device - Represents a discovered device in the BVC network
 */
data class Device(
    val deviceId: String,
    val name: String,
    val rssi: Int,
    val lastSeen: Date,
    val capabilities: List<String> = emptyList(),
    val batteryLevel: Int? = null
) {
    val isOnline: Boolean
        get() = (System.currentTimeMillis() - lastSeen.time) < 60000L // 1 minute
}

/**
 * Route - Represents a communication route to a target device
 */
data class Route(
    val target: String,
    val hops: List<String>,
    val totalDistance: Double,
    val qualityScore: Double,
    val estimatedLatency: Double
) {
    val hopCount: Int
        get() = hops.size - 1
}

/**
 * NetworkTopology - Represents the current network topology
 */
data class NetworkTopology(
    val devices: MutableMap<String, Device> = mutableMapOf(),
    val connections: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var lastUpdated: Date = Date()
) {
    fun getNeighbors(deviceId: String): List<String> {
        return connections[deviceId]?.toList() ?: emptyList()
    }
}
