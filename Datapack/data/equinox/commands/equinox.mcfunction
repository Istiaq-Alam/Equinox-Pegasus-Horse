# Equinox - root command
# Usage: /equinox <help|armor|enchant|disenchant|mount|whistle>

# Normalize first argument (case-insensitive)
execute as @a at @s run execute if entity @s run function equinox:commands/equinox

# Fallback: show help
execute unless data storage equinox:variables/equinox.registered run tellraw @a [{"translate":"equinox.usage"}]
