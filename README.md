# GameControl

Android keyboard & mouse mapper via OTG for mobile gaming.

Maps physical keyboard and mouse inputs to touch screen events, enabling PC-like controls on mobile games.

## Features

- **USB OTG Support** — Auto-detect keyboard & mouse via USB OTG
- **Key Mapping** — Map keys to tap, swipe, long press, and macro actions
- **Overlay UI** — Floating config window for in-game adjustments
- **Accessibility Service** — Touch event injection without root
- **Profile System** — Per-game mapping configurations (JSON-based)

## Tech Stack

- **Language:** Kotlin
- **Build:** Gradle (Kotlin DSL) + AGP 8.7
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35

## Architecture

```
Keyboard/Mouse (OTG)
        │
        ▼
  UsbDeviceManager (detect device)
        │
        ▼
  InputDevice API (read KeyEvent/MotionEvent)
        │
        ▼
  InputMapper (match key → action from config)
        │
        ▼
  GameControlService (AccessibilityService)
        │
        ▼
  dispatchGesture() → inject touch into game
```

## Setup

1. Enable **Accessibility Service** for GameControl in Settings
2. Allow **Display Over Other Apps** permission
3. Connect keyboard/mouse via OTG
4. Start the service and configure key mappings

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/`

## License

MIT
