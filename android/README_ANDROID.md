# DebateGPT Android App

Kotlin Android UI for the DebateGPT backend. Built with **Jetpack Compose** and **Material 3**.

## Features

- **STT Debate Mode**: Record 10 seconds of audio, transcribe via backend Whisper, run analysis, and get the winner
- **Chatbot Debate Mode**: Debate with AI on a topic (favor/against), analyze the transcript, and get the winner

## Setup

### 1. Open in Android Studio

- **File → Open** → select the `android` folder
- Android Studio will detect the project and sync Gradle
- If the Gradle wrapper is missing: **File → Settings → Build → Gradle** → set Gradle to "Gradle wrapper" and click **OK**, then **File → Sync Project with Gradle Files**

### 2. Configure Backend URL

- **Emulator**: Use `http://10.0.2.2:8000` (10.0.2.2 = localhost on emulator)
- **Physical device**: Use your PC's LAN IP (e.g. `http://192.168.1.x:8000`)

Enter the URL on the home screen before choosing a mode.

### 3. Run Backend

Ensure the FastAPI backend is running:

```bash
cd ..   # from android folder
uvicorn api.main:app --reload --host 0.0.0.0
```

`--host 0.0.0.0` is required for the device/emulator to reach the backend.

### 4. Connection Troubleshooting

If you get **connection timeout** or **ETIMEDOUT**:

**Option A: Use ADB reverse (recommended for emulator)**
```bash
adb reverse tcp:8000 tcp:8000
```
Then in the app, use URL: `http://127.0.0.1:8000` or `http://localhost:8000`

**Option B: Physical device**
- Find your PC's IP: run `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
- Use `http://YOUR_PC_IP:8000` (e.g. `http://192.168.1.100:8000`)
- Phone and PC must be on the same Wi‑Fi

**Option C: Windows Firewall**
- Allow Python/uvicorn through firewall for port 8000
- Or temporarily disable firewall to test

**Test first:** Use the **Test Connection** button on the Home screen before starting a debate.

## Permissions

- **Internet**: API calls
- **Record audio**: STT mode (for recording speech)

## Project Structure

```
app/src/main/java/com/debategpt/app/
├── data/           # API client, models, Retrofit
├── ui/
│   ├── screens/    # Home, STT, Chatbot
│   ├── theme/      # Material 3 theme
│   └── viewmodel/  # ViewModels
├── util/           # AudioRecorder (WAV)
└── MainActivity.kt
```

## Build

```bash
./gradlew assembleDebug   # Unix/Mac
gradlew.bat assembleDebug # Windows
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
