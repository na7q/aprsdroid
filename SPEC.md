# NA7Q APRSdroid — Technical Specification (as-built)

Reverse-engineered from the source on 2026-11-10 (HEAD `583a9e8`). This describes what the code *does*, not a design intent. Items marked **[Review]** are suspected defects or risks found while reading; none have been fixed.

## 1. Overview

NA7Q APRSdroid is a fork of ge0rg's APRSdroid (GPLv2), an Android amateur-radio APRS client written in **Scala 2.11** (plus a few Java files). The fork adds:

- RF digipeater, RF-only regeneration, and two-way IGate (APRS-IS <-> RF)
- BLE KISS TNC and DigiRig (USB serial PTT plus soundcard AFSK) backends
- Mic-E and compressed position encoding, including Mic-E status bits
- Offline maps (MBTiles and MapsForge), with Google Maps kept as an optional mode
- Metric/imperial units, hub log sorting, PTT tail, TNC frequency control, HUD broadcasts, and more

Package: `org.aprsdroid.app`. `minSdk` 14, `targetSdk`/`compileSdk` 33. Source root is `src/` (not `src/main`); resources are in `res/`. Manifest is at the repo root.

## 2. Repository layout

| Path | Contents |
|---|---|
| `src/*.scala` | Activities, services, DB, prefs, map code |
| `src/backend/` | Transport backends (APRS-IS TCP/UDP/HTTP, Bluetooth, BLE, USB, AFSK, DigiRig) |
| `src/tncproto/` | Wire protocols (`TncProto` subclasses): APRS-IS/TNC2, KISS, Kenwood, AFSK |
| `src/location/` | Location sources: SmartBeaconing, PeriodicGPS, FixedPosition |
| `res/xml/` | Preference screens (backend, location, digi, igate, privacy, etc.) |
| `res/values-*` | ~60 translations (`translations/`, `xml2po.sh` for tooling) |
| `libs/` | Prebuilt JARs: javAPRSlib (APRS parser), javAX25lib, jsoundmodem, mapsforge-map 0.3.0, usbserial 6.1.0 |
| `lib/`, `jni/` | Native libs per ABI |
| `PacketDroid/` | Git submodule (AFSK demod) |
| `doc/API.mdwn` | Third-party intent API |
| `test/`, `androidTest/` | One trivial unit test (`AprsPacket.passcode`) and an empty instrumented-test stub |
| `activities.md` | Activity inheritance map |
| `aprsdroid-release-7-24-25-latest.apk` | Binary checked into the repo |

## 3. Build and toolchain

- Gradle with the Android Gradle Plugin **3.5.1**, `gradle-android-scala-plugin` (AllBus fork), and grgit for versioning.
  - `versionName` is `git describe`; `versionCode` is `yyyyMMdd` + 2-digit minor.
- `build.gradle` uses **jcenter()**, which has been shut down. Resolution may fail or depend on mirrors/caches.
- Java 8 JDK is required. Scala 2.11.12 (`scala.collection.JavaConversions` is used in `TcpUploader`).
- ProGuard/minify is on for both debug and release (`proguard.cfg`).
- Dependencies:
  - play-services-maps 17.0.0 and android-maps-utils 0.5
  - androidx.core 1.6.0
  - Jetty server and servlet 9.4.43 (used for the local MBTiles tile server)
  - okio 2.1.0
- A Google Maps key is read from `local.properties` (`mapsApiKey`). If it is absent, **a hard-coded upstream key is embedded** **[Review]**.
- README notes that incremental builds may produce non-working APKs; do a clean build for verification.
- I could not run a build in this review environment, so nothing here is build-verified.

## 4. Runtime architecture

### 4.1 Core: `AprsService` (`src/AprsService.scala`)

A foreground `Service` (`foregroundServiceType="location"`) and the hub of the app.

**Intent actions** (all exported, no permission guard):

| Action | Behavior |
|---|---|
| `SERVICE` | Start continuous tracking |
| `ONCE` | Send one position, then stop (`singleShot`) |
| `SEND_PACKET` | Parse the `data` extra as an APRS body and transmit it; ignored if the service is not running |
| `SERVICE_STOP` | Clear the `service_running` pref and `stopSelf()` |
| `FREQUENCY` | Set the QRG comment, then fall through to `ONCE` if running |
| `IGATE_START`/`IGATE_STOP` | Internal, defined in the code but not declared in the manifest filter |

**Broadcasts emitted:**

