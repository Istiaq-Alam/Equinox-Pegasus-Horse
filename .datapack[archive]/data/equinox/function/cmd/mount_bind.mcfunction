# ============================================================
# EQUINOX - BIND MOUNT
# Usage: /function equinox:cmd/mount_bind   (run as the player)
#
# Mirrors /equinox mount bind:
#   - raycasts up to ~7 blocks for a horse
#   - must be tamed, owned by you
#   - must wear Equinox Horse Armor
# ============================================================

# --- Reset raycast state ---
tag @e[tag=equinox_target] remove equinox_target
tag @a[tag=equinox_binder] remove equinox_binder
tag @s add equinox_binder
scoreboard players set #ray equinox 0

# --- Raycast from the player's eyes ---
execute as @s rotated as @s anchored eyes positioned ^ ^ ^ run function equinox:mount/ray

# --- Capture this player's UUID (4 ints) as macro context ---
execute store result storage equinox:ctx i0 run data get entity @s UUID[0]
execute store result storage equinox:ctx i1 run data get entity @s UUID[1]
execute store result storage equinox:ctx i2 run data get entity @s UUID[2]
execute store result storage equinox:ctx i3 run data get entity @s UUID[3]

# --- Validate and register ---
function equinox:mount/bind with storage equinox:ctx
