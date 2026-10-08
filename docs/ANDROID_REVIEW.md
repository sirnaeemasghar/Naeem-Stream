# Android app: source review and build plan

Reviewed 7 October 2026. Source: all 532 lines of `app.py`.

This is a static review, not a claim that the desktop application or live downloads have been tested. The original Python file is preserved. This folder currently contains no Android project, dependency manifest, tests, or Git repository. No review can guarantee zero defects; the goal is explicit failure handling, recoverable operations, and verified releases.

## Existing features to preserve

| Existing behavior | Android equivalent |
| --- | --- |
| Paste a video, playlist, or channel URL | Paste field plus Android share-to-app entry point |
| Fetch titles and list collection entries | Metadata preview and pageable collection list |
| Select individual collection videos | Multi-select with select all/none and selection count |
| Choose quality per selected video | Per-item format choice plus a batch default |
| Single-video quality selection | Same unified format picker used for collections |
| Choices from 144p through 1080p when detected | Actual available formats, including higher resolutions when supported |
| Sequential batch downloads | Persistent queue, initially one active transfer |
| Download video and audio, request MP4 merge | Verified merge with compatible codecs and clear output choices |
| Filename, bytes, speed, ETA, progress | Per-item and batch status in app and notifications |
| Retrieval errors, selection warning, completion message | Inline errors, retry actions, truthful result summary |
| Dark interface and scrollable results | Accessible light/dark native UI with adaptive layouts |
| Email, WhatsApp, Facebook, creator credit | About/support screen with appropriate Android intents |

## Defects and limitations in the source

Line references apply to the unchanged original file. Critical/high findings concern correctness or data integrity; medium findings concern usability, maintainability, or missing resilience.