- `SERVICE_STARTED` / `SERVICE_STOPPED`
- `UPDATE` (log post added)
- `POSITION` (station heard)
- `MESSAGE` / `MESSAGETX`
- `LINK_ON` / `LINK_OFF`
- `HUD` (the NA7Q Mobile HUD companion app consumes this)

**Lifecycle:**

1. `handleStart` -> `startPoster()` instantiates the backend via `AprsBackend.instanciateUploader`.
2. The backend calls `service.postPosterStarted()` or returns `true` from `start()`.
3. `onPosterStarted()` starts the location source, flushes pending messages, broadcasts `SERVICE_STARTED`, persists `service_running=true`, and starts the IGate if enabled.
4. `SystemEventReceiver` restarts the service on boot if `service_running` is set.

**Outbound flow:**

1. `postLocation(location)` picks the format:
   - Mic-E when `compressed_mice` is set.
   - Otherwise `formatLoc`: uncompressed, or compressed when `compressed_location` is set.
2. `sendPacket(packet, postfix)` runs on a `Future` and calls `poster.update(packet)`.
3. The result is logged as a DB post. The type depends on the postfix:
   - `"Digipeated"` -> `TYPE_DIGI`
   - `"APRS-IS > RF"` -> `TYPE_TX`
   - otherwise `TYPE_POST`
4. `sendIsPacket` also forwards own packets to APRS-IS through the IGate connection when `p.positiontois` is on.

**Inbound flow:**

1. A backend calls `service.postSubmit(rawLine)`.
2. That does `postAddPost(TYPE_INCMG)` -> `addPost` -> `parsePacket` (stores positions/objects, handles messages, detects "own packet was digipeated") and `parseHudPackets`.
3. `DigipeaterService.processIncomingPost` runs.
4. If the IGate is on, `IgateService.handlePostSubmitData` runs. Only KISS and AFSK backends gate and digipeat, detected by the backend display name.

### 4.2 Backends (`src/backend/`, `AprsBackend.scala`)

Config model: pref `proto` ∈ {aprsis, afsk, kiss, tnc2, kenwood}. The link comes from pref `link`, `afsk` or `aprsis` depending on the proto. `backend_collection` maps link -> `BackendInfo(create, prefxml, permissions, duplex, passcode need)`.

| Backend | Class | Notes |
|---|---|---|
| tcp / tcpip | `TcpUploader` | APRS-IS or TCP TNC. Tries TLS first (callsign `.p12` in the app keystore dir, port 24580), then plain TCP (14580). Auto-reconnect loop. |
| udp / http | `UdpUploader` / `HttpPostUploader` | TX-only APRS-IS |
| bluetooth | `BluetoothTnc` | SPP socket |
| ble | `BluetoothLETnc` | GATT service `00000001-ba2a-46c9-ae49-01b0961f68bb` (RX `…03`, TX `…02`). MTU negotiation, reconnect, custom In/OutputStream. |
| usb | `UsbTnc` | usb-serial-for-android |
| vox / digirig | `AfskUploader` / `DigiRig` | Soundcard AFSK via jsoundmodem/PacketDroid; DigiRig adds a USB serial PTT with a configurable `ptt.tail` (default 200 ms) |

Protocols (`TncProto`): `Tnc2Proto`/`AprsIsProto` (text, APRS-IS login plus filter), `KissProto` (KISS framing, init strings, a vendor frequency-set frame `FEND 06 EA <4B freq> FEND`, `RETURN` frame), `KenwoodProto`, `AfskProto`.

### 4.3 Digipeater (`DigipeaterService`)

- Active only for KISS/AFSK backends.
- Modes:
  - `p.digipeating`: full digipeat.
  - `p.regenerate` (with digipeating off): re-transmit the heard packet unchanged.
- Digipeat algorithm:
  - Skip own packets.
  - Dedupe on a `src>dst:payload` key for `p.dedupe` seconds.
  - Digipeat only when the first unused path element matches `digipeater_path` (default `WIDE1,WIDE2`) by base name.
  - Decrement the `-N` suffix, or replace the element with `MYCALL*` when N reaches 0.
  - `p.directonly` refuses any packet that already has a `*` in its path.
- Digipeated frames are sent via `sendDigipeatedPacket` -> `sendPacket(..., "Digipeated")`.

### 4.4 IGate (`IgateService.scala`: `IgateService` + `TcpSocketThread`)

