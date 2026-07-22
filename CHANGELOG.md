# Changelog

## 1.0.3 - 2026-07-23

- Added centralized high-contrast Bedrock form styling across all 16 integrations and player selectors.
- Form titles and primary button lines now render in bold black; secondary detail lines render in dark blue against Bedrock's gray button surfaces.
- Removed inherited legacy, Bedrock, MiniMessage, obfuscated, and other external formatting codes before form serialization.
- Added regression tests for contrast styling, formatting-code cleanup, control characters, and empty form fields.
- Fixed the startup log to report the actual plugin version.

## 1.0.2 - 2026-07-06

- Added plugin-specific quick action catalogs for all 16 bundled ArtillexStudios integrations.
- Added internal Bedrock player selector flow for target-player commands such as AxTrade request, accept, and deny.
- Added support for reading target plugin command aliases from Artillex config paths like `command-aliases`, `player-command-aliases`, and `main-command-aliases`.
- Quick action commands now prefer the server's configured plugin alias when available.
- Expanded Bedrock forms so players can reach common plugin workflows without typing subcommands wherever safe.

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
