package com.bvc.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bvc.R
import com.bvc.databinding.ActivityMainBinding
import com.bvc.core.BVCViewModel

/**
 * MainActivity - Main entry point for BVC Android app
 * 
 * Features:
 * - Device discovery and network topology display
 * - Voice call initiation and management
 * - Settings and configuration
 * - Permission handling
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: BVCViewModel
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeBVC()
        } else {
            showPermissionError()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[BVCViewModel::class.java]
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        binding.apply {
            // Discovery button
            btnDiscovery.setOnClickListener {
                startActivity(Intent(this@MainActivity, DiscoveryActivity::class.java))
            }
            
            // Voice call button
            btnVoiceCall.setOnClickListener {
                if (viewModel.hasDiscoveredDevices()) {
                    startActivity(Intent(this@MainActivity, VoiceCallActivity::class.java))
                } else {
                    Toast.makeText(this@MainActivity, 
                        "No devices found. Run discovery first.", 
                        Toast.LENGTH_SHORT).show()
                }
            }
            
            // Settings button
            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
            
            // Emergency call button
            btnEmergencyCall.setOnClickListener {
                viewModel.initiateEmergencyCall()
            }
        }
        
        // Observe ViewModel
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.deviceStatus.observe(this) { status ->
            binding.tvDeviceStatus.text = "Device: ${status.deviceName} (${status.deviceId})"
        }
        
        viewModel.networkStats.observe(this) { stats ->
            binding.tvNetworkStats.text = 
                "Network: ${stats.onlineDevices}/${stats.totalDevices} devices, " +
                "${stats.totalConnections} connections"
        }
        
        viewModel.isServiceRunning.observe(this) { isRunning ->
            binding.tvServiceStatus.text = if (isRunning) "Service: Running" else "Service: Stopped"
            binding.btnDiscovery.isEnabled = isRunning
            binding.btnVoiceCall.isEnabled = isRunning
        }
        
        viewModel.errorMessage.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            initializeBVC()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    private fun initializeBVC() {
        viewModel.initializeBVC(this)
    }
    
    private fun showPermissionError() {
        Toast.makeText(this, 
            "BVC requires Bluetooth and audio permissions to function", 
            Toast.LENGTH_LONG).show()
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}
