# equinox:commands/armor/enchant/apply/swift
# Grants Swift enchantment to the held Equinox Horse Armor.

# Clear previous swift level
data modify entity @s tag.equinox_armor_type set value "swift"

# Apply speed bonus (10% per level, max 10 levels = +100%)
# Stored on entity for later tick use
execute if entity @s run data modify entity EquippedArmor minecraft:component.equinox_armor_speed set value 1.0
execute if entity @s run data get entity @s tag.equinox_armor_level run data modify entity EquippedArmor minecraft:component.equinox_armor_speed set value 1.0
execute if entity @s run data get entity @s tag.equinox_armor_level run math multiply entity EquippedArmor minecraft:component.equinox_armor_speed 0.10
execute if entity @s run data get entity @s tag.equinox_armor_level run math add entity EquippedArmor minecraft:component.equinox_armor_speed 1.0
execute if entity @s run data modify entity EquippedArmor minecraft:component.equinox_armor_speed set value entity EquippedArmor minecraft:component.equinox_armor_speed

# Feedback
tellraw @a [{"translate":"equinox.enchant.added","with":[{"text":"⚡ Swift"}]}]
