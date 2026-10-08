# Technology options and recommendation

7 October 2026. Decision proposal, not a completed Android implementation.

The recommendation is **Kotlin + Jetpack Compose + Material 3, with an isolated on-device yt-dlp engine adapter**. Validate the Android engine integration before committing to it. Keep the project in this directory and preserve `app.py` as the legacy reference.

## UI and application framework

| Option | Fit for this project | Tradeoffs | Recommendation |
| --- | --- | --- | --- |
| Native Kotlin + Compose | Android-focused product with transfers, notifications, storage, and polished native screens | Requires rewriting the desktop UI and application logic | First choice |
| Flutter | Strong candidate if Android and iOS become equal requirements | Engine integration and Android background/storage behavior still need platform work; adds Dart to the stack | Second choice if cross-platform becomes a firm requirement |
| React Native | Potential fit for a team already invested in React/TypeScript | Native integration remains necessary for the download engine and platform lifecycle | No project-specific advantage established here |
| Kotlin with XML Views | Native Android with established view-based UI | Feasible, but there is no existing Android view code to preserve | Prefer Compose for this new project |
| Python mobile UI / embedded Python | Can retain some Python business logic or yt-dlp usage | CustomTkinter UI is not reusable as native Android UI; packaging and background integration still need work | Consider embedded Python only inside an engine, not as the UI architecture |
| Web/PWA or WebView shell | Useful for a companion site or server-driven product | Does not by itself solve long-running device downloads, native runtime packaging, or storage integration | Poor fit as the core architecture |

Compose is Android's recommended modern native UI toolkit and supports Material 3: [official documentation](https://developer.android.com/compose). The rankings above are project-specific engineering judgments, not benchmark results or claims that another framework cannot produce good apps.

## Media engine

| Option | Advantages | Costs and unresolved questions | Decision |
| --- | --- | --- | --- |
| yt-dlp through an Android wrapper | Closest fit to the old app; mature extraction and format-selection capabilities | Bundled runtimes, FFmpeg, binary size, CPU support, licensing, and upstream breakages must be managed | Preferred feasibility candidate |
| NewPipeExtractor | JVM-oriented extraction alternative that could avoid a Python engine layer | Need to verify current source coverage, license obligations, metadata/format parity, and the transfer/merge components we must implement | Main alternative to evaluate if the wrapper fails acceptance gates |
| Direct Python embedding, e.g. Chaquopy | More direct control over Python integration | Embedding alone does not solve yt-dlp's full dependency set or native executable constraints | Fallback integration strategy, not assumed simpler |
| Hosted yt-dlp backend | Centralized extraction updates and less engine packaging on the device | Hosting/bandwidth cost, privacy implications, abuse controls, server availability, and possible server-IP blocking | Avoid for the initial local-first app |
| Custom extractor | Full control | High ongoing maintenance burden as upstream behavior changes | Do not start here |
| Official YouTube APIs / embedded player | Useful for a different product centered on supported metadata and playback | Do not treat this as an established replacement for the old app's file-download engine | Not selected for feature parity |

Candidate projects: [youtubedl-android](https://github.com/yausername/youtubedl-android), [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), [Chaquopy documentation](https://chaquo.com/chaquopy/doc/current/). These are investigation links, not endorsements of a verified release. Additional source requests during this comparison failed; current alternative-engine release health and compatibility have not been confirmed.

Upstream yt-dlp documents FFmpeg/ffprobe for separate-stream merging and yt-dlp-ejs plus a supported JavaScript runtime for full YouTube support. The complete dependency combination must be proven on Android: [upstream dependency documentation](https://github.com/yt-dlp/yt-dlp#dependencies).

## Recommended supporting design

- Room stores queue state and history; DataStore stores preferences.
- ViewModels and coroutine/Flow state separate UI rendering from extraction and transfers.
- A replaceable engine adapter insulates screens and queue logic from extractor changes.
- Begin with one active download, with immutable jobs and explicit resolving/downloading/merging/publishing/completed states.
- Choose background execution per Android version and workload. User-initiated transfer jobs are available from Android 14; long-running WorkManager jobs can hit job quotas on Android 16. See [UIDT](https://developer.android.com/develop/background-work/background-tasks/uidt) and [WorkManager guidance](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running).
- Use app-controlled staging and MediaStore or a user-selected folder for final output. See [Android storage documentation](https://developer.android.com/training/data-storage/shared/media).
- Start with GitHub source and signed APK releases when ready; treat store distribution as a separate decision.

## Decision gate before full implementation

The stack recommendation becomes an engine commitment only after a physical-device prototype proves metadata extraction, actual quality selection, separate audio/video merge, cancellation, background behavior, output publication, and recovery from network/storage failure. Record APK size and supported OS/CPU combinations. Verify bundled licenses and native compatibility. A polished demo UI alone does not pass this gate.

Then implement the existing feature set, followed by persistent queue/history, share-to-app, thumbnails, batch controls, destination selection, retry/cancel, light/dark themes, and accessibility. Add audio-only, subtitles, advanced formats, scheduling, and verified resume support in later increments.

No technology can guarantee zero issues when extraction depends on an independently changing service. The best option for this project is the one we can test, maintain, recover, and update clearly.
