# equinox:commands/armor/enchant/apply
# Requires: <type:string> <level:number> (passed from the argument function)

# Route by type
# SWIFT
execute if entity @s run data get entity @s tag.equinox_armor_type run data modify entity @s tag.equinox_armor_type set value "swift" run function equinox:commands/armor/enchant/apply/swift run function equinox:commands/armor/enchant/apply/apply_level
# TITAN_LEAP
execute if entity @s run data get entity @s tag.equinox_armor_type run data modify entity @s tag.equinox_armor_type set value "titan-leap" run function equinox:commands/armor/enchant/apply/titan-leap run function equinox:commands/armor/enchant/apply/apply_level
# VITALITY
execute if entity @s run data get entity @s tag.equinox_armor_type run data modify entity @s tag.equinox_armor_type set value "vitality" run function equinox:commands/armor/enchant/apply/vitality run function equinox:commands/armor/enchant/apply/apply_level
