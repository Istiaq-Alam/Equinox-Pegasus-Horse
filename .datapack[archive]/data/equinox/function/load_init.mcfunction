# ============================================================
# EQUINOX - FIRST LOAD INITIALISATION
# Creates scoreboards, storage defaults and constants.
# ============================================================

scoreboard objectives add equinox dummy

# --- Timing constants ---
scoreboard players set #tick equinox 0
scoreboard players set #bt equinox 0
scoreboard players set #ten equinox 10
scoreboard players set #sixteen equinox 16

# --- Mount registry (player UUID -> home / last known / horse UUID) ---
execute unless data storage equinox:mounts mounts run data modify storage equinox:mounts mounts set value []

# --- Recovery state ---
data modify storage equinox:state recovering set value 0b

tellraw @a [{"text":"⚡ ","color":"gold"},{"text":"Equinox ","color":"gold","bold":true},{"text":"v2.0 ","color":"dark_purple"},{"text":"- ","color":"dark_gray"},{"text":"Legendary Mount System enabled!","color":"gray"}]
tellraw @a [{"text":"⚡ ","color":"gold"},{"text":"Equinox ","color":"gold","bold":true},{"text":"- ","color":"dark_gray"},{"text":"Real Mount Chunk Recovery enabled!","color":"gray"}]
tellraw @a [{"text":"⚡ ","color":"gold"},{"text":"Equinox ","color":"gold","bold":true},{"text":"- ","color":"dark_gray"},{"text":"Pegasus Flight System enabled!","color":"gray"}]