- Separate APRS-IS TCP connection (`p.igserver`, default `rotate.aprs2.net:14580`) with login `user CALL pass CODE vers …` and `#filter …`.
- **RF -> IS:**
  - Skips packets containing `RFONLY`, `TCPIP`, or `NOGATE` anywhere in the string.
  - Inserts `,qAO,MYCALL` (or `qAR` if bidirectional) before the first `:`.
  - Records "last heard" per source call.
- **IS -> RF** (only when `p.aprsistorf` is on):
  - Messages: gated only if the *addressee* was heard on RF within `p.timelastheard` minutes. This creates an "MSP" entry for the sender.
  - Positions: gated only if the source has an MSP entry (one-shot).
  - Third-party format `}SRC>DST,TCPIP,MYCALL*:payload` is sent via `igpath` (default `WIDE1-1`).
  - Ignores telemetry definitions and `BLN`/`NWS`/`SKY`/`CWA`/`BOM` bulletins.
  - Rate limits: `p.ratelimit1` (default 6/min) and `p.ratelimit5` (default 10/5 min).
- `p.aprsistraffic`: when set, it suppresses showing general IS traffic but still shows messages.

### 4.5 Messaging (`MessageService`)

- Incoming messages to own call+SSID are stored, notified, and ACKed. Messages to other SSIDs of the same callsign are stored without an ACK.
- ACKs/REJs update outbound message state.
- Outbound retries use exponential backoff: base `p.retry` (30 s), up to `p.messaging` (7) tries, then a final 15 s step, then `TYPE_OUT_ABORTED`.
- Messages with no message ID are marked "acked" immediately after the first send.
- Pending messages are re-driven by a `Handler` timer and by `MESSAGETX` broadcasts.

### 4.6 Location sources (`src/location/`)

- **SmartBeaconing**
  - GPS provider, corner pegging, speed-scaled interval.
  - Settings: `sb.fastspeed`, `sb.fastrate`, `sb.slowspeed`, `sb.slowrate`, `sb.turntime`, `sb.turnmin`, `sb.turnslope`.
  - Metric/imperial aware (`p.units`).
- **PeriodicGPS:** fixed interval.
- **FixedPosition:** manual lat/lon.

### 4.7 Packet encoding (`AprsPacket.scala`)

Pure helpers:
- Mic-E: `encodeDest`, `encodeInfo`, `statusToBits`, altitude, ambiguity.
- Compressed position, course/speed, altitude, frequency formatting.
- Passcode hash, host:port parsing, comment/QRG/device parsing.

Position privacy prefs: `priv_ambiguity`, `priv_spdbear`, `priv_altitude`.

### 4.8 Persistence (`StorageDatabase.scala`)

SQLite `storage.db`, version 4. Tables:
- `posts` (log; types POST/INFO/ERROR/INCMG/TX/DIGI/IG)
- `stations` (unique by call)
- `positions` (track points)
- `messages`

Posts are periodically trimmed. Preferences live in default `SharedPreferences`, accessed through `PrefsWrapper`.

### 4.9 UI

`HubActivity` (stations by distance/newest), `LogActivity`, `ConversationsActivity`/`MessageActivity`, `StationActivity`, `PrefsAct` and sub-prefs screens, `MapAct` (MapsForge/MBTiles/OSM), `GoogleMapAct`, symbol chooser, profile (`.aprs` JSON) and key (`.p12`) import activities, and `PermissionHelper` (runtime permissions incl. BT_CONNECT, POST_NOTIFICATIONS, all-files access). `activities.md` has the inheritance tree.

### 4.10 Permissions and exposure

Notable manifest items:
- Location (fine/coarse), Bluetooth (legacy plus CONNECT), RECORD_AUDIO, `MANAGE_EXTERNAL_STORAGE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`.
- `usesCleartextTraffic=true`, `allowBackup=true`, `requestLegacyExternalStorage=true`.
- Exported: `AprsService`, `MapAct`, `ProfileImportActivity`, `KeyfileImportActivity`, `SystemEventReceiver`.

## 5. Third-party API

See `doc/API.mdwn`. In short: start and stop the service and send a packet with the intent actions above; listen for `SERVICE_STARTED`, `POSITION`, `MESSAGE`, `UPDATE`. API version constant is 1.

## 6. Review findings

Ordered roughly by impact. All are read-only observations; confidence is noted.

### Likely defects

