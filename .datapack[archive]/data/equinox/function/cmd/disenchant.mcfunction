# ============================================================
# EQUINOX - DISENCHANT HELD ARMOR
# Usage:
#   data modify storage equinox:args type set value "swift"
#   function equinox:cmd/disenchant with storage equinox:args
# ============================================================

data modify storage equinox:write type set from storage equinox:args type
data modify storage equinox:write level set value 0
data modify storage equinox:write m set value 0

function equinox:cmd/item_write with storage equinox:write
