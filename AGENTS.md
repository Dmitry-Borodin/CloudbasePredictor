Use only English for documentation and commands in the code.

For every file that defines `@Composable` UI, add at least one `@Preview` function at the end of that same file with realistic example input.

Keep preview sample data for UI models in a dedicated preview data layer (for example `ui/preview/PreviewData.kt` or feature-level `PreviewData.kt`) and reuse it from previews instead of hardcoding values inline.

Test policy:
- The agent must run only tests that are relevant to files changed in the current repository diff.
- The full test suite should be run only when explicitly requested by the user or when changes affect shared infrastructure, navigation, or app startup.

Manual test commands (agent and user):
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugAndroidTestKotlin`
- `./gradlew :app:connectedDebugAndroidTest`
