# Audio Visualizer

A modern Android audio visualization application built with Kotlin and AndroidX.

## Project Structure

```
audiovisualizer/
├── app/                              # Main app module
│   ├── build.gradle                  # App build configuration
│   ├── proguard-rules.pro           # ProGuard configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/audiovisualizer/
│   │   │   │   └── MainActivity.kt  # Main activity
│   │   │   ├── res/
│   │   │   │   ├── layout/          # Layout files
│   │   │   │   ├── values/          # String and color resources
│   │   │   │   └── mipmap/          # App icons
│   │   │   └── AndroidManifest.xml  # App manifest
│   │   ├── test/                    # Unit tests
│   │   └── androidTest/             # Instrumented tests
├── build.gradle                      # Root build configuration
├── settings.gradle                   # Project settings
├── gradle.properties                 # Gradle properties
└── README.md                         # This file
```

## Requirements

- Android SDK 21 (API Level 21) or higher
- Gradle 8.1.0 or higher
- Kotlin 1.9.0 or higher
- Java 8+

## Building

To build the project, run:

```bash
./gradlew build
```

To build and install on a connected device or emulator:

```bash
./gradlew installDebug
```

## Features

- Modern Material Design UI
- Kotlin-based implementation
- Audio recording capability
- AndroidX support

## Next Steps

1. Set up your audio visualization engine
2. Implement audio recording functionality
3. Create visualization components
4. Add audio processing logic
5. Test on various devices

## License

MIT License - See LICENSE file for details
