# Reads the held hotbar slot stack into storage equinox:item stack
$data modify storage equinox:item stack set from entity @s Inventory[{Slot:$(slot)b}]
