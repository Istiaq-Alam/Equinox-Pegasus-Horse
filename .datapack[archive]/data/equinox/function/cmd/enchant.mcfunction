# ============================================================
# EQUINOX - ENCHANT HELD ARMOR
# Usage:
#   data modify storage equinox:args type set value "swift"
#   data modify storage equinox:args level set value 3
#   function equinox:cmd/enchant with storage equinox:args
#
# type: swift (max 10) | titan-leap (max 5) | vitality (max 5)
# ============================================================

# --- Read and clamp the requested level ---
execute store result score #lvl equinox run data get storage equinox:args level
execute unless score #lvl equinox matches 1.. run scoreboard players set #lvl equinox 1

# Per-type max level (mirrors config.yml max-level values)
$function equinox:cmd/enchant/max_$(type)
execute if score #lvl equinox > #max equinox run scoreboard players operation #lvl equinox = #max equinox

# --- Build the write context ---
execute store result storage equinox:write level run scoreboard players get #lvl equinox
data modify storage equinox:write type set from storage equinox:args type
data modify storage equinox:write m set value 1

# --- Apply to held item ---
function equinox:cmd/item_write with storage equinox:write
