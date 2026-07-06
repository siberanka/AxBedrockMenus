# AxBedrockMenus

AxBedrockMenus adds Floodgate/Geyser Bedrock form support for free ArtillexStudios plugins without filling fake inventory slots.

Package: `com.siberanka.axbedrockmenus`  
Author: `siberanka`  
Version: `1.0.0`

## Supported ArtillexStudios Plugins

The bundled integration matrix targets the 16 public ArtillexStudios Modrinth projects verified on July 6, 2026:

AxGraves, AxTrade, AxVaults, AxShulkers, AxAFKZone, AxInventoryRestore, AxPlayerWarps, AxSellwands, AxRankMenu, AxRewards, AxMinions, AxEnvoys, AxSmithing, AxMines, AxKills, AxCalendar.

## How It Works

- Bedrock players detected through Floodgate are intercepted before known Artillex menu commands open Java inventory GUIs.
- The plugin scans the target plugin's data folder for configured YAML menu files and extracts real actionable buttons.
- Filler panes and decorative empty entries are ignored.
- Button clicks execute sanitized player commands on the player's Folia entity scheduler.
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

## Security Notes

AxBedrockMenus does not move items or create server-side inventories. It uses Bedrock forms and delegates real actions back to the owning plugin through validated commands. Command payloads are length-limited, newline-blocked, root-filtered, rate-limited, and executed only after the player context is checked again.

Review `SECURITY.md` before enabling custom console actions or editing `integrations.yml`.
