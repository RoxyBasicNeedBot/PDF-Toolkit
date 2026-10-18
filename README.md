# Roxy PDF Toolkit 🚀

> **100% Offline-First, Private PDF Studio built with Compose Multiplatform & Apple-style Liquid Glass UI**

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.1-teal.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Privacy](https://img.shields.io/badge/Network_Permission-None_(0%20Data%20Collected)-brightgreen.svg)]()

---

## ✨ Features

- 🛡️ **100% Offline & Private:** Zero network permissions requested. Your documents never leave your device.
- 🪞 **Signature Liquid Glass UI:** Built with real-time GPU shaders (Kyant Backdrop) featuring optical lens refraction, chromatic aberration (RGB channel splitting), and touch-following glow.
- 🧭 **Floating Liquid Glass Dock:** iOS-inspired bottom navigation dock with spring-animated sliding selection indicator.
- 📑 **End-to-End PDF Merge:** Combine multi-page PDFs with custom drag-and-drop reordering and memory-efficient streaming.
- 📲 **System Intent Filters:** Open or share PDFs directly from WhatsApp, Telegram, Files, or Downloads into Roxy PDF Toolkit.
- ⚡ **3 Hardware Tiers:**
  - **Tier 3 (Android 13+):** Full AGSL RuntimeShader lens curvature and chromatic aberration.
  - **Tier 2 (Android 12):** RenderEffect hardware blur + vibrancy boost.
  - **Tier 1 (Android 11-):** 60fps translucent fallback for budget hardware.

---

## 🏛️ Architecture & Clean Modular Structure

Roxy PDF Toolkit follows a clean, modular architecture separating common UI from platform PDF engines:

```
roxy-pdf-toolkit/
├── androidApp/                                  # Android Application entry point
│   ├── src/main/AndroidManifest.xml             # Intent filters, zero internet permission
│   └── src/main/kotlin/com/roxy/pdftoolkit/
│       ├── RoxyApplication.kt                   # PDFBox & Koin initialization
│       └── RoxyMainActivity.kt                  # Edge-to-edge Compose host & SAF handlers
│
└── composeApp/                                  # Shared Multiplatform module
    ├── commonMain/                              # 100% Platform-Agnostic UI & Engine Interface
    │   └── kotlin/com/roxy/pdftoolkit/
    │       ├── ui/
    │       │   ├── theme/                       # RoxyTheme, RoxyColor, RoxyType, RoxyMeshBackground
    │       │   ├── glass/                       # RoxyLiquidGlass, RoxyTier, RoxyGlassInteraction, RoxyGlassCard
    │       │   ├── navigation/                  # RoxyGlassNavBar, RoxyNavDestination
    │       │   ├── scaffold/                    # RoxyToolScaffold, RoxyToolHeader
    │       │   ├── home/                        # RoxyHomeScreen, RoxyToolCard
    │       │   └── tools/merge/                 # RoxyMergeToolScreen, RoxyMergeViewModel, RoxyMergeState
    │       ├── engine/                          # RoxyPdfEngine interface, RoxyPdfDocument, RoxyOperationResult
    │       └── di/                              # RoxyCommonModule (Koin)
    │
    └── androidMain/                             # Android Platform Implementations
        └── kotlin/com/roxy/pdftoolkit/
            ├── engine/                          # RoxyAndroidPdfEngine, RoxyPdfRendererWrapper
            ├── saf/                             # RoxySafFilePicker, RoxySafFileSaver
            ├── tier/                            # RoxyDeviceTierDetector
            └── di/                              # RoxyAndroidModule (Koin)
```

---

## 🚀 Building & Running

1. Open project in **Android Studio Meerkat / Ladybug** (or IntelliJ IDEA).
2. Sync Gradle dependencies.
3. Run `:androidApp` on an Android device or emulator (Android 8.0+ / API 26+).

---

## 📄 License

Licensed under the **Apache License 2.0**.
