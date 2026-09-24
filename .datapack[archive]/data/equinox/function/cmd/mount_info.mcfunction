# ============================================================
# EQUINOX - MOUNT INFO
# Usage: /function equinox:cmd/mount_info   (run as the player)
# ============================================================

# --- Player UUID context ---
execute store result storage equinox:ctx i0 run data get entity @s UUID[0]
execute store result storage equinox:ctx i1 run data get entity @s UUID[1]
execute store result storage equinox:ctx i2 run data get entity @s UUID[2]
execute store result storage equinox:ctx i3 run data get entity @s UUID[3]

# Refresh the one-entry view, or clear it
data remove storage equinox:view e
$execute if data storage equinox:mounts mounts[{id:[I;$(i0),$(i1),$(i2),$(i3)]}] run data modify storage equinox:view e set from storage equinox:mounts mounts[{id:[I;$(i0),$(i1),$(i2),$(i3)]}]

execute unless data storage equinox:view e run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"You do not have a registered Equinox mount.","color":"red"}]

tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"✦ EQUINOX MOUNT INFO ✦","color":"gold","bold":true}]
tellraw @s [{"text":"Status: ","color":"gray"},{"text":"Registered","color":"green"}]
tellraw @s [{"text":"Dimension: ","color":"gray"},{"nbt":"e.dim","storage":"equinox:view","color":"white"}]
tellraw @s [{"text":"Home: ","color":"gray"},{"nbt":"e.home.x","storage":"equinox:view","color":"white"},{"text":", ","color":"white"},{"nbt":"e.home.y","storage":"equinox:view","color":"white"},{"text":", ","color":"white"},{"nbt":"e.home.z","storage":"equinox:view","color":"white"}]
tellraw @s [{"text":"Last known: ","color":"gray"},{"nbt":"e.last.x","storage":"equinox:view","color":"white"},{"text":", ","color":"white"},{"nbt":"e.last.y","storage":"equinox:view","color":"white"},{"text":", ","color":"white"},{"nbt":"e.last.z","storage":"equinox:view","color":"white"}]
