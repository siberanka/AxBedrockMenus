# Security Policy

AxBedrockMenus is designed as a form bridge, not as an item container. It never places plugin-owned items in a fake inventory and never trusts client-side slot state.

## Hardening Model

- Floodgate-only interception for Bedrock players.
- Permission and plugin-enabled checks before every menu open.
- Per-player rate limits for menu opens and button clicks.
- One-shot form callbacks with stale-session invalidation.
- Folia-safe player-context scheduling before Bukkit/command interaction.
- Bounded YAML scanning with maximum file size, node count, depth, and button count.
- Filler/decorative items are ignored.
- Commands are normalized, placeholder-expanded from trusted server values only, length-limited, newline-blocked, and checked against dangerous roots/substrings.
- Console command execution is disabled by default.
- Config/lang files are validated and backed up before automatic repair.

## Reporting

Open a private security advisory or contact the maintainer with:

- Server version and platform.
- Floodgate/Geyser versions.
- AxBedrockMenus version.
- Target Artillex plugin and version.
- Minimal reproduction steps.

Do not publish dupe/exploit details before a fix is available.
