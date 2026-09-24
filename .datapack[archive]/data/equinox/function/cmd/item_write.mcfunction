# ============================================================
# EQUINOX - ITEM WRITE (shared by enchant / disenchant)
# Called with storage equinox:write {type, level, m}
#   m = 1 add enchant, m = 0 remove enchant
# ============================================================

# --- Clear and read the held item into storage ---
data remove storage equinox:item stack
execute store result storage equinox:item slot run data get entity @s SelectedItemSlot
function equinox:cmd/read_held with storage equinox:item

# Empty hand / non-item slot
execute unless data storage equinox:item stack run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Hold Equinox Horse Armor in your main hand.","color":"red"}]

# Not an Equinox armor item
execute unless data storage equinox:item stack.components."minecraft:custom_data".equinox_armor run return run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Hold Equinox Horse Armor in your main hand.","color":"red"}]

# --- Write the level for this enchant type (macro dispatch) ---
$function equinox:cmd/enchant/set_$(type) with storage equinox:write

# --- Write the modified stack back to the main hand ---
# loot replace performs a proper inventory sync (unlike raw /data edits)
loot replace entity @s weapon.mainhand 1 from storage equinox:item stack

# --- Feedback (m=1 added, m=0 removed) ---
execute store result score #mode equinox run data get storage equinox:write m
execute if score #mode equinox matches 1 run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"","color":"green"},{"nbt":"type","storage":"equinox:write","color":"aqua"},{"text":" ","color":"aqua"},{"score":{"name":"#lvl","objective":"equinox"},"color":"aqua"},{"text":" added!","color":"green"}]
execute if score #mode equinox matches 0 run tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"nbt":"type","storage":"equinox:write","color":"aqua"},{"text":" removed.","color":"green"}]
