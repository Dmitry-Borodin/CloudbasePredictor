Use only English for documentation and commands in the code.

For every file that defines `@Composable` UI, add at least one `@Preview` function at the end of that same file with realistic example input.

Keep preview sample data for UI models in a dedicated preview data layer (for example `ui/preview/PreviewData.kt` or feature-level `PreviewData.kt`) and reuse it from previews instead of hardcoding values inline.

