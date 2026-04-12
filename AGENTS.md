Use only English for documentation and commands in the code.

Local agent preferences:
- If `./agent-local/AGENTS.md` exists, read it after this file and apply it as a local extension for this repository.

For every file that defines `@Composable` UI, add at least one `@Preview` function at the end of that same file with realistic example input.

Keep preview sample data for UI models in a dedicated preview data layer (for example `ui/preview/PreviewData.kt` or feature-level `PreviewData.kt`) and reuse it from previews instead of hardcoding values inline.

Manual test commands (agent and user):
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugAndroidTestKotlin`
- `./gradlew :app:connectedDebugAndroidTest`
