<div align="center">
  <h1>SaveTube for Android</h1>
  <p>An open-source YouTube and YouTube Music client with recommendations that stay on your phone.</p>
  <p>
    <a href="https://goldendevuz.github.io/savetube-website/">Website</a> ·
    <a href="https://github.com/goldendevuz/flow-desktop">Windows app</a> ·
    <a href="https://github.com/goldendevuz/savetube-extension">Browser extension</a>
  </p>
</div>

---

SaveTube is a fork of [Flow for Android](https://github.com/Flow-Tube/Flow) by A-EDev, made for personal use. It keeps Flow's client and its on-device recommendation engine, and adds filters, more ways to sort subscriptions and settings that stay in sync with the Windows app. There is no paid version.

SaveTube uses YouTube's public data without an official API, so it can stop working when YouTube changes something. It is not affiliated with or endorsed by YouTube or Google.

## What it does

- Watches and downloads YouTube videos and plays YouTube Music, with SponsorBlock, DeArrow, synced lyrics, Picture in Picture and casting.
- Builds the Home feed on your device from what you watch, skip and search. No account, no tracking.
- **Home filters:** hide videos below a number of views, hide channels below a number of subscribers, hide music videos, and choose whether fresh uploads from your subscriptions are pinned.
- **Subscriptions** sort by name, recent upload, most watched, subscribers, video count and when you subscribed.
- **Regions:** Uzbekistan and Automatic send no country limit, and Uzbek view and subscriber counts are read correctly.
- **Sync:** history, likes, playlists, subscriptions and settings stay the same on phone and PC, through a WebDAV folder you own, end-to-end encrypted.

## Install

SaveTube installs next to the original Flow app and does not replace it. Every build is signed with the same key, so a new APK installs over the last one and keeps your data.

1. Open the [latest release](https://github.com/goldendevuz/savetube-android/releases/latest) and download the APK for your phone (`arm64-v8a` for most phones).
2. Open the file and allow installs from your browser or file manager when Android asks.

Builds come from [GitHub Actions](https://github.com/goldendevuz/savetube-android/actions/workflows/android-apk.yml) (Run workflow, then download the artifact).

## Build it yourself

```sh
./gradlew :app:assembleGithubDebug
```

You need JDK 21 and the Android SDK. Always use flavor-prefixed tasks (`assembleGithubDebug`, `compileFossDebugKotlin`), never the bare `assembleDebug`. A signed release build reads `release.keystore` and the `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables; `.github/workflows/android-apk.yml` does this with repository secrets.

## Acknowledgments

SaveTube stands on the shoulders of [Flow](https://github.com/Flow-Tube/Flow) and the projects Flow thanks:

- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) and [NewPipe](https://github.com/TeamNewPipe/NewPipe), for YouTube data extraction.
- [PipePipe](https://codeberg.org/NullPointerException/PipePipe), for its SABR and InnerTube playback implementation.
- [Metrolist](https://github.com/MetrolistGroup/Metrolist), for the hybrid music fetching, lyrics and icon references.
- [LibreTube](https://github.com/LibreTube/LibreTube), for SponsorBlock and DeArrow handling.
- [ExoPlayer](https://github.com/google/ExoPlayer), [Jetpack Compose](https://developer.android.com/jetpack/compose) and [Material Design 3](https://m3.material.io/).

If you like the app, consider supporting the original author: [Flow on Open Collective](https://opencollective.com/flow-tube).

## Licence

Original work: **Copyright © 2025–2026 A-EDev** ([Flow](https://github.com/Flow-Tube/Flow)). SaveTube's changes are released under the same licence.

SaveTube is free software under the **GNU GPL v3.0**, like Flow. Anyone who uses its source code, including the recommendation engine, must also publish it under GPL-3.0. See [License](License).
