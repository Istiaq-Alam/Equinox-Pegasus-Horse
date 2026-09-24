# ============================================================
# EQUINOX - REGISTER MOUNT
# Called with storage equinox:ctx {i0..i3} as the player.
# Stores: owner UUID, dimension, permanent HOME (bind spot),
# LAST KNOWN location and horse UUID. Never duplicates entries.
# ============================================================

# --- Release any previously bound horse of this player ---
execute as @e[type=horse,tag=equinox_bound] run function equinox:mount/scan with storage equinox:ctx
tag @e[tag=equinox_hit] remove equinox_bound
tag @e[tag=equinox_hit] remove equinox_flying
tag @e[tag=equinox_hit] remove equinox_chase
tag @e[tag=equinox_hit] remove equinox_sw_on
tag @e[tag=equinox_hit] remove equinox_lp_on
tag @e[tag=equinox_hit] remove equinox_vt_on
tag @e[tag=equinox_hit] remove equinox_hit

# --- Upsert the registry entry ---
$data remove storage equinox:mounts mounts[{id:[I;$(i0),$(i1),$(i2),$(i3)]}]
$data modify storage equinox:mounts mounts append value {id:[I;$(i0),$(i1),$(i2),$(i3)]}

# Permanent HOME = bind location (never changes automatically)
data modify storage equinox:mounts mounts[-1].dim set from entity @e[tag=equinox_target,limit=1] Dimension
data modify storage equinox:mounts mounts[-1].home.x set from entity @e[tag=equinox_target,limit=1] Pos[0]
data modify storage equinox:mounts mounts[-1].home.y set from entity @e[tag=equinox_target,limit=1] Pos[1]
data modify storage equinox:mounts mounts[-1].home.z set from entity @e[tag=equinox_target,limit=1] Pos[2]

# Horse UUID
data modify storage equinox:mounts mounts[-1].hid set from entity @e[tag=equinox_target,limit=1] UUID

# LAST KNOWN starts equal to HOME (updated every 5 seconds afterwards)
data modify storage equinox:mounts mounts[-1].last set from storage equinox:mounts mounts[-1].home

# --- Tag the real horse ---
tag @e[tag=equinox_target] add equinox_bound
tag @e[tag=equinox_target] remove equinox_target
tag @s remove equinox_binder

# --- Success messages (plugin parity) ---
tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"✦ EQUINOX MOUNT BOUND ✦","color":"gold","bold":true}]
tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Your horse has been successfully bound!","color":"green"}]
tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"You can now use your Equinox Mount system.","color":"dark_gray"}]
