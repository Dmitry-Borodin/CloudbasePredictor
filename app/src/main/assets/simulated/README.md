# Simulated Forecast Snapshots

Real Open-Meteo API responses saved as JSON for deterministic testing and screenshots.

## brauneck_icon_seamless_20260418.json

- **Location**: Brauneck Süd, Bavaria, Germany
- **Coordinates**: 47.66347, 11.52365 (N 47°39'48.48" E 11°31'25.14")
- **Elevation**: 1523 m ASL (Open-Meteo grid cell)

### Contents

| Block        | Entries | Interval | Description                                      |
|--------------|---------|----------|--------------------------------------------------|
| `hourly`     | 24      | 1 h      | Full surface + 14 pressure levels (1000–500 hPa) |
| `minutely_15`| 96      | 15 min   | Surface T, Td, precip, wind, radiation, is_day   |
| `daily`      | 1       | —        | Tmax, Tmin, weather_code, sunrise, sunset         |

### Usage

When `DataSourcePreference.SIMULATED` is selected in settings, the app reads this
JSON via the Android AssetManager and converts it to `HourlyForecastData` — the same
model the real API path produces. This gives deterministic, network-free forecasts for
all chart views, unit tests, instrumentation tests, and screenshot captures.
