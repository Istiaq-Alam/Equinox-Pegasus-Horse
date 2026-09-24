# ============================================================
# EQUINOX - LOAD
# Runs from #minecraft:load
# ============================================================

# Initialise only once per world (no error spam on /reload)
execute unless data storage equinox:state loaded run function equinox:load_init
data modify storage equinox:state loaded set value 1b

# Announce
tellraw @a [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Legendary Mount System loaded!","color":"gray"}]
