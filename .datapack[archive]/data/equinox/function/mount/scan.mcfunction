# ============================================================
# EQUINOX - OWNER MATCH SCANNER
# Called with storage equinox:ctx {i0..i3} as each bound horse.
# Tags matching horses as equinox_hit.
# ============================================================
$execute if data entity @s {Owner:[I;$(i0),$(i1),$(i2),$(i3)]} run tag @s add equinox_hit
