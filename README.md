# BVC - Bluetooth Voice Communication

An Android application for short-distance voice communication utilizing Bluetooth technology for peer-to-peer audio calls with intelligent path-finding capabilities.

## Overview

BVC (Bluetooth Voice Communication) is a native Android application that creates a decentralized voice communication system designed for short-range scenarios where traditional cellular or WiFi infrastructure may be unavailable or unreliable. The system leverages Bluetooth's broadcasting capabilities and implements custom routing protocols to establish voice communication paths between Android devices.

## Features

- **Native Android Application**: Optimized for Android devices with modern Bluetooth capabilities
- **Short-Range Voice Communication**: Direct peer-to-peer voice calls within Bluetooth range (~10-100 meters)
- **Mesh Networking**: Multi-hop routing through intermediate Android devices to extend communication range
- **Broadcasting Protocol**: Efficient device discovery and network topology mapping using Bluetooth LE
- **Adaptive Path Finding**: Dynamic route selection based on signal strength and network conditions
- **Low Latency Audio**: Optimized audio encoding and transmission for real-time communication using Android audio APIs
- **Battery Efficient**: Power-optimized protocols to maximize device battery life
- **Emergency Communication**: Priority emergency calling with optimized routing

## Architecture

### Android Components

1. **MainActivity**: Main user interface for device discovery and call management
2. **VoiceManager**: Handles voice call lifecycle and audio processing coordination
3. **AndroidBluetoothAdapter**: Android Bluetooth LE implementation for device communication
4. **AndroidNetworkManager**: Manages mesh networking and routing between Android devices
5. **AndroidAudioEngine**: Native Android audio capture, encoding, decoding, and playback
6. **BVCViewModel**: MVVM architecture with LiveData for reactive UI updates

### Android-Specific Features

- **Bluetooth LE Integration**: Uses Android's BluetoothLeAdvertiser and BluetoothLeScanner
- **Audio Processing**: Leverages AudioRecord and AudioTrack for low-latency audio
- **Background Services**: Maintains connectivity even when app is backgrounded
- **Permissions Management**: Handles runtime permissions for Bluetooth, audio, and location
- **Battery Optimization**: Integrates with Android's battery optimization features

### Communication Protocols

#### Device Discovery Protocol (Android Bluetooth LE)
- Uses BluetoothLeAdvertiser for periodic advertising of device capabilities
- BluetoothLeScanner for neighbor discovery and signal strength measurement
- Network topology construction and maintenance using Android's Bluetooth APIs

#### Routing Protocol
- **Algorithm**: Hybrid approach combining:
  - Distance Vector routing for basic path establishment
  - Link State updates for topology changes
  - Quality-based metrics (RSSI, latency, battery level)
- **Path Selection Criteria**:
  - Minimum hop count
  - Maximum signal strength
  - Lowest latency
  - Battery availability of intermediate Android devices

#### Voice Transport Protocol
- Real-time voice packet transmission using Android audio APIs
- Forward Error Correction (FEC) for packet loss recovery
- Adaptive bitrate based on link quality and Android system resources
- Jitter buffer management for smooth audio playback on Android devices

## Technical Specifications

### Bluetooth Standards
- **Primary**: Bluetooth 5.0+ (LE Audio support)
- **Fallback**: Bluetooth Classic (A2DP/HSP profiles)
- **Range**: 10-100 meters (depending on device class)

### Audio Specifications
- **Codec**: Opus (primary), AAC (fallback)
- **Sample Rate**: 16 kHz / 48 kHz
- **Bitrate**: 16-64 kbps (adaptive)
- **Latency Target**: <150ms end-to-end

### Network Topology
- **Maximum Hops**: 5 (configurable)
- **Maximum Network Size**: 50 devices
- **Discovery Interval**: 5-30 seconds (adaptive)
- **Route Update Interval**: 10-60 seconds

## Installation

### Prerequisites
- Android device with Bluetooth 5.0+ support
- Android 7.0+ (API level 24+)
- Target Android 14 (API level 34)
- Microphone and speaker capabilities

### Installation Steps
1. Open the `android/` directory in Android Studio
2. Sync Gradle dependencies
3. Connect an Android device with Bluetooth 5.0+ support
4. Build and install the application

### Required Permissions
The app requires the following Android permissions:
- `BLUETOOTH` and `BLUETOOTH_ADMIN` - Bluetooth communication
- `ACCESS_FINE_LOCATION` - Required for Bluetooth LE scanning
- `RECORD_AUDIO` - Voice capture
- `MODIFY_AUDIO_SETTINGS` - Audio configuration

## Usage

### Getting Started
1. Install the BVC app on your Android device
2. Grant required permissions (Bluetooth, Microphone, Location)
3. Open the app and allow device discovery
4. Your device will appear to other BVC-enabled devices nearby

