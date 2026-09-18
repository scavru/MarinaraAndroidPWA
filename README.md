# MarinaraAndroid

Android WebView wrapper for a configurable web application.

The app opens the configured web application as a standalone Android application, without a browser address bar.

## Features

- Full-screen Android WebView
- Server URL entered by the user
- HTTP Basic Authentication
- Username and password entered by the user
- Credentials stored locally on the Android device
- No server credentials hardcoded in the source code
- No default server URL
- JavaScript enabled
- DOM storage enabled
- Android Back button support
- Connection error screen
- Retry and Settings controls
- One-command Windows debug APK build

## Server configuration

**There is no default server address in this repository.**

On first launch, the user enters the complete server URL, for example:

```text
http://your-server:7777
```

The URL can be changed later from the application's settings.

No specific/private server address is embedded in the project.

## Requirements

The project was tested with:

- Windows 10/11
- Java 17
- Android SDK Platform 35
- Gradle 8.10.2
- Android Gradle Plugin 8.7.3

Android Studio is recommended for editing the project and managing the Android SDK.

## Build on Windows

Machine-specific paths are kept outside the repository.

### 1. Create `.env`

Copy:

```text
.env.example
```

to:

```text
.env
```

Edit the paths for your computer:

```env
JAVA_HOME=C:\Program Files\Java\jdk-17
ANDROID_SDK=C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk
GRADLE_HOME=D:\gradle-8.10.2
```

`.env` is ignored by Git, so local Windows paths are not uploaded to GitHub.

### 2. Build

Double-click:

```text
build-apk.bat
```

The resulting debug APK will be:

```text
app\build\outputs\apk\debug\app-debug.apk
```

A debug APK is sufficient for manual installation on an Android phone.

## Authentication and credentials

The application uses Android WebView's HTTP Basic Authentication callback.

On first launch, the user enters:

- Server URL
- Username
- Password

The credentials are saved locally using Android `SharedPreferences`.

They are not hardcoded in:

- `MainActivity.java`
- Gradle files
- `build-apk.bat`
- `.env.example`
- the Git repository

The `.env` file is only for build-machine paths. It does not contain application login credentials.

## HTTP security note

Cleartext HTTP support is enabled because the application is intended to support servers that use HTTP.

For production or Internet-facing use, HTTPS is recommended. HTTP Basic Authentication over plain HTTP does not provide transport encryption.

## GitHub safety

Do not commit:

- `.env`
- real passwords
- API keys
- private signing keys
- `local.properties`
- generated APK/AAB files

The included `.gitignore` already excludes these local/generated files.

## Project structure

```text
MarinaraAndroid/
├── app/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── build-apk.bat
├── .env.example
├── .gitignore
└── README.md
```

## License

No license is imposed by this repository. Add a `LICENSE` file if you want to distribute the project under a specific open-source license.
