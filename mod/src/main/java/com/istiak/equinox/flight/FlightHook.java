package com.istiak.equinox.flight;

import com.istiak.equinox.items.EquinoxItems;
import com.istiak.equinox.mount.MountData;
import com.istiak.equinox.mount.MountSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * Static hook called from the PlayerMixin so the mixin class itself
 * stays free of Equinox logic.
 *
 * Dismount is blocked when:
 *  - the player rides their own registered Equinox mount, AND
 *  - the horse still wears Equinox armor.
 *
 * Not blocking when: not registered, not the owner, or armor was removed
 * mid-flight (FlightManager's tick force-stops that case separately).
 */
public final class FlightHook {

    private FlightHook() {
    }

    public static boolean shouldBlockDismount(ServerPlayer player) {
        if (!(player.getVehicle() instanceof Horse horse)) {
            return false;
        }

        MountSavedData mounts = MountSavedData.get(player.level().getServer());
        MountData data = mounts.get(player.getUUID());
        if (data == null || !mounts.isOwner(player, horse)) {
            return false;
        }

        return EquinoxItems.isEquinoxArmor(horse.getItemBySlot(EquipmentSlot.BODY));
    }
}
