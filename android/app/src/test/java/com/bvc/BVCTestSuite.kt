package com.bvc

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for all BVC unit tests
 * 
 * Run this to execute all unit tests together
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    VoiceManagerTest::class,
    AndroidNetworkManagerTest::class,
    AndroidAudioEngineTest::class
)
class BVCTestSuite
