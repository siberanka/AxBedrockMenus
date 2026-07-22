# AxBedrockMenus

AxBedrockMenus adds Floodgate/Geyser Bedrock form support for free ArtillexStudios plugins without filling fake inventory slots.

Package: `com.siberanka.axbedrockmenus`  
Author: `siberanka`  
Version: `1.0.3`

## Supported ArtillexStudios Plugins

The bundled integration matrix targets the 16 public ArtillexStudios Modrinth projects verified on July 6, 2026:

AxGraves, AxTrade, AxVaults, AxShulkers, AxAFKZone, AxInventoryRestore, AxPlayerWarps, AxSellwands, AxRankMenu, AxRewards, AxMinions, AxEnvoys, AxSmithing, AxMines, AxKills, AxCalendar.

## How It Works

- Bedrock players detected through Floodgate are intercepted before known Artillex menu commands open Java inventory GUIs.
- Provider-specific menu adapters are used where safe. For example, AxRankMenu reads `ranks.yml` and displays ranks, prices, currencies, and server scope directly in the Bedrock form.
- The generic Artillex scanner reads the target plugin's data folder for configured YAML menu/resource files and extracts safe navigation buttons plus informational entries.
- Integration quick actions provide plugin-specific Bedrock buttons for common player workflows such as graves, trade requests, vault selection, rewards, player warps, calendar, mines, envoys, and similar entry points.
- Player-targeted actions such as AxTrade request/accept/deny open a second Bedrock player picker instead of requiring typed names.
- Configured Artillex command aliases are read from the target plugin's `config.yml` where possible, so custom server aliases are respected.
- Filler panes and decorative empty entries are ignored.
- Form titles and primary button labels use bold black text; secondary button lines use dark blue text for strong contrast against Bedrock's gray button surfaces. Form content remains white against its dark content panel.
- Button clicks execute sanitized player commands on the player's Folia entity scheduler only when the action can be delegated safely.
- Reward `claim-commands` and rank `buy-actions` are never executed directly by AxBedrockMenus; those must stay behind the owning plugin's own economy, cooldown, permission, and state checks.
- Stale form responses are invalidated on reload/disable and duplicate responses are consumed once.

## Build

```bash
gradle clean build
```

The jar is generated in `build/libs/`.

## Admin Commands

```text
/axbedrockmenus reload
/axbedrockmenus status
```

## Reporting Issues

Use GitHub Issues for reproducible bugs, compatibility reports, and feature requests:

https://github.com/siberanka/AxBedrockMenus/issues

Do not report dupes, exploit payloads, bypasses, or private crash vectors publicly. Use the security policy instead:

https://github.com/siberanka/AxBedrockMenus/security/policy

## Security Notes

AxBedrockMenus does not move items or create server-side inventories. It uses Bedrock forms and delegates real actions back to the owning plugin through validated commands. Command payloads are length-limited, newline-blocked, root-filtered, rate-limited, and executed only after the player context is checked again.

Review `SECURITY.md` before enabling custom console actions or editing `integrations.yml`.