1. **SmartBeaconing double-converts speed** (`SmartBeaconing.smartBeaconSpeedRate`, high confidence).
   - `getSpeed()` already returns m/s, but the value is passed through `convertSpeed(speed, isMetric)`, dividing by 3.6 or 2.237 again.
   - The thresholds are converted correctly once, so the measured speed looks 3.6x (or 2.2x) too slow.
   - Beacon rate is biased toward the slow interval, and corner pegging is unaffected.
2. **Ack-dedupe feature can suppress all ACKs** (`MessageService.handleMessage`).
   - When `p.ackdupetoggle` is on and `p.ackdupe` is 0 (the default), the code returns before sending any ACK.
   - The ACK is only sent when the toggle is off.
3. **Backend type is detected by localized display-name substring** (`getBackendName().contains("KISS"|"AFSK"|"Bluetooth")`, used in `AprsService`, `DigipeaterService`, `KissProto`).
   - Names come from translated string arrays, so digipeat, IGate, and frequency control can silently stop working in locales that translate those labels.
   - Fix direction: use the raw `proto`/`link` pref values.
4. **IGate reconnect blocks and re-enters** (`IgateService.reconnect`).
   - It runs on the socket thread, calls `stop()` (which joins itself) and `Thread.sleep(retry*1000)`, then spawns a new thread.
   - State flags (`reconnecting`) are unsynchronized.
   - IGate settings (host, timeouts, retry) are read once in the constructor, so changes need a service restart.
5. **IGate sends from background threads straight into `AprsService.addPost`** (DB writes, broadcasts, parsing) without the `Handler` the other backends use via `postAddPost`. Thread-safety depends on SQLite's own locking.
6. **IGate login/filter use `println("...\r\n")`**, which produces blank lines. It is tolerated by servers but sloppy. The `vers` string is built by an odd digit-splitting expression.
7. **`modifyData` filters with substring `contains`** for `RFONLY`/`TCPIP`/`NOGATE`, so a comment or payload containing those words suppresses gating. It should check the path elements.
8. **KISS frequency escaping** (`KissProto.freqConvert`) escapes `FEND` but not `FESC` (0xDB).
9. **`DigipeaterService`** keeps unsynchronized shared state and uses non-local `return` inside `foldLeft`. Callsign comparison to own call is case-sensitive.

### Security and privacy

10. **`AprsService` is exported with no permission**, so any installed app can send `SEND_PACKET` and cause RF transmission under the user's callsign. This is a regulatory and abuse risk (also true upstream).
11. **TLS trust is disabled** (`NaiveTrustManager` accepts every certificate) for APRS-IS SSL, and cleartext traffic is allowed app-wide.
12. **`HUD` and `POSITION` broadcasts are implicit and unrestricted.** Any app can receive station positions and packets, plus their own location. The private `MSG_*` intents do use `setPackage`.
13. A Google Maps API key fallback is committed in `build.gradle`; the key is restricted to the upstream signer.
14. `MANAGE_EXTERNAL_STORAGE` is requested for offline map files, which is broad.
15. A release APK is committed in the repo (provenance and size).

### Maintainability

16. Build stack is very old: AGP 3.5.1, jcenter, Scala 2.11, legacy `android.preference` and Apache HTTP legacy, `minSdk` 14.
17. Test coverage is effectively nil (one passcode assertion). The digipeater path logic, IGate rules, Mic-E encoding, and SmartBeaconing are all pure or near-pure and would be easy to unit test.
18. Large, heavily commented-out and mixed-indentation (tabs/spaces) files: `AprsService` (742 lines), `IgateService` (615), `BluetoothLETnc` (513), `StorageDatabase` (506). Debug logging is very verbose, including full packets and locations via `Log.d`.
19. Duplicate class name: `TcpSocketThread` exists as an inner class of `TcpUploader` and as a top-level class in `IgateService.scala`. It compiles because of scoping but is confusing.
20. Two near-identical network stacks (`TcpUploader` and `IgateService`) with different reconnect behavior.

## 7. Suggested next steps (not done)

1. Fix items 1–3 (small, contained, user-visible).
2. Add unit tests for `DigipeaterService.processDigiPath`, `IgateService` filters/rate limits, `AprsPacket` Mic-E, and `SmartBeaconing.smartBeaconSpeedRate`.
3. Harden the exported service and broadcasts (permission or `setPackage` where feasible, which means coordinating with the HUD app).
4. Refactor IGate reconnect onto a handler/executor; read prefs at connect time.
5. Modernize the build (drop jcenter, bump AGP/Gradle) once a green baseline build exists.
