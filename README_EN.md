# QiTong AI Station

QiTong AI Station is an Android WebView and Jetpack Compose application for opening multiple AI websites, managing tabs and bookmarks, injecting messages, and chaining completed replies across several web-AI platforms.

## Multi-AI pipeline

The pipeline currently includes adapters for:

- Doubao
- Yuanbao
- Qwen / Tongyi
- DeepSeek
- Kimi

To run a pipeline:

1. Sign in to every selected service in its browser tab. The app does not bypass sign-in, CAPTCHA, account verification, or platform security controls.
2. Tap the pipeline icon in the bottom browser toolbar.
3. Enter the initial prompt, select platforms, and arrange their execution order.
4. The app creates or reuses one WebView tab per platform, waits for its input control, sends the current text, and waits for a new assistant response.
5. A response is forwarded only after generation has stopped and the assistant text remains stable across repeated polls.
6. A failed stage is retried once. A second failure stops later stages and displays the cause. A running pipeline can be cancelled.

The orchestration layer supports prompt templates with `{{input}}`, `{{original}}`, and `{{step}}`. The default UI forwards each complete reply unchanged into the next platform.

### Verification

The repository includes:

- JVM tests for ordering, output forwarding, templates, retry, cancellation, and rejection of stale callbacks.
- An Android instrumentation test using three real WebViews with different DOM structures. It performs the complete `Doubao ? Yuanbao ? DeepSeek` injection/extraction chain and verifies the final fixture output `S:Y:D:hello`.

External AI websites can change their DOM, login flow, availability, or anti-automation behaviour without notice. The app therefore treats a stage as successful only when it captures a new stable assistant reply; merely clicking Send is not reported as pipeline success. Selector updates may still be required when a provider changes its website.

## Windows build

This repository builds an **Android APK** on Windows; it is not a native Windows desktop application.

### Requirements

- Android Studio with Android SDK Platform 35
- JDK 17 or newer; Android Studio's bundled JDK 21 works
- PowerShell or Command Prompt

### PowerShell

```powershell
$env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

.\gradlew.bat assembleDebug
```

The APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Run unit tests with:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run the WebView instrumentation test on a connected emulator or device with:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Languages

The interface supports:

- Follow system language
- English
- Simplified Chinese
- Traditional Chinese (Taiwan)
- Traditional Chinese (Hong Kong)

Open **About ? Language** inside the app to switch languages. Android 13 and newer can also display the supported languages in the system's per-app language settings.

## Repository resolution

The project keeps Aliyun and Huawei Maven mirrors as fallbacks for networks where the official Google or Maven Central repositories are unavailable. Official repositories are preferred on ordinary Windows, macOS, and Linux systems.
