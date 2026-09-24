# ============================================================
# EQUINOX - TICK
# Runs every tick from #minecraft:tick
# ============================================================

# --- Per-tick cleanup of transient tags ---
tag @e[tag=equinox_caller] remove equinox_caller
tag @e[tag=equinox_hit] remove equinox_hit
tag @e[tag=equinox_target] remove equinox_target
tag @a[tag=equinox_binder] remove equinox_binder
tag @a[tag=equinox_me] remove equinox_me

# --- Global 1-tick clock (#tick cycles 0..9) ---
scoreboard players add #tick equinox 1
execute if score #tick equinox matches 10.. run scoreboard players set #tick equinox 0

# --- 2-tick clock for flight visuals (#bt cycles 0..1) ---
scoreboard players add #bt equinox 1
execute if score #bt equinox matches 2.. run scoreboard players set #bt equinox 0

# --- Whistle cooldown countdown (750ms = 38 ticks) ---
execute as @a[scores={equinox.cd=1..}] run scoreboard players remove @s equinox.cd 1

# --- Pegasus flight (every tick) ---
function equinox:flight/tick

# --- Whistle chase mounts (only while a mount is running in) ---
execute if entity @e[tag=equinox_chase] run function equinox:whistle/chase_all

# --- Unloaded-chunk mount recovery state machine ---
execute if data storage equinox:state recovering run function equinox:whistle/recover_tick

# --- Enchantment attribute scanner (every 10 ticks) ---
execute if score #tick equinox matches 0 run function equinox:effect/scan
