package com.istiak.equinox.mixin;

import com.istiak.equinox.flight.FlightHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Port of FlightListener.onToggleSneak's dismount handling.
 *
 * Vanilla dismounts the player when they press SHIFT while riding. Since 1.21.2
 * the dismount decision is made SERVER-SIDE in Player.wantsToStopRiding(), so a
 * server-side event (like Bukkit's PlayerToggleSneakEvent) never sees it first.
 *
 * This mixin cancels the vanilla dismount while the player rides their own
 * flying Equinox mount - exactly what Bukkit's event.setCancelled(true) did.
 * SHIFT then stays a free button for EquinoxMod's takeoff/land toggle.
 *
 * Note: wantsToStopRiding() is DECLARED on Player (not ServerPlayer), so the
 * mixin must target Player. Casting to ServerPlayer is always safe on the
 * logical server side because the reference is only ever a ServerPlayer there.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void equinox$keepRidingEquinoxMount(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && FlightHook.shouldBlockDismount(player)) {
            cir.setReturnValue(false);
        }
    }
}
