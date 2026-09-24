# Equinox Datapack - root variable storage
# This file seeds the datapack variables on first load.
# Variables that hold mount + flight state.

# --- Mount registration ---
# Was a mount bound to this player?
define equinox.mount.registered bool false
# Owner UUID (string) of the bound mount
define equinox.mount.owner string ""
# Horse UUID (string) of the bound mount
define equinox.mount.horse string ""
# Permanent bind (home) location
define equinox.mount.home.x double 0.0
define equinox.mount.home.y double 0.0
define equinox.mount.home.z double 0.0
define equinox.mount.home.yaw double 0.0
define equinox.mount.home.pitch double 0.0
# Last known location of the real horse
define equinox.mount.last.x double 0.0
define equinox.mount.last.y double 0.0
define equinox.mount.last.z double 0.0
define equinox.mount.last.yaw double 0.0
define equinox.mount.last.pitch double 0.0

# --- Flight state ---
# Is the current mount actively flying?
define equinox.flight.active bool false
# Original Y of the mount on takeoff (used to rebuild vertical speed on landing)
define equinox.flight.og-y double 0.0
# Flight clock for the Bifrost pathway
define equinox.flight.ticks long 0

# --- Whistle system ---
define equinox.whistle.cooldown long 0
define equinox.whistle.target.exists bool false
define equinox.whistle.target.x double 0.0
define equinox.whistle.target.y double 0.0
define equinox.whistle.target.z double 0.0
define equinox.whistle.target.yaw double 0.0
define equinox.whistle.target.pitch double 0.0

# --- Encounter / escort state ---
define equinox.escort.active bool false
define equinox.escort.owner string ""
define equinox.escort.horse string ""
define equinox.escort.cooldown long 0
