# equinox:commands/armor/give
# Requires: <player: target> <type: string>

# Permission: equinox.admin (op default)
# (Datapacks have no perm system; the server owner grants perms in ops.txt / permissions.)

# Validate armor type
execute if entity @s run data get entity @s Inventory[0] type
# (Type validation is done by the summon dispatcher; here we just route.)

# Build the armor item via loot table
# <player> gets a new Equinox Horse Armor in their inventory
loot add entity $armor_player armor_helmet 1 full loot
  if entity @s run data get entity @s Inventory[0] type
  if entity @s run data get entity @s minecraft:component.equinox_armor run data modify entity $armor_player tag.equinox_armor set value true
  if entity @s run data modify entity $armor_player tag.equinox_armor_type set value "leather"
  if entity @s run data modify entity $armor_player tag.equinox_armor_level set value 0

# Feedback
tellraw $armor_player [{"translate":"equinox.armor.given","with":[{"text":"✦ Equinox Horse Armor ✦"}]}]
tellraw $armor_player [{"translate":"equinox.armor.given.to","with":[{"text":"@s"}]}]
