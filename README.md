# ARK Sticky Notes

| Folder | What it is |
|---|---|
| `ark.sticky.notes/` | ASP.NET Core web app behind https://sticky-notes.immanuel.co |
| `android-studio/StickyNotes/` | Android app (`ark.immanuel.stickynotes`), a WebView wrapper around `https://sticky-notes.immanuel.co/record` |

## Run the Android app on a phone (macOS, USB-C)

### Prerequisites

- **Android SDK** in `~/Library/Android/sdk` (Android Studio installs it). `adb` lives in
  `~/Library/Android/sdk/platform-tools/`.
- **JDK 21**: the one bundled with Android Studio. Gradle 8.14 / AGP 8.13 run on it.
- Optional: put `adb` on your PATH by adding this line to `~/.zshrc`:

  ```bash
  export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
  ```

  The commands below write the full path, so they work without it.

### 1. Turn on USB debugging on the phone (once per phone)

1. **Settings → About phone** → tap **Build number** 7 times until it says "You are now a developer".
   - Samsung: **About phone → Software information → Build number**.
   - Xiaomi: tap **MIUI version** / **OS version**.
2. **Settings → System → Developer options** → turn on **USB debugging**.
   - Samsung: Developer options is at the bottom of Settings.
   - Xiaomi: also turn on **USB debugging (Security settings)** and **Install via USB**.

### 2. Connect it to the Mac

1. Use a USB-C cable that carries **data**. Many charger cables carry power only.
2. Plug it **straight into the Mac**. Cheap USB-C hubs and dongles often pass power but not data. On
   such a hub, `system_profiler SPUSBHostDataType` lists the hub with nothing behind it.
3. Unlock the phone. If a USB notification appears, choose **File transfer**.
4. Accept **Allow USB debugging?** on the phone and tick **Always allow from this computer**.
5. Check it:

   ```bash
   ~/Library/Android/sdk/platform-tools/adb devices -l
   ```

   | Output | Meaning |
   |---|---|
   | `<serial>  device  model:...` | Ready. |
   | `<serial>  unauthorized` | The prompt was not accepted. Unlock the phone and accept it. If it never shows, tap **Revoke USB debugging authorizations** in Developer options, then replug. |
   | Empty list | The Mac sees no data connection: try another cable, another port, no hub. Then run `adb kill-server && adb start-server`. |

### 3. Build, install and run

From a terminal:

```bash
cd android-studio/StickyNotes
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew installDebug        # builds app-debug.apk and installs it on the phone
"$ANDROID_HOME/platform-tools/adb" shell am start -n ark.immanuel.stickynotes/.SplashActivity
```

The APK is written to `android-studio/StickyNotes/app/build/outputs/apk/debug/app-debug.apk`. You can also
install that file directly with `adb install -r <path>`.

From VS Code (open the repo root):

- **Terminal → Run Task… → Android: install & run on device** does the same as the commands above.
- **Run and Debug → Android: launch Sticky Notes** (F5) builds, installs, launches and attaches the
  debugger. It needs the *Android* extension (`adelphes.android-dev-ext`).
- **Android: logcat (app only)** streams the app's log while it runs.

If more than one device is attached (for example an emulator and the phone), choose one:
`adb -s <serial> ...` or `ANDROID_SERIAL=<serial> ./gradlew installDebug`.

### Inspect the web page inside the app

Debug builds allow WebView debugging. With the app open on the phone, open `chrome://inspect` in
Chrome on the Mac and click **inspect** under *ark.immanuel.stickynotes*. You get DevTools (console,
network, elements) for the page.

### Troubleshooting

- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**: the phone has the Play Store copy, which is signed with a
  different key. Run `adb uninstall ark.immanuel.stickynotes`, then install again. Uninstalling deletes
  the app's data on the phone, so you will need to sign in again.
- **Record / speech-to-text does nothing**: the app asks for microphone permission the first time you
  use it. If you denied it, allow it under **Settings → Apps → Sticky Notes → Permissions**.

### Without a cable: wireless debugging (Android 11+)

Phone and Mac must be on the same Wi-Fi.

1. **Developer options → Wireless debugging** → on → **Pair device with pairing code**.
2. On the Mac, using the IP, port and code the phone shows:

   ```bash
   ~/Library/Android/sdk/platform-tools/adb pair <ip>:<pairing-port>
   ~/Library/Android/sdk/platform-tools/adb connect <ip>:<port>   # port from the Wireless debugging screen
   ```

3. Continue with step 3 above.

### No phone? Use the emulator

`~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1`, or the VS Code task
**Android: start emulator**. Then use step 3 as usual.

### Release build for Google Play

Copy `android-studio/StickyNotes/keystore.properties.example` to `keystore.properties`, fill in the
upload-key details (the file is gitignored), then run `./gradlew bundleRelease`. The signed `.aab` is
written to `app/build/outputs/bundle/release/`.

With `keystore.properties` in place, `./gradlew assembleRelease` writes a signed
`app/build/outputs/apk/release/app-release.apk`. Without it, you get `app-release-unsigned.apk`, which you
can sign by hand as shown below.

### Prove package ownership (Android developer verification)

Google's Android Developer Console asks for a signed APK containing a per-account snippet before it
registers `ark.immanuel.stickynotes` to the account.

1. In the console, copy the snippet shown under **How to sign an APK**.
2. Save it, alone on one line, as `android-studio/StickyNotes/app/src/main/assets/adi-registration.properties`.
   The file name must match exactly. The file is gitignored, and
   `android-studio/StickyNotes/adi-registration.properties.example` describes it.
3. Build and sign a release APK. Use the key whose SHA-256 fingerprint you entered in the console. If
   that was the Play **app signing** key, Google holds it and you cannot sign with it, so register your
   upload key's fingerprint instead.

   ```bash
   cd android-studio/StickyNotes
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   export PATH="$JAVA_HOME/bin:$PATH"
   BT="$HOME/Library/Android/sdk/build-tools/36.1.0"
   OUT=app/build/outputs/apk/release

   ./gradlew assembleRelease
   "$BT/apksigner" sign --ks /path/to/upload-key.jks --ks-key-alias key \
     --out $OUT/app-release-signed.apk $OUT/app-release-unsigned.apk   # prompts for the password
   ```

4. Check the APK before uploading it. The snippet must be inside it, and the SHA-256 must match the
   fingerprint you gave the console. `apksigner` prints it in lowercase without colons.

   ```bash
   unzip -p $OUT/app-release-signed.apk assets/adi-registration.properties
   "$BT/apksigner" verify --print-certs $OUT/app-release-signed.apk | grep SHA-256
   ```

   The current key (alias `key`, `CN=Immanuel Raj, OU=Personal`) prints
   `2a024eebb05109ff4911566e9aea0f6de88acbe298da66346faea441c32e0164`.

5. Upload `app-release-signed.apk` in the console. Android checks the signature and registers the
   package.

The snippet is packaged into every build made while the file exists, including the Play bundle. That
does no harm.
