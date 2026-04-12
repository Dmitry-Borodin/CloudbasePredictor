Use only English for documentation and commands in the code.

Local agent preferences:
- If `./agent-local/AGENTS.md` exists, read it after this file and apply it as a local extension for this repository.

For every file that defines `@Composable` UI, add at least one `@Preview` function at the end of that same file with realistic example input.

Keep preview sample data for UI models in a dedicated preview data layer (for example `ui/preview/PreviewData.kt` or feature-level `PreviewData.kt`) and reuse it from previews instead of hardcoding values inline.

Manual test commands (agent and user):

Unit tests (JVM, no device needed):
- `./gradlew :app:testDebugUnitTest`

Instrumentation tests (require running emulator/device):
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.cloudbasepredictor.e2e.ForecastModelE2eTest`

Compile-check instrumentation tests without running:
- `./gradlew :app:compileDebugAndroidTestKotlin`

E2E tests (require running emulator/device AND real network to Open-Meteo backend):
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cloudbasepredictor.e2e.ForecastModelE2eTest`
