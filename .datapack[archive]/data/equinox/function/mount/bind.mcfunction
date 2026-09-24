# ============================================================
# EQUINOX - BIND VALIDATION
# Called with storage equinox:ctx {i0..i3} as the player.
# equinox_target tags the horse found by the raycast.
# ============================================================

# No horse looked at
execute unless entity @e[tag=equinox_target] run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"You must look directly at your horse.","color":"red"}]

# Must be tamed
execute unless data entity @e[tag=equinox_target,limit=1] Tame run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"This horse is not tamed.","color":"red"}]

# Must be owned by the player
$execute unless data entity @e[tag=equinox_target,limit=1] {Owner:[I;$(i0),$(i1),$(i2),$(i3)]} run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"You do not own this horse.","color":"red"}]

# Must wear Equinox Horse Armor
execute unless data entity @e[tag=equinox_target,limit=1] Inventory[{components:{"minecraft:custom_data":{equinox_armor:"1"}}}] run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Your horse must wear Equinox Horse Armor.","color":"red"}]

# All checks passed
return run function equinox:mount/register with storage equinox:ctx
