# equinox:commands/armor/enchant
# Requires: <type:string> <level:number>

# 1. Confirm the held item is Equinox Horse Armor
# 2. Apply the enchantment via loot table / component

# Check held item is equinox armor
execute if entity @s run data get entity @s minecraft:component.equinox_armor run function equinox:commands/armor/enchant/apply

# Held item is not equinox armor
tellraw @a [{"translate":"equinox.cmd.enchant.not_armor"}]
