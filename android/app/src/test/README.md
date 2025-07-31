# BVC Testing Guide

## Test Structure

The BVC Android application includes comprehensive testing at multiple levels:

### Unit Tests (`src/test/`)
- **VoiceManagerTest.kt**: Tests core voice call functionality
- **AndroidNetworkManagerTest.kt**: Tests network discovery and routing logic  
- **AndroidAudioEngineTest.kt**: Tests audio processing components
- **BVCTestSuite.kt**: Runs all unit tests together

### Instrumented Tests (`src/androidTest/`)
- **BVCInstrumentedTest.kt**: Tests with actual Android components and permissions

## Running Tests

### From Android Studio
1. Right-click on the test class or method
2. Select "Run 'TestName'"

### From Command Line
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.bvc.VoiceManagerTest"

# Run all instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bvc.BVCInstrumentedTest
```

### Test Reports
After running tests, view HTML reports at:
- Unit tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Instrumented tests: `app/build/reports/androidTests/connected/index.html`

## Test Requirements

### For Unit Tests
- No special requirements (run on JVM)
- Mocked Android dependencies

### For Instrumented Tests
- Connected Android device or emulator
- Device must support:
  - Bluetooth 5.0+
  - Android 7.0+ (API 24+)
  - Microphone access
- Runtime permissions may need to be granted manually

## Test Coverage

Currently testing:
- ✅ Basic component initialization
- ✅ Audio engine statistics
- ✅ Permission declarations
- ✅ App context and main activity

TODO - Implement actual tests for:
- [ ] Bluetooth LE advertising and scanning
- [ ] Voice call establishment and termination
- [ ] Audio capture and playback
- [ ] Network routing algorithms
- [ ] UI interactions
- [ ] Error handling and edge cases

## Writing New Tests

### Unit Test Template
```kotlin
@Test
fun testMethodName_Scenario_ExpectedResult() {
    // Arrange
    val input = "test input"
    
    // Act
    val result = methodUnderTest(input)
    
    // Assert
    assertEquals(expectedValue, result)
}
```

### Instrumented Test Template
```kotlin
@Test
fun testAndroidComponent_Scenario_ExpectedResult() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    // Test with real Android components
    // Assert expected behavior
}
```

## Mock Strategy

- **Unit Tests**: Mock all Android dependencies (Context, AudioRecord, etc.)
- **Integration Tests**: Use real Android components where possible
- **UI Tests**: Use Espresso for user interaction testing

## Continuous Integration

Tests are designed to run in CI environments:
- Unit tests run on any JVM
- Instrumented tests require Android emulator in CI
