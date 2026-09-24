# Equinox - Fabric Edition

Fabric port of the **Equinox** Paper plugin for **Minecraft 26.1.2**.

## Building

```bash
cd mod
gradle build
```

The compiled mod jar is produced at `mod/build/libs/equinox-2.0.0.jar`.

Requires Java 25 and Gradle 9.x.

## Installing

1. Install the Fabric Loader for Minecraft 26.1.2.
2. Download Fabric API `0.155.3+26.1.2` into your `mods/` folder.
3. Drop `equinox-2.0.0.jar` into `mods/`.

## Commands

```
/equinox help
/equinox armor give <player> <leather|iron|golden|diamond|netherite>
/equinox enchant <swift|titan-leap|vitality> <level>
/equinox disenchant <type>
/equinox mount bind | info | unbind
/equinox whistle give <player>
/eq  (alias)
```

## Features ported

- Equinox Horse Armor (custom_data `equinox_armor=1`)
- Equinox Whistle (custom_data `equinox_whistle=1`, goat horn)
- Mount binding with permanent HOME vs. LAST KNOWN location split
- Whistle: return home / run to player / teleport / unloaded-chunk recovery
- Pegasus flight (SHIFT while riding) with the Bifrost pathway particles
- Swift / Titan Leap / Vitality enchantments (attribute modifiers)
- Mount registry persisted via SavedData (`equinox_mounts.dat`)