| Severity | Location | Finding and consequence | Required change |
| --- | --- | --- | --- |
| High | 134, 210 | Format extraction executes in UI callbacks. Network delays freeze interaction. | Run extraction off the main thread; expose loading, error, and cancellation states. |
| High | 394–395, 413, 439, 462–528 | Worker threads read Tk state, mutate widgets, and show dialogs. Behavior depends on Tk threading/event-loop state and can fail during shutdown. | Workers emit data; only UI code renders it. |
| High | 349–359, 384–408 | Repeated fetches share mutable results and progress-widget references. An older request can overwrite a newer result or update a destroyed widget. | Cancel superseded requests and ignore stale results using request IDs. |
| High | 349–355, 442–466 | Downloads retain list indices, then read mutable `self.videos` and quality state. A new fetch can change the meaning of an index or make it invalid. | Snapshot immutable jobs containing source ID, canonical URL, and chosen format. |
| High | 452–454, 462–495 | Repeated download taps create independent threads but overwrite shared progress references. Jobs can compete for files and update the wrong window. | One queue owner, deduplicated enqueue operations, stable job IDs. |
| High | 127, 143, 216 | A successful extraction with no recognized formats returns an empty list; `qualities[-1]` raises an exception. | Represent no usable formats explicitly; disable enqueue and provide explanation. |
| High | 128–130 | Extraction failure invents a 720p choice. The interface claims availability that was never established. | Preserve the actual error and offer retry. |
| High | 471, 481, 494 | `ignoreerrors=True`, ignored download return status, and unconditional completion can report success after failure. Even caught failures still lead to “All downloads completed.” | Record success/failure per job; verify final output and summarize actual results. |
| High | 469, 473 | Separate video/audio streams require merging support; FFmpeg availability is never checked. MP4 selection does not itself guarantee codec compatibility or universal playback. | Validate engine capabilities and compatible format selection before enqueueing. |
| High | 457–458 | Windows-specific destination is hardcoded. On macOS the string is a relative filename containing backslashes, not the intended Windows absolute path. Directory creation is outside the try block. | User-visible Android destination with checked space, access, and write failures. |
| High | 470 | Title-only filenames collide across videos with the same title, potentially causing a wrong skip or file conflict. | Include stable video IDs and define duplicate handling. |
| High | 233–237, 358, 453 | Closing progress or the main window does not cancel/join work. Threads can keep running and address destroyed UI. | Separate job lifetime from screens; explicit cancel and shutdown handling. |
| Medium | 115–124 | Exact `format_note` matching is brittle and excludes formats with other labels, 1440p/2160p, and richer frame-rate/HDR/audio choices. | Model numeric dimensions, codecs, FPS, audio tracks, and format IDs. |
| Medium | 469 | A chosen resolution means an upper bound, not an exact resolution. Actual downloads can be lower than the displayed choice. | Label “up to” presets accurately or select a specific supported format with an explicit fallback policy. |
| Medium | 119–120, 134, 210 | Repeated metadata extraction; toggling selection can repeatedly fetch the same video. | Bounded metadata cache with refresh and expiry; resolve temporary media URLs at execution time. |
| Medium | 384–396 | Fake progress starts after retrieval and adds roughly `1.02 × max(3, fetch_time)` seconds. A 20-second fetch appears to take about 40 seconds. | Indeterminate loading until measurable progress exists; no artificial delay. |
| Medium | 418, 422–437 | `None` results and nested unavailable entries are not consistently checked. Broad exceptions convert malformed data into an empty result. | Parse defensively; distinguish empty collections, unavailable items, and failed retrieval. |
| Medium | 427–433 | Nested entries with a title and URL are accepted before checking children; a collection node can be mistaken for a video. Only one extra nesting level is traversed. | Normalize extractor result types and deliberately support collection/tab semantics. |
| Medium | 412–440 | No URL validation, canonicalization, or explicit decision for watch URLs containing playlist parameters. The engine can accept more sites than the UI advertises. | Parse supported HTTP(S) hosts and source types; offer video-versus-playlist choice when ambiguous. |
| Medium | 425–434, 175–196 | Entire collections are accumulated and rendered as individual widgets. Large channels are slow and memory-heavy. | Incremental discovery, virtualized lists, bounded requests, and cancellation. |
| Medium | 349–355, 215 | Old videos and single-video quality are not consistently cleared while refreshing. Failed lookup destroys the prior visible list. | Explicit screen state and atomic result replacement; keep the last valid result where helpful. |
| Medium | 464–466 | Tk variables are constructed inside worker code, including eagerly evaluated fallback expressions. Download logic depends directly on UI objects. | Pass plain immutable settings into the engine. |
| Medium | 472 | A five-minute socket timeout plus library defaults offers poor visible recovery. No app-level retry classification or cancellation policy exists. | Bounded retry with backoff for transient failures; actionable permanent errors. |
| Medium | 497–528 | Progress describes individual transfer phases, not an entire video+audio+merge job. `finished` occurs before all post-processing is necessarily complete. | Separate downloading, merging, publishing, and completed states. |
| Medium | 500–520 | Unknown totals display as zero, estimates can change, percent is not clamped, missing ETA defaults to zero, and float ETA can fail integer formatting. | Nullable totals/ETA, validated numeric values, throttled updates, indeterminate progress where appropriate. |
| Medium | 489–495 | Results disappear after two seconds; there is no history, queue position, batch count, or open-file action. | Persistent history and user-controlled result screen. |
| Medium | Entire file | No durable queue, app-level pause/resume, cancellation, network preferences, or restart recovery. yt-dlp may resume partial files by default, but the app does not manage recovery. | Persist job state and partial artifacts; distinguish resumable transfers from required restarts. |
| Medium | Entire file | No explicit disk-space checks, staging cleanup, destination permission recovery, or output validation. | Account for temporary streams plus merge output; publish only validated files. |
| Medium | 23–24, UI throughout | Fixed sizes, hardcoded strings/colors, horizontal quality controls, and desktop dialogs do not address mobile layouts, large text, screen readers, or localization. | Resource strings, scalable layout, semantic labels, adequate contrast and touch targets. |
| Medium | 10–531 | One class combines UI, extraction, storage, scheduling, and progress. Constructor starts the event loop, making isolated testing difficult. | Separate presentation, domain models, queue repository, storage, and engine adapter. |
| Medium | Repository | No pinned environment, dependency inventory, setup guide, tests, release process, or license. | Reproducible build, documented support matrix, automated checks, license and notices. |

## Recommended Android foundation

Use Kotlin, Jetpack Compose, and Material 3 for a native Android Studio project. Compose is Android's recommended modern native UI toolkit: [Android documentation](https://developer.android.com/compose).

Use ViewModels and coroutine/Flow state for screens; Room for durable jobs/history; DataStore for preferences. Keep the initial codebase modest, with clear package boundaries rather than premature multi-module complexity.

