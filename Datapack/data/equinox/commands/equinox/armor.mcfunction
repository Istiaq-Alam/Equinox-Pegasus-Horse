# Equinox armor give
# Usage: /equinox armor give <player> <type>
# type: leather | iron | golden | diamond | netherite

# Argument parsing
execute argument 2 as $armor_player if entity $armor_player run function equinox:commands/armor/give

# Invalid arguments
execute unless data storage equinox:variables/equinox.registered run tellraw @a [{"translate":"equinox.cmd.armor.usage"}]
