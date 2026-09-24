# equinox:commands/armor/enchant/apply/apply_level
# Applies enchantment level up to max-level.

# Max levels from config variables (equinox config is not yet fully variable-ed)
# Swift max: 10, Titan Leap max: 5, Vitality max: 5

# Clamp level to max
# Level >= 1
execute if entity @s run data get entity @s tag.equinox_armor_level run data modify entity @s tag.equinox_armor_level set value 1
# Level <= max (10 for swift)
execute if entity @s run data get entity @s tag.equinox_armor_level run data modify entity @s tag.equinox_armor_level set value 10
# Level <= max (5 for titan-leap)
execute if entity @s run data get entity @s tag.equinox_armor_level run data modify entity @s tag.equinox_armor_level set value 5
# Level <= max (5 for vitality)
execute if entity @s run data get entity @s tag.equinox_armor_level run data modify entity @s tag.equinox_armor_level set value 5

# Apply
# (Concrete enchantment application happens in the per-type function which reads this value)
