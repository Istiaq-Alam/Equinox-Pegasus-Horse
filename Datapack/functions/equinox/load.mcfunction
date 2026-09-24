# Equinox - world/datapack load entry point
# Runs when the datapack is loaded into a world.
# Calls the per-scope init so variables are ready before tick() runs.

# Run BEFORE world tick
time set day 0
function equinox:variables/init
function equinox:tick
