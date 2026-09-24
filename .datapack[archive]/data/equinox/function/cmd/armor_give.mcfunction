# ============================================================
# EQUINOX - GIVE ARMOR
# Usage:
#   data modify storage equinox:args player set value "Steve"
#   data modify storage equinox:args type set value "golden"
#   function equinox:cmd/armor_give with storage equinox:args
#
# type: leather | iron | golden | diamond | netherite
# ============================================================

# custom_data holds the plugin's PDC flags:
#   equinox_armor = "1"   (identified as Equinox armor)
#   swift / titan-leap / vitality = enchant levels
$give $(player) minecraft:$(type)_horse_armor[custom_data={equinox_armor:"1",swift:0,"titan-leap":0,vitality:0},custom_name='{"text":"✦ Equinox Horse Armor ✦","color":"gold","bold":true,"italic":false}',lore=['{"text":"━━━━━━━━━━━━━━━━━━","color":"dark_gray","italic":false}','{"text":"No Equinox enchantments","color":"gray","italic":false}','{"text":"━━━━━━━━━━━━━━━━━━","color":"dark_gray","italic":false}','{"text":"✦ Legendary Mount Equipment","color":"gold","italic":false}']]

# Feedback
$tellraw $(player) [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"You received Equinox Horse Armor!","color":"gold"}]
tellraw @s [{"text":"⚡ ","color":"gold"},{"text":"Equinox","color":"gold","bold":true},{"text":" » ","color":"dark_gray"},{"text":"Equinox Horse Armor given.","color":"green"}]
