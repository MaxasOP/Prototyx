# Fix Black Screen and Theme Issues

The user reports a black screen on launch. Analysis shows that the UI hierarchy is present but likely invisible due to theme/color mismatches (black-on-black text) or rendering issues related to edge-to-edge configuration with an outdated parent theme.

## User Review Required

> [!IMPORTANT]
> I am updating the app's theme to use Material 3 standards and refactoring components to use semantic colors (`MaterialTheme.colorScheme`) instead of hardcoded hex values. This will ensure the app works correctly in both Light and Dark modes.

## Proposed Changes

### Core Theme & Configuration

#### [MODIFY] [themes.xml](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/res/values/themes.xml)
- Update parent theme to `Theme.Material3.DayNight.NoActionBar` to support modern Compose/M3 features and edge-to-edge rendering.

#### [MODIFY] [Theme.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/theme/Theme.kt)
- Ensure `PrototyxTheme` correctly applies the color scheme and provides it to the `MaterialTheme`.
- Verify `DarkColorScheme` and `LightColorScheme` mapping.

### UI Components & Screens

#### [MODIFY] [UIComponents.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/ui/components/UIComponents.kt)
- Replace hardcoded `TextCharcoal` and `SurfaceWhite` with `MaterialTheme.colorScheme` references.
- Fix `EditorialHeading` and `MetadataLabel` to use `colorScheme.onSurface` or `onBackground`.
- Update `DoubleBezelCard` to use `colorScheme.surface` or `surfaceVariant`.

#### [MODIFY] [Navigation.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/Navigation.kt)
- Update `NavigationBar` to use `MaterialTheme.colorScheme.surface` instead of hardcoded `Color.White`.
- Update icon and label colors to be theme-aware.

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/ui/screens/DashboardScreen.kt)
- Ensure the background and text contrast correctly.
- Fix any remaining hardcoded colors.

### Activity

#### [MODIFY] [MainActivity.kt](file:///C:/Users/hp/Desktop/AI-Wealth/Prototyx/android/app/src/main/java/com/example/prototyx/MainActivity.kt)
- Ensure `enableEdgeToEdge()` is used with a compatible theme.

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to ensure no build regressions.

### Manual Verification
- Deploy to emulator and verify that the screen is no longer black.
- Toggle between Light and Dark modes to ensure visibility in both.
- Inspect the UI hierarchy and screenshot to confirm rendering.
