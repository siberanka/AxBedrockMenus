# Changelog

## 1.0.1 - 2026-07-06

- Added provider-based menu generation.
- Added a dedicated AxRankMenu provider that reads `ranks.yml` and shows rank names, prices, currencies, and server scope directly in Bedrock forms.
- Expanded ArtillexStudios menu/resource paths for `menus/`, `guis/`, `sellwands/`, `crates/`, `envoys/`, `mines/`, `zones/`, `ranks.yml`, and calendar reward files.
- Added informational form buttons for config-backed rewards/ranks/mines/envoys when no safe owner-plugin action is available.
- Stopped executing `buy-actions` and `claim-commands` directly from third-party configs to avoid bypassing owner-plugin economy, cooldown, permission, and state checks.

## 1.0.0 - 2026-07-06

- Initial public release.
- Added Floodgate/Geyser Bedrock form bridge.
- Added 16-plugin ArtillexStudios integration matrix.
- Added Folia-safe scheduling, rate limits, stale form invalidation, config/lang repair, and security-focused command validation.
