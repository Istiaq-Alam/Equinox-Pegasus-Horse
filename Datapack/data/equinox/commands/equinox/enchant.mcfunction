# Equinox enchant
# Usage: /equinox enchant <type> <level>
# type: swift | titan-leap | vitality
# level: 1..max (configurables per type)

# Argument parsing
execute argument 2 as $enchant_type if entity $enchant_type run function equinox:commands/armor/enchant
execute unless data storage equinox:variables/equinox.registered run tellraw @a [{"translate":"equinox.cmd.enchant.usage"}]
