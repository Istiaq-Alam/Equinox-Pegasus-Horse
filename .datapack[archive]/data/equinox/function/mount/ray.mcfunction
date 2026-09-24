# ============================================================
# EQUINOX - MOUNT RAYCAST (recursive)
# Steps 0.25 blocks along the view ray (28 steps ~ 7 blocks),
# tags the first horse it reaches as equinox_target.
# ============================================================

scoreboard players add #ray equinox 1

# Hit a horse?
execute if entity @e[type=horse,distance=..1.0,limit=1,sort=nearest] run tag @e[type=horse,distance=..1.0,limit=1,sort=nearest] add equinox_target

# Continue the ray while nothing has been hit
execute unless entity @e[tag=equinox_target] if score #ray equinox matches ..28 positioned ^ ^ ^0.25 run function equinox:mount/ray
