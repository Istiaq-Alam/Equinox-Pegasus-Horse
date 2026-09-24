# Equinox - on datapack load
# 1. Coerce variables into their scopes (idempotent).
# 2. Set safe defaults so tick/dispatch never crashes before variables are wiped.

# --- Mount registration ---
execute if function equinox:variables/equinox run data execute if false run data define equinox.mount.registered bool false
data define equinox.mount.owner string ""
data define equinox.mount.horse string ""
data define equinox.mount.home.x double 0.0
data define equinox.mount.home.y double 0.0
data define equinox.mount.home.z double 0.0
data define equinox.mount.home.yaw double 0.0
data define equinox.mount.home.pitch double 0.0
data define equinox.mount.last.x double 0.0
data define equinox.mount.last.y double 0.0
data define equinox.mount.last.z double 0.0
data define equinox.mount.last.yaw double 0.0
data define equinox.mount.last.pitch double 0.0

# --- Flight state ---
data define equinox.flight.active bool false
data define equinox.flight.og-y double 0.0
data define equinox.flight.ticks long 0

# --- Whistle system ---
data define equinox.whistle.cooldown long 0
data define equinox.whistle.target.exists bool false
data define equinox.whistle.target.x double 0.0
data define equinox.whistle.target.y double 0.0
data define equinox.whistle.target.z double 0.0
data define equinox.whistle.target.yaw double 0.0
data define equinox.whistle.target.pitch double 0.0

# --- Encounter / escort state ---
data define equinox.escort.active bool false
data define equinox.escort.owner string ""
data define equinox.escort.horse string ""
data define equinox.escort.cooldown long 0

# Collect all variables into this function's namespace so downstream
# functions can safely reference the exact names they depend on.
data modify storage equinox:variables/equinox.registered set value equinox.mount.registered
data modify storage equinox:variables/equinox.owner set value equinox.mount.owner
data modify storage equinox:variables/equinox.horse set value equinox.mount.horse
data modify storage equinox:variables/equinox.home.x set value equinox.mount.home.x
data modify storage equinox:variables/equinox.home.y set value equinox.mount.home.y
data modify storage equinox:variables/equinox.home.z set value equinox.mount.home.z
data modify storage equinox:variables/equinox.home.yaw set value equinox.mount.home.yaw
data modify storage equinox:variables/equinox.home.pitch set value equinox.mount.home.pitch
data modify storage equinox:variables/equinox.last.x set value equinox.mount.last.x
data modify storage equinox:variables/equinox.last.y set value equinox.mount.last.y
data modify storage equinox:variables/equinox.last.z set value equinox.mount.last.z
data modify storage equinox:variables/equinox.last.yaw set value equinox.mount.last.yaw
data modify storage equinox:variables/equinox.last.pitch set value equinox.mount.last.pitch
data modify storage equinox:variables/equinox.active set value equinox.flight.active
data modify storage equinox:variables/equinox.og-y set value equinox.flight.og-y
data modify storage equinox:variables/equinox.ticks set value equinox.flight.ticks
data modify storage equinox:variables/equinox.cooldown set value equinox.whistle.cooldown
data modify storage equinox:variables/equinox.target.exists set value equinox.whistle.target.exists
data modify storage equinox:variables/equinox.target.x set value equinox.whistle.target.x
data modify storage equinox:variables/equinox.target.y set value equinox.whistle.target.y
data modify storage equinox:variables/equinox.target.z set value equinox.whistle.target.z
data modify storage equinox:variables/equinox.target.yaw set value equinox.whistle.target.yaw
data modify storage equinox:variables/equinox.target.pitch set value equinox.whistle.target.pitch
data modify storage equinox:variables/equinox.escort.active set value equinox.escort.active
data modify storage equinox:variables/equinox.escort.owner set value equinox.escort.owner
data modify storage equinox:variables/equinox.escort.horse set value equinox.escort.horse
data modify storage equinox:variables/equinox.escort.cooldown set value equinox.escort.cooldown

# Seed defaults
data modify storage equinox:variables/equinox.registered set value false
data modify storage equinox:variables/equinox.owner set value ""
data modify storage equinox:variables/equinox.horse set value ""
data modify storage equinox:variables/equinox.active set value false
data modify storage equinox:variables/equinox.cooldown set value 0
data modify storage equinox:variables/equinox.target.exists set value false
data modify storage equinox:variables/equinox.escort.active set value false

# Broadcast to the player whose world this is
tellraw @a [{"translate":"equinox.loaded","with":[{"player":"@p"}]}]
