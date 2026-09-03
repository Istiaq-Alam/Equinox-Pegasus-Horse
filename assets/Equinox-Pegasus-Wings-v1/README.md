# Equinox Pegasus Wings

Functional first-pass Minecraft resource-pack assets for the Equinox plugin.

## IDs
- Left wing: CustomModelData 9001
- Right wing: CustomModelData 9002

## Files
- assets/equinox/models/item/pegasus_wing_left.json
- assets/equinox/models/item/pegasus_wing_right.json
- assets/equinox/textures/item/pegasus_wings.png
- assets/minecraft/items/feather.json

The plugin should create FEATHER ItemStacks with the matching CustomModelData and place
them in ItemDisplay entities.

Note: `pack_format` is intentionally left as 999 because exact client resource-pack format
must match the Minecraft client build used by your Paper 26.1.2 server.
