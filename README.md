# Customer Counter

A private, local-first Android app that turns unique phone numbers appearing in the Android call log into sequential customer numbers.

## Core behavior

- First unique phone number -> next customer number.
- Repeated phone number -> same customer number, update last call and increment count.
- Incoming, outgoing, missed and rejected call-log entries are handled when Android exposes them in the call log.
- Initial import orders calls chronologically, so the earliest occurrence of each unique number determines numbering.
- Phone matching uses Google's libphonenumber with Jordan (`JO`) as the default region, including local and international representations.
- Processed call-log IDs are stored to prevent double-counting.
- Room is the local source of truth.

## Architecture

UI (Jetpack Compose + Material 3) -> ViewModel -> Repository -> Room

Call-log integration is isolated under `calllog/` and uses:

- `PHONE_STATE` receiver to schedule a short post-call synchronization.
- `ContentObserver` while the app process is alive.
- WorkManager periodic reconciliation every 15 minutes.
- Boot receiver to schedule a reconciliation after device boot.
- An on-open synchronization as an additional safety net.

Android does not provide a universal unrestricted "call log changed" background callback for every device state. The implementation therefore combines these mechanisms rather than relying on a permanently running service.

## Permissions

- `READ_CALL_LOG`: required to read call history.
- `READ_PHONE_STATE`: used by the phone-state receiver to trigger synchronization after calls. The app does not record or upload call audio.
- `RECEIVE_BOOT_COMPLETED`: allows a reconciliation to be scheduled after reboot.

No contacts, location, camera, microphone, SMS, internet, advertising or analytics permissions are used.

## Backup and export

- JSON backup through Android's document picker.
- Restore through Android's document picker.
- UTF-8 CSV export.
- Lightweight `.xlsx` generation using Open XML parts written directly to a ZIP container, so an additional Excel SDK is not required.

## Build

Open this directory in Android Studio with an Android SDK that includes API 35. Let Android Studio download the Gradle/Android dependencies, then:

```text
Build > Make Project
Build > Build APK(s)
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK output:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

For a personal signed release, use Android Studio's `Build > Generate Signed Bundle / APK` and create/select a keystore.

## Important verification note

This project was generated as a complete Android Studio project, but the current execution environment does not contain the Android SDK, Gradle distribution, or Android build tools and has no external network access. Therefore an APK cannot honestly be claimed as compiled or device-tested from this environment. The source is structured for Android Studio/Gradle and the README intentionally does not label an unbuilt file as an APK.
