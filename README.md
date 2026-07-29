# وذكّر

وذكّر is a lightweight, offline Qur'an reminder app for Android. It provides
selected verses at times chosen by the user, keeps a local reminder archive,
and supports favorites, read-later lists, notes, and shareable verse images.

## Privacy

- No account is required.
- No ads, analytics, or tracking SDKs are included.
- No internet permission is requested.
- App preferences and reminder history remain on the device.

## Build from source

Requirements:

- JDK 17
- Android SDK 36

```bash
cd android
./gradlew assembleDebug
./gradlew assembleRelease
```

When `android/keystore.properties` is absent, the release task creates an
unsigned APK suitable for signing by F-Droid. Local Google Play builds can use
an ignored private keystore configuration.

## Font

The app packages Amiri 1.003 under the SIL Open Font License 1.1:

- `android/app/src/main/assets/fonts/amiri_regular.ttf`
- `android/app/src/main/assets/fonts/amiri_bold.ttf`
- `android/app/src/main/assets/fonts/OFL-Amiri.txt`

## License

Wazaker is licensed under the Apache License 2.0. See `LICENSE`.
