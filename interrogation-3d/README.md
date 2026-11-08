# Interrogation 3D — Android prototype

A portrait-mode Android interrogation prototype with a stylized, animated suspect, Uzbek dialogue, evidence prompts, pressure/trust indicators, and simple sound cues.

## Build APK

The GitHub Actions workflow at `.github/workflows/interrogation-apk.yml` builds a debug APK whenever files in this project change.

1. Open the repository's **Actions** tab.
2. Select **Build Interrogation 3D APK**.
3. Open the latest successful run.
4. Download the artifact named **Interrogation3D-debug-apk**.
5. Extract the ZIP; the APK is `app-debug.apk`.

## Local build

Requires JDK 17 and Gradle 8.9:

```sh
cd interrogation-3d
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Current prototype

- Portrait Android layout.
- Custom-drawn interrogation room and suspect character.
- Breathing, reaction, and screen-shake animation.
- Uzbek Latin dialogue.
- Question, evidence, pressure, and physical-action buttons.
- No graphic gore.

This is an early playable 2D prototype, not a photorealistic 3D game. APK size depends on the build and is not yet guaranteed to be 50 MB.
