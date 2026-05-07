# 🚗 Drift Park — LibGDX + Kotlin Android Game
## Complete Setup & Development Guide

---

## PART 1: INSTALLATION

### Step 1 — Java Development Kit (JDK)

LibGDX requires **JDK 11 or 17** (17 recommended).

**Windows:**
1. Go to https://adoptium.net
2. Download **Temurin 17 (LTS)** — choose Windows x64 `.msi`
3. Run the installer. Check "Set JAVA_HOME variable" and "Add to PATH"
4. Verify: open Command Prompt → `java -version`
   Expected: `openjdk version "17.x.x"`

**macOS:**
```bash
brew install --cask temurin17
java -version   # verify
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update && sudo apt install openjdk-17-jdk
java -version   # verify
```

---

### Step 2 — Android Studio

1. Go to https://developer.android.com/studio
2. Download and install the latest stable version
3. On first launch, let it download the Android SDK (this takes a few minutes)
4. **SDK location** will be set automatically, usually:
   - Windows: `C:\Users\YourName\AppData\Local\Android\Sdk`
   - macOS: `~/Library/Android/sdk`
   - Linux: `~/Android/Sdk`

**Install required SDK components:**
1. Open Android Studio → `Tools > SDK Manager`
2. Under **SDK Platforms**, check: `Android 14.0 (API 34)`
3. Under **SDK Tools**, check:
   - `Android SDK Build-Tools 34`
   - `Android Emulator`
   - `Android SDK Platform-Tools`
4. Click Apply → OK

---

### Step 3 — Set ANDROID_HOME environment variable

**Windows:**
```
System Properties > Environment Variables > New System Variable:
  Name:  ANDROID_HOME
  Value: C:\Users\YourName\AppData\Local\Android\Sdk
```
Also add to PATH: `%ANDROID_HOME%\platform-tools`

**macOS/Linux** (add to `~/.bashrc` or `~/.zshrc`):
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```
Then: `source ~/.bashrc`

---

### Step 4 — Install Kotlin Plugin in Android Studio

Android Studio usually ships with Kotlin built-in. To verify/update:
`File > Settings > Plugins > search "Kotlin" > check it's installed`

---

### Step 5 — LibGDX (no separate install needed!)

LibGDX is pulled in as a **Gradle dependency** — no separate download.
It's declared in `build.gradle` with: `gdxVersion = '1.12.1'`
Gradle downloads it automatically when you build.

> 💡 **Optional:** Use the LibGDX Project Generator for future projects:
> https://libgdx.com/wiki/start/project-generation

---

## PART 2: PROJECT STRUCTURE

```
ParkingGame/                          ← Project root
│
├── build.gradle                      ← Root Gradle config (versions, repos)
├── settings.gradle                   ← Module list: :core, :android
│
├── core/                             ← ALL game logic (platform-independent)
│   ├── build.gradle
│   └── src/main/kotlin/com/parkinggame/
│       ├── ParkingGame.kt            ← Main Game class (entry point)
│       ├── MenuScreen.kt             ← Main menu
│       ├── GameScreen.kt             ← Gameplay screen (render loop)
│       ├── Car.kt                    ← Car physics
│       ├── TouchInputHandler.kt      ← Joystick + brake input
│       └── LevelData.kt              ← All 5 levels + obstacles + parking spots
│
├── android/                          ← Android wrapper (thin layer)
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/parkinggame/android/
│       │   └── AndroidLauncher.kt    ← Android Activity entry point
│       └── res/
│           ├── mipmap-hdpi/
│           │   └── ic_launcher.png   ← App icon (create these)
│           └── values/
│               └── strings.xml       ← App name string resource
│
└── assets/                           ← Shared game assets (fonts, sounds, textures)
    └── (empty for now — shapes only)
```

**The golden rule:** All game logic goes in `core/`. Android-specific code only goes in `android/`. This makes your game portable to desktop too.

---

## PART 3: OPENING IN ANDROID STUDIO

1. Open Android Studio
2. `File > Open` → navigate to the `ParkingGame/` folder → click OK
3. Android Studio detects it as a Gradle project and imports it
4. Wait for **Gradle sync** to complete (first time downloads LibGDX — ~2 minutes)
5. If asked about JDK: select your JDK 17 installation

**If Gradle sync fails:**
- Check your internet connection (needs to download dependencies)
- `File > Invalidate Caches > Invalidate and Restart`
- Make sure JDK 17 is set: `File > Project Structure > SDK Location > JDK Location`

---

## PART 4: CREATE AN ANDROID EMULATOR

1. In Android Studio: `Tools > Device Manager`
2. Click `+ Create Device`
3. Choose **Pixel 7** (or similar) → Next
4. System Image: select **API 34, x86_64** → Download if needed → Next
5. Name it "Pixel7_API34" → Finish
6. Click the ▶ (Play) button next to the device to start it
7. Wait ~30 seconds for it to boot (first boot is slow)

**Emulator tips:**
- Portrait orientation is set in the manifest — the emulator will match
- For better performance: enable Hardware acceleration in emulator settings
- Virtual sensors for tilt are available but we're using touch input