Put extraction behind a `MediaEngine` interface: inspect a source, discover collection entries, resolve formats, execute a job, cancel, and report structured events. The initial candidate is an Android yt-dlp integration such as [youtubedl-android](https://github.com/yausername/youtubedl-android). This is a candidate, not a verified selection. Before committing, prove current YouTube support, packaged runtime compatibility, Android CPU architectures, native-library/page-size support, cancellation, licensing, and acceptable APK size on a real phone.

Current upstream yt-dlp documentation lists FFmpeg/ffprobe for separate-stream merging, and yt-dlp-ejs plus a supported JavaScript runtime for full YouTube support. Copying the old Python import into Android is insufficient. See [yt-dlp dependencies](https://github.com/yt-dlp/yt-dlp#dependencies). Evaluate the exact bundled dependency combination and keep engine updates replaceable and verifiable. Do not assume a desktop executable will run on Android.

Prefer on-device processing for the initial product: no required account or hosted download server. A server alternative would introduce operating costs, availability dependencies, and transmission of users' URLs; there is currently no requirement justifying it.

Use user-initiated data transfer jobs where appropriate on Android 14+, with a version-aware fallback chosen during the prototype. WorkManager suits deferrable/interruptible work; it should not be assumed to provide unlimited downloads. Android 16 introduces job-quota concerns for long-running workers. Sources: [UIDT jobs](https://developer.android.com/develop/background-work/background-tasks/uidt), [long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running).

Stage downloads in app-controlled storage, verify merge results, then publish through MediaStore or a user-selected document destination. Handle revoked access and failed publication separately from successful transfer. See [shared media storage](https://developer.android.com/training/data-storage/shared/media).

Model job transitions explicitly: queued → resolving → downloading → merging → publishing → completed, with failed/cancelled and supported pause/wait/retry paths. Completion requires successful engine execution and a valid accessible output, not a progress callback alone. Recover interrupted jobs after process death without claiming that Android force-stop permits uninterrupted background execution.

## Product improvements

First release: retain every existing capability, and add share-to-app, thumbnails/duration, batch selection/default quality, persistent queue/history, cancel/retry, destination choice, duplicate detection, useful notifications, and light/dark accessible design. Show unavailable collection entries with a reason or count rather than silently losing them.

Later candidates: audio-only formats, subtitles and language selection, higher resolutions, FPS/HDR choices, Wi-Fi-only transfers, queue reordering, scheduling, configurable concurrency, and export/import of queue metadata. Pause/resume must be advertised only for combinations we have verified; signed URLs and segmented sources may require re-resolution or restart. Audio conversion is distinct from extracting an existing audio stream and has different CPU/battery costs.

Suggested navigation: Home (paste/share and preview), Downloads (queue and history), Settings; put About/support inside Settings. Use a clear format sheet and an explicit confirmation showing count, destination, and estimated size when available. “Professional” includes clear empty, offline, error, and recovery states—not only polished colors.

## Implementation sequence and acceptance gates

1. **Engine feasibility:** Inspect the installed SDK/JDK, choose compatible pinned versions, create a minimal Android project in this directory, and test a permitted single-video transfer, audio/video merge, cancellation, storage publication, and screen-off behavior on a physical device. Measure binary size and runtime dependencies before choosing the permanent engine.
2. **Reliable vertical slice:** Implement paste/share → metadata → format → persistent job → notification → verified output → open/share. A failed merge must never create a completed history item.
3. **Full feature parity:** Add playlists/channels, incremental listing, per-item quality, batch selection and progress, all existing support links and creator credit, plus recovery and failure summaries. Verify channel tab behavior explicitly.
4. **Design and expansion:** Polish the visual system and accessibility; add the prioritized optional features against working engine capabilities.
5. **Public release:** Prepare README, screenshots, setup instructions, contribution guide, chosen source license, dependency notices, automated build checks, and versioned signed APK releases. Keep release signing keys, credentials, local SDK paths, and user data out of Git. Confirm public-facing contact details before publication. GitHub publication is a later step requested by the owner, not performed during this review.

## Verification matrix

- Formats: no formats, audio-only, no 720p, 60 FPS, portrait, HDR, higher resolutions, incompatible containers/codecs, and disappearing formats.
- Sources: single video, short URL, Shorts, watch URL with playlist, empty/large playlist, unavailable/private entries, channel tabs, malformed input, and unsupported host.
- Concurrency: repeated taps, rapid source changes, cancellation during lookup/download/merge/publication, duplicate enqueue, and stale callback arrival.
- Recovery: offline start, connection drop, Wi-Fi/mobile transition, throttling, expired media URLs, app backgrounding, rotation, process death, reboot, and explicit force-stop/reopen.
- Storage: low space before and during merge, duplicate titles, Unicode/long names, missing output, revoked folder access, and failed final publication.
- UI: unknown size/ETA, estimated totals changing, partial batch failure, large fonts, TalkBack, tablet/foldable layouts, and light/dark contrast.
- Release: clean checkout build, supported API/CPU combinations, native dependency compatibility, signed APK installation/update, and absence of secrets in artifacts.

Use deterministic engine fakes for queue/state tests and permitted live media for integration checks. Live YouTube availability is an external dependency and should not be the only test oracle.

## Remaining decisions and boundaries

App name, application ID, minimum Android version, license, and optional-feature priorities remain open. They need not block the feasibility experiment; any temporary values should be labeled provisional.

YouTube can change extraction behavior independently of this app. Reliability therefore includes a clear unsupported/broken-source state and a maintained engine update path. Downloads should respect rights and service permissions; [YouTube's terms](https://www.youtube.com/static?template=terms) restrict downloading except as authorized by the service or with the required permissions. GitHub availability is distinct from Google Play acceptance, which has not been evaluated here.
