# Build and verification notes

1. Import the project into Android Studio.
2. Ensure JDK 17 is selected for Gradle.
3. Install Android SDK Platform 35 and Build Tools through SDK Manager.
4. Sync Gradle.
5. Run unit tests.
6. Build the debug APK.
7. Install on an Android device with a real call log.
8. Grant Call Log and Phone State permissions.
9. Run the exact seven-event numbering test from the specification.
10. Verify app restart, reboot, backup/restore, CSV/XLSX, search, stats, dialer, dark mode and Arabic RTL.

Because the current environment lacks Android SDK/build tools and cannot access external Maven/Google repositories, these final build/device steps cannot be executed here. No fabricated APK or test result is included.
