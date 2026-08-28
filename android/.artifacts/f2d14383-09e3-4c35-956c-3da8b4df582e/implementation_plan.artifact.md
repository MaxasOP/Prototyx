# Fix Crash on Scroll in Committee Screen

The app crashes when scrolling down in the `CommitteeScreen` after running "Manager Orchestration". This is likely due to non-defensive handling of API response data, particularly around null values in maps or potential issues with Material 3 components during lazy loading.

## Proposed Changes

### [Data Models]

#### [MODIFY] [Models.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/data/model/Models.kt)
- Make properties in response data classes nullable to safely handle missing or null fields from the API (especially maps which GSON can populate with nulls even for non-nullable Kotlin types).

### [UI Layer]

#### [MODIFY] [CommitteeScreen.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/ui/screens/CommitteeScreen.kt)
- Add null safety to `String.format` calls (e.g., using `?.let` or default values).
- Replace `HorizontalDivider` (which can occasionally cause layout-based crashes in nested lazy columns in some M3 versions) with a more stable custom implementation.
- Add `key` to `item` blocks in `LazyColumn` for better state management during scroll.
- Use `Locale.US` in `String.format` to ensure consistent formatting across different device locales.

## Verification Plan

### Automated Tests
- Run `:app:compileDebugKotlin` to ensure no regression in types.

### Manual Verification
- Deploy the app to the device.
- Navigate to the Committee screen.
- Run "Launch Manager Orchestration".
- Scroll down to verify the "Worker Metrics" card renders without crashing.
- Run "Initiate Governance Debate" and verify the "Implied Forward Estimates" card renders and scrolls correctly.
