package com.xingmot.gtmadvancedhatch.mixin.gtm;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * mixin: {@link com.gregtechceu.gtceu.utils.OverlayedItemHandler.OverlayedItemHandlerSlot}
 */
@Mixin(targets = "com.gregtechceu.gtceu.utils.OverlayedItemHandler$OverlayedItemHandlerSlot")
public class OverlayedItemSlotHandlerMixin {

    @Shadow(remap = false)
    private int slotLimit;

    @Shadow(remap = false)
    private ItemStack itemStack;

    @Inject(remap = false, method = "<init>(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("RETURN"))
    private void initMixin(ItemStack stackToMirror, int slotLimit, CallbackInfo ci) {
        this.slotLimit = Math.min(slotLimit, itemStack.getMaxStackSize() * (slotLimit / 64));
    }

    @Inject(remap = false, method = "<init>(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
    private void initMixin(ItemStack itemStack, int slotLimit, int count, CallbackInfo ci) {
        this.slotLimit = Math.min(slotLimit, itemStack.getMaxStackSize() * (slotLimit / 64));
    }
}