---

## PART 5: RUNNING ON THE EMULATOR

**From Android Studio:**
1. Make sure your emulator is running
2. In the toolbar: select `android` from the run configuration dropdown
3. Select your emulator as the target device
4. Click ▶ Run (or Shift+F10)
5. Gradle builds the APK and installs it automatically
6. The game launches on the emulator

**From the command line:**
```bash
cd ParkingGame
./gradlew android:installDebug          # Build + install debug APK
adb shell am start -n com.parkinggame/.android.AndroidLauncher   # Launch
```

---

## PART 6: RUNNING ON A REAL ANDROID DEVICE

1. On your phone: `Settings > About Phone` → tap **Build Number** 7 times → enables Developer Options
2. `Settings > Developer Options` → enable **USB Debugging**
3. Connect phone via USB cable
4. Accept the "Allow USB Debugging?" prompt on the phone
5. In Android Studio, your phone appears in the device dropdown
6. Click ▶ Run — it builds and deploys to your phone

---

## PART 7: BUILDING A RELEASE APK

### Step 7a — Create a Keystore (one-time setup)

A keystore is your app's signing identity. Keep it safe — you need the same keystore to update your app.

```bash
keytool -genkey -v \
  -keystore parking-game-key.jks \
  -alias parkinggame \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Follow the prompts. Store `parking-game-key.jks` somewhere safe (NOT inside the project folder for security).

### Step 7b — Configure signing in android/build.gradle

Uncomment and fill in the `signingConfigs.release` block:
```groovy
signingConfigs {
    release {
        storeFile file("/path/to/parking-game-key.jks")
        storePassword "yourStorePassword"
        keyAlias "parkinggame"
        keyPassword "yourKeyPassword"
    }
}
```

Also uncomment: `signingConfig signingConfigs.release` in `buildTypes.release`.

### Step 7c — Build the release APK

```bash
./gradlew android:assembleRelease
```

Output: `android/build/outputs/apk/release/android-release.apk`

### Step 7d — Build an AAB (for Google Play Store)

```bash
./gradlew android:bundleRelease
```

Output: `android/build/outputs/bundle/release/android-release.aab`

---

## PART 8: REQUIRED RESOURCE FILES

Create these files before building:

**android/src/main/res/values/strings.xml:**
```xml
<resources>
    <string name="app_name">Drift Park</string>
</resources>
```

**App icons** — create PNG files at these paths:
- `android/src/main/res/mipmap-mdpi/ic_launcher.png`     (48×48)
- `android/src/main/res/mipmap-hdpi/ic_launcher.png`     (72×72)
- `android/src/main/res/mipmap-xhdpi/ic_launcher.png`    (96×96)
- `android/src/main/res/mipmap-xxhdpi/ic_launcher.png`   (144×144)
- `android/src/main/res/mipmap-xxxhdpi/ic_launcher.png`  (192×192)

For development, a simple red square works fine. For release, use a proper icon.

---

## PART 9: GAME CONTROLS (REMINDER)

```
┌─────────────────────────────────────┐
│                                     │
│  LEFT HALF        RIGHT HALF        │
│                                     │
│  [BRAKE]          [JOYSTICK]        │
│  Tap/hold         Touch & drag      │
│  to brake         Drag LEFT  = turn RIGHT
│                   Drag RIGHT = turn LEFT
│                                     │
│  Car always moves forward           │
│  Drift into the yellow parking spot │
│                                     │
└─────────────────────────────────────┘
```

---

## PART 10: NEXT STEPS & CUSTOMIZATION

### Tweaking physics (in Car.kt):
- `MIN_SPEED` — raise to make car always go faster
- `MAX_SPEED` — lower for tighter control
- `BRAKE_DECEL` — higher = harder braking
- `STEER_RATE` — higher = more responsive turning
- `DRIFT_FACTOR` — higher = more sliding (0.9 = very slidey, 0.7 = tighter)
- `DRIFT_THRESHOLD` — speed at which drifting starts

### Adding sounds:
1. Add `.ogg` files to `assets/`
2. In `ParkingGame.kt`: `val music = Gdx.audio.newMusic(Gdx.files.internal("engine.ogg"))`

### Adding textures (sprites):
1. Add `.png` files to `assets/`
2. Use `Texture` and `Sprite` classes instead of `ShapeRenderer`

### Adding more levels:
In `LevelData.kt`, add `level6()` and update the `get()` when block.

---

## COMMON ERRORS & FIXES

| Error | Fix |
|-------|-----|
| `SDK location not found` | Set `ANDROID_HOME` env var or create `local.properties` with `sdk.dir=/path/to/sdk` |
| `Minimum supported Gradle version` | Update gradle wrapper: `./gradlew wrapper --gradle-version 8.x` |
| `Could not resolve com.badlogicgames.gdx` | Check internet; try `./gradlew --refresh-dependencies` |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Clear emulator storage in Device Manager |
| App crashes on start | Check Logcat in Android Studio for the actual error |
| Black screen on emulator | Emulator needs x86 natives — check android/build.gradle has x86/x86_64 natives |

---

*Happy parking! 🚗💨*
