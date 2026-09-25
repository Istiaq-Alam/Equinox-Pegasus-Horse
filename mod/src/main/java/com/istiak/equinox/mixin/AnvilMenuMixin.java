package com.istiak.equinox.mixin;

import com.istiak.equinox.items.EquinoxAnvilHandler;
import com.istiak.equinox.items.EquinoxItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anvil enchanting for Equinox armor.
 *
 * createResult: when the two input slots hold Equinox armor + an Equinox
 * enchant book (either order), the vanilla result is replaced with the
 * enchanted armor and the XP cost DataSlot is set (vanilla's mayPickup gates
 * on it and onTake deducts the levels).
 *
 * onTake: vanilla consumes one item from each input slot - the book is spent,
 * the plain armor is spent, the player receives the enchanted result. We only
 * add feedback (sound + action bar).
 *
 * All other combinations (renaming, vanilla books, other items) fall through
 * to vanilla logic untouched.
 *
 * NOTE: the input/result containers live on the SUPERCLASS
 * (ItemCombinerMenu.inputSlots/resultSlots). @Shadow only supports members
 * declared on the target class itself, so slots are reached through the
 * public Menu.getSlot(int) API instead - slot layout for the anvil is
 * 0=input, 1=material, 2=result (AnvilMenu.INPUT_SLOT/ADDITIONAL_SLOT/
 * RESULT_SLOT). 'cost' IS declared on AnvilMenu and can be shadowed.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    private ItemStack equinox$slotItem(int index) {
        return ((AnvilMenu) (Object) this).getSlot(index).getItem();
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void equinox$enchantArmor(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;

        ItemStack slot0 = equinox$slotItem(0);
        ItemStack slot1 = equinox$slotItem(1);

        ItemStack armor = null;
        ItemStack book = null;
        if (EquinoxItems.isEquinoxArmorItem(slot0.getItem()) && isBook(slot1)) {
            armor = slot0;
            book = slot1;
        } else if (EquinoxItems.isEquinoxArmorItem(slot1.getItem()) && isBook(slot0)) {
            armor = slot1;
            book = slot0;
        }
        if (armor == null) {
            return; // not our combination - vanilla handles it
        }

        ItemStack result = EquinoxAnvilHandler.buildResult(armor, book);
        int xpCost = EquinoxAnvilHandler.resultCost(armor, book);

        if (result == null || xpCost <= 0) {
            // Our items but invalid operation (e.g. downgrade): no result.
            menu.getSlot(2).set(ItemStack.EMPTY);
            cost.set(0);
            ci.cancel();
            return;
        }

        menu.getSlot(2).set(result);
        cost.set(xpCost);
        ci.cancel();
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void equinox$enchantFeedback(Player player, ItemStack stack, CallbackInfo ci) {
        ItemStack slot0 = equinox$slotItem(0);
        ItemStack slot1 = equinox$slotItem(1);
        if ((isBook(slot0) && EquinoxItems.isEquinoxArmorItem(slot1.getItem()))
                || (isBook(slot1) && EquinoxItems.isEquinoxArmorItem(slot0.getItem()))) {
            EquinoxAnvilHandler.playEnchantSound(player);
        }
    }

    private boolean isBook(ItemStack stack) {
        return !stack.isEmpty() && EquinoxItems.getBookEnchant(stack) != null;
    }
}
