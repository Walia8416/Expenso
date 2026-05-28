# Expenso

India-first, UPI-first Android expense tracker. Scan a UPI QR, pick a category in one tap, launch your favourite UPI app, and the expense is logged — without parsing SMS or snooping on notifications.

## Why

Every other expense tracker in India tries to parse bank SMS or UPI notifications and guess categories. That approach is brittle because SMS formats vary across banks, merchant names are often raw VPAs, and category inference is ~60–75% accurate at best. Expenso flips the flow: the user picks the category **before** paying, so the expense entry is correct by the time money moves.

## Core flow

1. Open Expenso → camera scanner is the landing screen.
2. Point at any UPI QR (static or dynamic).
3. Bottom sheet appears with parsed VPA and optional prefilled amount.
4. Enter amount, pick category (one-tap chips), optional note.
5. Tap **Pay with [Your UPI app]** → Expenso launches Google Pay / PhonePe / Paytm / BHIM with a `upi://pay?...` intent.
6. Pay in the UPI app.
7. Return to Expenso → confirm sheet asks *"Did the payment go through?"* — [Paid] [Failed] [Later].
8. Expense is stored locally.

## Stack

- **Kotlin 2.0.21** + **Jetpack Compose** (Material 3)
- **MVVM** + `StateFlow` / `SharedFlow`, thin use-case layer
- **Hilt** for DI, **KSP** for annotation processing
- **Room 2.6.1** with **SQLCipher** (encrypted local DB, key in Android Keystore)
- **DataStore** (Preferences) for app settings
- **CameraX 1.4.1** + **ML Kit Barcode Scanning 17.3.0** (bundled model — works offline)
- **Accompanist Permissions** for runtime camera permission
- **WorkManager** + **Hilt-Work** for the pending-expense reconciliation nudge
- **Timber** for logging

Min SDK 24 · Target SDK 35 · Single-module (splits into feature modules post-MVP).

## Project layout

```
app/src/main/kotlin/com/expenso/app/
  ExpensoApp.kt              Application entrypoint + notification channels
  MainActivity.kt            ComponentActivity + Compose root
  navigation/                Root routing, bottom bar, RootViewModel
  core/
    data/
      db/                    Room DB + SQLCipher key provider + seed callback
      prefs/                 DataStore wrapper (ExpensoPrefs)
      repository/            Expense / Category / Payee repositories
    domain/
      model/                 Domain models (Expense, Category, Payee, ...)
      upi/                   UpiUriParser, UpiIntentBuilder, UpiAppDiscovery
    ui/
      theme/                 Material 3 color scheme, typography, theme wrapper
      components/            Shared Compose components (CategoryChip, money fmt)
  di/                        Hilt modules (DatabaseModule)
  feature/
    onboarding/              3-page value prop, camera permission, default UPI picker
    scanner/                 CameraX preview + ML Kit QR analyzer + ScannerScreen
    pay/                     PaySheet, ConfirmPaymentSheet, PayViewModel
    history/                 HistoryScreen, ExpenseDetailScreen
    insights/                InsightsScreen, CategoryDonut
    settings/                SettingsScreen, CategoryManagerScreen
```

## UPI technical notes

### QR format
```
upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>&tr=<txnref>
```
- `pa` is the only required param for a valid payment.
- `am` is absent on static QRs (stickers). User enters the amount.
- Signed merchant QRs include `sign`, `orgid`, `mode` — we preserve them verbatim or the transaction fails.

### Launching a UPI app
```kotlin
val uri = UpiIntentBuilder.buildUri(vpa, name, amount, note, txnRef)
val intent = UpiIntentBuilder.buildIntent(uri, targetPackage)
context.startActivity(intent)
```
Android 11+ requires `<queries>` in the manifest listing every UPI package we want to resolve. See `AndroidManifest.xml`.

### Payment success detection — the hard truth
`startActivityForResult` / Activity Result API on a UPI intent is **unreliable**. Different UPI apps return different extras (or none at all), and behaviour changes across versions. Expenso therefore:
- Fires the intent with `startActivity` (fire-and-forget).
- Creates a `PENDING` expense immediately, linked to a generated `transactionRef`.
- On `ON_RESUME`, if a pending expense exists, shows a confirm sheet asking the user.
- Never silently counts `PENDING` in totals.
- Optional: the best-effort Activity Result handler can pre-select the confirmation answer (not a source of truth).

## Privacy posture

- No SMS permission, no notification listener, no accessibility service.
- Local-only DB, encrypted at rest with SQLCipher (AES-256), key in Android Keystore via EncryptedSharedPreferences.
- `android:allowBackup="false"` to prevent the OS from cloud-backing the DB.
- Crash reporting is opt-in (Crashlytics wiring intentionally omitted from MVP).

## Building

1. Install **Android Studio Ladybug** (or newer), JDK 17.
2. Open the project root in Android Studio.
3. Let Gradle sync. You need internet on first sync to fetch dependencies.
4. Run configuration: `app`. Target: a real device with a UPI app installed (emulators don't have UPI apps).

From the CLI:
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:testDebugUnitTest
```

### Gradle wrapper
The `gradle-wrapper.jar` is not committed to keep the repo text-only — generate it once with:
```bash
gradle wrapper --gradle-version 8.9 --distribution-type bin
```
or just open the project in Android Studio, which will generate it automatically.

## Testing

`UpiUriParserTest` covers the critical parsing paths:
- Static QR without amount
- Dynamic signed merchant QR
- Non-UPI URI rejection
- Missing / malformed VPA
- Case normalization and name sanitization

Run:
```bash
./gradlew :app:testDebugUnitTest
```

## Roadmap (beyond MVP)

See `/.cursor/plans/expenso_upi_expense_tracker_*.plan.md` for the full product + technical plan. Short version:

- **v1.1** — Budgets per category, merchant auto-recognition (learned suggested categories).
- **v1.2** — CSV export, encrypted Drive backup (user-owned file).
- **v1.3** — Home screen widget + quick-settings tile.
- **v2** — Split expenses, biometric lock polish, anonymized aggregate insights.
