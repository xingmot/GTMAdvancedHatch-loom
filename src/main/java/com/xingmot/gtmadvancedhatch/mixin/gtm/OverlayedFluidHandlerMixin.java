package com.xingmot.gtmadvancedhatch.mixin.gtm;

import com.gregtechceu.gtceu.utils.OverlayedFluidHandler;

import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.misc.FluidTransferList;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidStorage;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OverlayedFluidHandler.class)
public class OverlayedFluidHandlerMixin {

    @Redirect(remap = false,
              method = "<init>",
              at = @At(value = "INVOKE", target = "Lcom/lowdragmc/lowdraglib/misc/FluidStorage;setFluid(Lcom/lowdragmc/lowdraglib/side/fluid/FluidStack;)V"))
    public void setFluidMixin(FluidStorage storage, FluidStack fluid, FluidTransferList tank, @Local int i) {
        storage.setValidator(fluidStack -> tank.isFluidValid(i, fluidStack));
        storage.setFluid(fluid);
    }

    @Mixin(targets = "com.gregtechceu.gtceu.utils.OverlayedFluidHandler$OverlayedTank")
    private static class OverlayedTankMixin {

        @Shadow(remap = false)
        @Final
        private IFluidStorage property;

        @Inject(remap = false, method = "tryInsert", at = @At("HEAD"), cancellable = true)
        public void tryInsertMixin(FluidStack fluid, long amount, CallbackInfoReturnable<Long> cir) {
            if (!this.property.isFluidValid(fluid)) cir.setReturnValue(0L);
        }
    }
}
