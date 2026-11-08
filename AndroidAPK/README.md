# So‘roq 2D — Android APK

This folder wraps the current HTML5 Canvas game in a small native Android WebView application.

## Build

The GitHub Actions workflow **Build So'roq 2D APK** runs on pushes affecting this project and can also be started manually from the Actions tab using **Run workflow**.

On success, download the artifact named **Soroq2D-debug-apk**. It contains `app-debug.apk`.

## App details

- Package: `uz.soroq.interrogation`
- Orientation: portrait
- Minimum Android version: Android 6.0 (API 23)
- Game files are copied from `Interrogation2D/index.html` during CI.

This is an unsigned debug APK for testing, not a Play Store release. The first build must complete successfully in GitHub Actions before an installable APK can be claimed.
