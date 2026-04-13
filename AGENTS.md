Use only English for documentation and commands in the code.

Local agent preferences:
- If `./agent-local/AGENTS.md` exists, read it after this file and apply it as a local extension for this repository.

For every file that defines `@Composable` UI, add at least one `@Preview` function at the end of that same file with realistic example input.

Keep preview sample data for UI models in a dedicated preview data layer (for example `ui/preview/PreviewData.kt` or feature-level `PreviewData.kt`) and reuse it from previews instead of hardcoding values inline.

Manual test commands (agent and user):

Unit tests (JVM, no device needed):
- `./gradlew :app:testDebugUnitTest`

Instrumentation tests (require running emulator/device):
- `./gradlew :app:connectedInstrumentationTest`

Compile-check instrumentation tests without running:
- `./gradlew :app:compileDebugAndroidTestKotlin`

E2E tests (require running emulator/device AND real network to Open-Meteo backend):
- `./gradlew :app:connectedE2eTest`

Screenshot capture (require running emulator/device, uses simulated data):
- Capture all screens:
  `./gradlew :app:connectedScreenshotTest`
- Capture a single screen (e.g. Stüve):
  `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cloudbasepredictor.screenshot.ScreenshotCaptureTest#captureStuveForecast`
- Pull screenshots from device to local folder:
  `adb pull /sdcard/Pictures/CloudbaseScreenshots/ app/screenshots/`
- Screenshots are saved to `app/screenshots/` which is gitignored.
- After pulling, the agent can view screenshots via the `view_image` tool to iterate on UI changes.
