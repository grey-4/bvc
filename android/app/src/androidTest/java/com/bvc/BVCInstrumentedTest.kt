package com.bvc

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.bvc.ui.MainActivity
import com.bvc.audio.AndroidAudioEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented tests for BVC Android components
 * 
 * These tests run on an Android device or emulator and can test
 * Android-specific functionality like Bluetooth, audio, and UI.
 */
@RunWith(AndroidJUnit4::class)
class BVCInstrumentedTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun useAppContext() {
        // Context of the app under test
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.bvc", appContext.packageName)
    }
    
    @Test
    fun testMainActivityLaunches() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
            assertTrue(activity is MainActivity)
        }
    }
    
    @Test
    fun testAudioEngineInitializationWithRealContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioEngine = AndroidAudioEngine(appContext)
        
        // Test initialization with real Android context
        // Note: This may require runtime permissions in a real test scenario
        val stats = audioEngine.getStats()
        
        assertNotNull(stats)
        assertTrue(stats.containsKey("sample_rate"))
        assertEquals(16000, stats["sample_rate"])
    }
    
    @Test
    fun testBluetoothPermissionsRequired() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName
        
        // Check that required permissions are declared in manifest
        try {
            val packageInfo = packageManager.getPackageInfo(
                packageName, 
                android.content.pm.PackageManager.GET_PERMISSIONS
            )
            
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            
            assertTrue("BLUETOOTH permission should be declared", 
                permissions.contains(android.Manifest.permission.BLUETOOTH))
            assertTrue("BLUETOOTH_ADMIN permission should be declared", 
                permissions.contains(android.Manifest.permission.BLUETOOTH_ADMIN))
            assertTrue("RECORD_AUDIO permission should be declared", 
                permissions.contains(android.Manifest.permission.RECORD_AUDIO))
            
        } catch (e: Exception) {
            fail("Failed to check permissions: ${e.message}")
        }
    }
}