### Making a Voice Call
1. Open the BVC app
2. Wait for device discovery to complete
3. Select a device from the available list
4. Tap "Call" to initiate voice communication
5. The receiving device will show an incoming call notification

### Emergency Mode
- Long press the emergency button for priority communication
- Emergency calls will override normal call routing for fastest connection
- Emergency mode increases transmission power and reduces discovery intervals

### Configuration
```yaml
# config.yml
audio:
  codec: "opus"
  sample_rate: 16000
  bitrate: 32000
  
network:
  discovery_interval: 10
  max_hops: 3
  route_update_interval: 30
  
bluetooth:
  device_name: "BVC-Device"
  transmit_power: "medium"
  scan_duration: 10
```

## Android API Reference

### Core Classes

#### `VoiceManager`
Manages voice call lifecycle and audio processing coordination.

```kotlin
class VoiceManager {
    fun startCall(targetDevice: String): CallSession
    fun answerCall(callId: String): CallSession
    fun endCall(callId: String): Boolean
}
```

#### `AndroidNetworkManager`
Handles device discovery and routing using Android Bluetooth APIs.

```kotlin
class AndroidNetworkManager {
    fun discoverDevices(timeout: Int): List<Device>
    fun findRoute(target: String): Route
    fun updateTopology(): NetworkTopology
}
```

#### `AndroidBluetoothAdapter`
Low-level Android Bluetooth LE communication interface.

```kotlin
class AndroidBluetoothAdapter {
    fun startAdvertising(data: ByteArray): Boolean
    fun startScanning(duration: Int): List<Advertisement>
    fun connect(device: Device): Connection
}
```

## Development

### Project Structure
```
bvc/
├── android/                # Android application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/bvc/
│   │   │   │   ├── core/       # Core voice management
│   │   │   │   ├── bluetooth/  # Bluetooth LE implementation
│   │   │   │   ├── network/    # Mesh networking
│   │   │   │   ├── audio/      # Android audio engine
│   │   │   │   ├── ui/         # User interface
│   │   │   │   └── utils/      # Utility functions
│   │   │   ├── res/            # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── build.gradle        # App build configuration
│   │   └── proguard-rules.pro  # Code obfuscation
│   ├── build.gradle            # Project build configuration
│   └── settings.gradle         # Gradle settings
├── docs/                       # Documentation
└── README.md                   # This file
```

### Building the Android App
```bash
# Open project in Android Studio
# File -> Open -> /path/to/bvc/android

# Or build from command line
cd android
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### Development Tools
- **Android Studio**: Primary IDE for development
- **Gradle**: Build system and dependency management
- **Android SDK**: Platform APIs and tools
- **Bluetooth LE Scanner**: For testing device discovery
## Performance Considerations

### Latency Optimization
- Minimize audio processing pipeline delay
- Optimize Bluetooth connection intervals
- Implement predictive routing for known network patterns

### Battery Efficiency
- Adaptive discovery intervals based on network stability
- Power-aware routing (avoid low-battery devices)
- Efficient audio encoding with quality/power trade-offs

### Scalability
- Hierarchical network organization for large deployments
- Load balancing across multiple routing paths
- Graceful degradation with network congestion

## Use Cases

- **Emergency Communication**: When cellular networks are unavailable
- **Event Coordination**: Temporary communication networks for events  
- **Remote Area Operations**: Communication in areas without infrastructure
- **Gaming and Social**: Local multiplayer gaming with voice chat
- **Industrial Applications**: Worker communication in facilities
- **Disaster Response**: Rapid deployment communication networks

## Android-Specific Limitations

- **Range**: Limited to Bluetooth transmission range and device density
- **Audio Quality**: Dependent on network conditions and Android device capabilities  
- **Battery Life**: Continuous Bluetooth operation affects device battery significantly
- **Compatibility**: Requires Android 7.0+ with Bluetooth 5.0+ capabilities
- **Background Restrictions**: Android's background processing limitations may affect performance

## Roadmap

### Phase 1 (Current)
- [x] Android project structure and core components
- [x] Bluetooth LE discovery and advertising
- [x] Basic audio capture and playback
- [ ] Voice call establishment and termination
- [ ] Multi-hop routing implementation

### Phase 2
- [ ] Audio codec integration (Opus/AAC)
- [ ] Advanced mesh networking optimizations
- [ ] Battery life optimization
- [ ] Background service implementation

### Phase 3
- [ ] iOS companion app
- [ ] Advanced UI/UX features
- [ ] Analytics and diagnostics
- [ ] Enterprise deployment features

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This project is under active development. APIs and features may change between versions. Please refer to the changelog for detailed version information.
