# ============================================================
# EQUINOX - GIVE WHISTLE
# Usage:
#   data modify storage equinox:args player set value "Steve"
#   function equinox:cmd/whistle_give with storage equinox:args
# ============================================================

# custom_data equinox_whistle = "1" is what the
# minecraft:using_item advancement looks for.
$give $(player) minecraft:goat_horn[custom_data={equinox_whistle:"1"},custom_name='{"text":"✦ Equinox Whistle","color":"light_purple","bold":true,"italic":false}',lore=['{"text":"A mystical horn bound to your mount.","color":"gray","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"Right-click to call your","color":"white","italic":false}','{"text":"Equinox Mount.","color":"light_purple","italic":false}','{"text":"","color":"gray","italic":false}','{"text":"✦ Bound to Equinox","color":"dark_purple","italic":false}']]

$tellraw $(player) [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"✦ You received an Equinox Whistle!","color":"light_purple"}]
tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Equinox Whistle given.","color":"green"}]
