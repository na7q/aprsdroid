# CLAUDE.md — working memory for this repo

NA7Q fork of APRSdroid (Android, **Scala 2.11**, GPLv2). Full detail is in `SPEC.md`; this file is the short version to load before working.

## Orientation
- Package `org.aprsdroid.app`. Sources are in `src/` (not `src/main`), resources in `res/`, `AndroidManifest.xml` at the root.
- Entry point is `src/AprsService.scala`. It is a foreground service that drives the backend, the location source, messaging, the digipeater and the IGate.
- Backends are `src/backend/*`, selected by prefs `proto` + `link` in `AprsBackend.scala`. Wire protocols are `src/tncproto/*`.
- Prefs go through `PrefsWrapper`. Preference screens are `res/xml/*.xml` plus the `*Prefs.scala` activities.
- DB is `StorageDatabase.scala` (SQLite v4: posts, stations, positions, messages).
- Prebuilt libs live in `libs/` and `lib/`. `PacketDroid/` is a git submodule (run `git submodule update --init`).

## Data flow (memorize)
- **TX:** location source -> `AprsService.postLocation` -> `formatLoc` or `formatLocMice` -> `sendPacket` (Future) -> `poster.update` -> `addPost` (log).
- **RX:** backend -> `service.postSubmit(line)` -> `addPost(TYPE_INCMG)` -> `parsePacket` (stations/messages), `parseHudPackets`, `DigipeaterService.processIncomingPost`, `IgateService.handlePostSubmitData`.
- Digipeating and IGating only run for KISS/AFSK backends. That is decided by `getBackendName().contains(...)`, which is fragile (localized names).

## Conventions and gotchas
- Mixed tabs and spaces. Match the surrounding file's style. Do not reformat whole files.
- Prefs keys use two styles: `p.something` (newer NA7Q features) and plain names (upstream). Some numeric prefs are stored as strings; use `getStringInt` and `getStringFloat`.
- Backend threads should report to the service through `postAddPost`/`postSubmit`/`postAbort`, which hop to the UI `Handler`. `IgateService` bypasses this and calls `addPost` directly.
- `service_running` pref is the persisted "should be running" flag. The boot receiver and the IGate both read it.
- Digipeater and IGate read their prefs at different times: the digipeater reads live, the IGate reads at construction. A pref change needs a restart for the IGate.
- Do not log secrets. The code logs full packets and locations with `Log.d`.
- Never auto-transmit in tests or demos. This app keys real radios under a real callsign.

## Known issues (found in review, **not fixed**; see SPEC.md §6)
1. `SmartBeaconing.smartBeaconSpeedRate` converts an already-m/s speed again.
2. `MessageService`: with the ack-dupe toggle on and `p.ackdupe` = 0, no ACK is ever sent.
3. Backend detection via localized name substring (KISS/AFSK/Bluetooth).
4. IGate reconnect blocks the socket thread, is unsynchronized, and snapshots prefs.
5. `modifyData` matches `RFONLY`/`TCPIP`/`NOGATE` as substrings of the whole packet.
6. `AprsService` is exported with no permission, so any app can cause an RF TX. `HUD` and `POSITION` broadcasts are unrestricted. TLS trust is naive (accept-all).
7. `KissProto.freqConvert` doesn't escape `FESC`.

## Build and test
- Not verified in this environment. Expected: Java 8, Android SDK (compileSdk 33, build-tools 33.0.2), `./gradlew assembleDebug`. `build.gradle` uses jcenter (dead) and AGP 3.5.1, so expect repo/plugin resolution trouble.
- Needs `local.properties` with `sdk.dir` (and optionally `mapsApiKey`).
- Tests: `test/java/.../AprsPacketTests.java` (one assertion). Run with `./gradlew test`. Nearly everything is untested.

## Workflow rules for this repo and session
- Develop on branch `ccr-3dbdfe4a-sssbhh`. Push with `git push -u origin ccr-3dbdfe4a-sssbhh`. Do not open a PR unless asked.
- `SPEC.md` and this `CLAUDE.md` were added in a review-only pass. The user asked for no code changes, and none were made. They are uncommitted unless the user says otherwise.
- Update `SPEC.md` and the Known issues list here when behavior changes or a listed issue is fixed.
