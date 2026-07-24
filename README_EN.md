# QiTong AI Station

QiTong AI Station is an Android WebView and Jetpack Compose application for opening multiple AI websites, managing tabs and bookmarks, and injecting messages into supported web chat interfaces.

## Windows build

This repository builds an **Android APK** on Windows; it is not a native Windows desktop application.

### Requirements

- Android Studio with Android SDK Platform 35
- JDK 17 or newer (Android Studio's bundled JDK 21 works)
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

## Languages

The interface supports:

- Follow system language
- English
- Simplified Chinese
- Traditional Chinese (Taiwan)
- Traditional Chinese (Hong Kong)

Open **About ? Language** inside the app to switch languages. Android 13 and newer can also display the supported languages in the system's per-app language settings.

## Notes

The project keeps Aliyun and Huawei Maven mirrors as fallbacks for networks where the official Google or Maven Central repositories are unavailable. Official repositories are preferred on ordinary Windows, macOS, and Linux systems.
